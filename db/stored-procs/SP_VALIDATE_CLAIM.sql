-- =====================================================================
-- SP_VALIDATE_CLAIM
-- ---------------------------------------------------------------------
-- Validates a claim against business rules that live in DB tables
-- (blacklists, geographic exclusions, hospital tie-ups).
--
-- Called from: ClaimService.checkEligibility used to call this in 2015-17,
--              Rakesh removed the Java call in 2018 as "too slow" but the
--              proc is still executed by the NIGHTLY_CLAIM_SWEEP job.
--
-- Return codes:
--   'OK'                        - claim passes all DB-side checks
--   'HOSPITAL_BLACKLISTED'      - provider on the blacklist
--   'HOSPITAL_NOT_EMPANELLED'   - provider not in the empanelled network
--   'GEO_EXCLUDED'              - claim origin in an excluded region
--   'CUSTOMER_KYC_INCOMPLETE'   - KYC not up to date
--   'DUPLICATE_CLAIM'           - same amount + provider + week window
-- =====================================================================

CREATE OR REPLACE PROCEDURE SP_VALIDATE_CLAIM (
    p_claim_id       IN  VARCHAR2,
    p_result         OUT VARCHAR2
)
AS
    v_customer_id    VARCHAR2(50);
    v_provider_id    VARCHAR2(50);
    v_claim_amount   NUMBER;
    v_claim_date     DATE;
    v_claim_pincode  VARCHAR2(10);
    v_policy_type    VARCHAR2(20);
    v_kyc_status     VARCHAR2(20);
    v_blacklisted    NUMBER;
    v_empanelled     NUMBER;
    v_excluded       NUMBER;
    v_duplicate_ct   NUMBER;
BEGIN

    SELECT CUSTOMER_ID, PROVIDER_ID, CLAIM_AMOUNT, CLAIM_DATE, CLAIM_PINCODE
      INTO v_customer_id, v_provider_id, v_claim_amount, v_claim_date, v_claim_pincode
      FROM CLAIMS
     WHERE CLAIM_ID = p_claim_id;

    SELECT p.POLICY_TYPE
      INTO v_policy_type
      FROM CLAIMS c JOIN POLICIES p ON c.POLICY_NUMBER = p.POLICY_NUMBER
     WHERE c.CLAIM_ID = p_claim_id;

    -- KYC check
    SELECT KYC_STATUS INTO v_kyc_status
      FROM CUSTOMERS
     WHERE CUSTOMER_ID = v_customer_id;

    IF v_kyc_status <> 'COMPLETE' THEN
        p_result := 'CUSTOMER_KYC_INCOMPLETE';
        RETURN;
    END IF;

    -- ---------------------------------------------------------------
    -- Blacklisted providers (updated monthly by ops via a manual UPDATE
    -- — TODO: build a real admin UI, CLM-1811, open since 2018)
    -- ---------------------------------------------------------------
    SELECT COUNT(*) INTO v_blacklisted
      FROM PROVIDER_BLACKLIST
     WHERE PROVIDER_ID = v_provider_id
       AND (END_DATE IS NULL OR END_DATE > SYSDATE);

    IF v_blacklisted > 0 THEN
        p_result := 'HOSPITAL_BLACKLISTED';
        RETURN;
    END IF;

    -- ---------------------------------------------------------------
    -- Empanelled network check — only for HEALTH policies
    -- ---------------------------------------------------------------
    IF v_policy_type = 'HEALTH' THEN
        SELECT COUNT(*) INTO v_empanelled
          FROM EMPANELLED_HOSPITALS
         WHERE PROVIDER_ID = v_provider_id;

        IF v_empanelled = 0 THEN
            p_result := 'HOSPITAL_NOT_EMPANELLED';
            RETURN;
        END IF;
    END IF;

    -- ---------------------------------------------------------------
    -- Geographic exclusions
    -- Some pincodes are excluded due to natural disaster history
    -- (see the 2018 flood exclusion list) or being outside the
    -- policy's declared coverage zone.
    -- ---------------------------------------------------------------
    SELECT COUNT(*) INTO v_excluded
      FROM GEO_EXCLUSIONS
     WHERE PINCODE = v_claim_pincode
       AND EXCLUSION_TYPE IN ('DISASTER', 'OUT_OF_ZONE', 'REGULATORY');

    IF v_excluded > 0 THEN
        p_result := 'GEO_EXCLUDED';
        RETURN;
    END IF;

    -- ---------------------------------------------------------------
    -- Duplicate detection — same customer + provider + amount within
    -- a 7-day window. Ops added this after the 2017 double-billing case.
    -- ---------------------------------------------------------------
    SELECT COUNT(*) INTO v_duplicate_ct
      FROM CLAIMS
     WHERE CUSTOMER_ID = v_customer_id
       AND PROVIDER_ID = v_provider_id
       AND CLAIM_AMOUNT = v_claim_amount
       AND CLAIM_DATE BETWEEN v_claim_date - 7 AND v_claim_date + 7
       AND CLAIM_ID <> p_claim_id;

    IF v_duplicate_ct > 0 THEN
        p_result := 'DUPLICATE_CLAIM';
        RETURN;
    END IF;

    p_result := 'OK';

END SP_VALIDATE_CLAIM;
/
