-- =====================================================================
-- SP_CALCULATE_PREMIUM
-- ---------------------------------------------------------------------
-- Calculates the annual premium for a policy.
-- Original author: Anitha M., 2014
-- Modified by: Rakesh S. (2016 — added tobacco loading)
--              Anitha M. (2017 — added group discount)
--              Rakesh S. (2018 — MOVED age-based multipliers to Java,
--                                 see ClaimService.java. This proc still
--                                 owns the tobacco / BMI / group rules.)
--              Ops team  (2019 — added the war/terror exclusion loading)
--
-- WARNING: parts of the premium logic are ALSO in ClaimService.java.
--          The Java layer normally overrides what this proc returns for
--          HEALTH/LIFE. Don't change one without checking the other.
--
-- Called from: ClaimService.calculatePremium (via ClaimDAO — currently
--              disabled in the Java path since 2018 refactor, but this
--              proc is still called by the nightly batch job
--              CLAIMS_NIGHTLY_RECALC.SQL — do not drop.)
-- =====================================================================

CREATE OR REPLACE PROCEDURE SP_CALCULATE_PREMIUM (
    p_policy_number  IN  VARCHAR2,
    p_customer_id    IN  VARCHAR2,
    p_premium        OUT NUMBER
)
AS
    v_policy_type       VARCHAR2(20);
    v_sum_insured       NUMBER;
    v_base              NUMBER;
    v_multiplier        NUMBER := 1.0;
    v_tobacco_flag      CHAR(1);
    v_bmi               NUMBER;
    v_is_group          CHAR(1);
    v_group_size        NUMBER;
    v_state_code        VARCHAR2(2);
BEGIN

    SELECT POLICY_TYPE, SUM_INSURED
      INTO v_policy_type, v_sum_insured
      FROM POLICIES
     WHERE POLICY_NUMBER = p_policy_number;

    v_base := v_sum_insured * 0.02;

    -- ---------------------------------------------------------------
    -- Tobacco loading (added by Rakesh, 2016)
    -- Data comes from the CUSTOMER_LIFESTYLE flag table.
    -- If we have no lifestyle record, assume non-smoker.
    -- ---------------------------------------------------------------
    BEGIN
        SELECT TOBACCO_FLAG INTO v_tobacco_flag
          FROM CUSTOMER_LIFESTYLE
         WHERE CUSTOMER_ID = p_customer_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN v_tobacco_flag := 'N';
    END;

    IF v_tobacco_flag = 'Y' THEN
        IF v_policy_type = 'HEALTH' THEN
            v_multiplier := v_multiplier * 1.40;   -- heavy loading
        ELSIF v_policy_type = 'LIFE' THEN
            v_multiplier := v_multiplier * 1.55;
        END IF;
    END IF;

    -- ---------------------------------------------------------------
    -- BMI loading — only for HEALTH policies
    -- Rakesh 2016. Data source: CUSTOMER_LIFESTYLE.BMI
    -- ---------------------------------------------------------------
    IF v_policy_type = 'HEALTH' THEN
        BEGIN
            SELECT BMI INTO v_bmi
              FROM CUSTOMER_LIFESTYLE
             WHERE CUSTOMER_ID = p_customer_id;

            IF v_bmi > 35 THEN
                v_multiplier := v_multiplier * 1.30;
            ELSIF v_bmi > 30 THEN
                v_multiplier := v_multiplier * 1.15;
            ELSIF v_bmi < 18 THEN
                v_multiplier := v_multiplier * 1.10;   -- underweight loading
            END IF;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN NULL;
        END;
    END IF;

    -- ---------------------------------------------------------------
    -- Group discount (Anitha, 2017)
    -- Applies when the policy is part of a corporate group scheme.
    -- Bigger groups get bigger discounts. Capped at 30%.
    -- ---------------------------------------------------------------
    BEGIN
        SELECT IS_GROUP_POLICY, GROUP_SIZE
          INTO v_is_group, v_group_size
          FROM POLICY_GROUP_MAP
         WHERE POLICY_NUMBER = p_policy_number;

        IF v_is_group = 'Y' THEN
            IF v_group_size > 500 THEN
                v_multiplier := v_multiplier * 0.70;
            ELSIF v_group_size > 100 THEN
                v_multiplier := v_multiplier * 0.80;
            ELSIF v_group_size > 25 THEN
                v_multiplier := v_multiplier * 0.90;
            END IF;
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN NULL;
    END;

    -- ---------------------------------------------------------------
    -- War / terror / disturbed-region loading (Ops, 2019)
    -- List of state codes maintained in RISK_STATES table.
    -- ---------------------------------------------------------------
    BEGIN
        SELECT SUBSTR(PINCODE,1,2) INTO v_state_code
          FROM CUSTOMERS
         WHERE CUSTOMER_ID = p_customer_id;

        FOR r IN (SELECT STATE_CODE, LOADING_FACTOR
                    FROM RISK_STATES
                   WHERE STATE_CODE = v_state_code)
        LOOP
            v_multiplier := v_multiplier * r.LOADING_FACTOR;
        END LOOP;
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;

    p_premium := ROUND(v_base * v_multiplier, 2);

    -- log to audit table (fire-and-forget)
    INSERT INTO PREMIUM_AUDIT (POLICY_NUMBER, PREMIUM, CALC_TIME)
    VALUES (p_policy_number, p_premium, SYSDATE);
    COMMIT;

END SP_CALCULATE_PREMIUM;
/
