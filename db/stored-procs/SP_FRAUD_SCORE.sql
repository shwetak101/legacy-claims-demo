-- =====================================================================
-- SP_FRAUD_SCORE
-- ---------------------------------------------------------------------
-- Original fraud scoring proc from 2015. Rakesh moved most of the
-- scoring logic to ClaimService.java in 2018 (CLM-1704) but this
-- proc is still called by the NIGHTLY_FRAUD_SWEEP job for retrospective
-- scoring of already-approved claims.
--
-- Rules here differ slightly from the Java version — nobody's sure
-- which is authoritative when they disagree. Both write to
-- CLAIM_FRAUD_LOG.
-- =====================================================================

CREATE OR REPLACE PROCEDURE SP_FRAUD_SCORE (
    p_claim_id       IN  VARCHAR2,
    p_score          OUT NUMBER,
    p_risk_level     OUT VARCHAR2
)
AS
    v_customer_id     VARCHAR2(50);
    v_claim_amount    NUMBER;
    v_claim_pincode   VARCHAR2(10);
    v_hour            NUMBER;
    v_prior_claims    NUMBER;
    v_amount_ratio    NUMBER;
    v_sum_insured     NUMBER;
BEGIN
    p_score := 0;

    SELECT c.CUSTOMER_ID, c.CLAIM_AMOUNT, c.CLAIM_PINCODE,
           EXTRACT(HOUR FROM c.SUBMITTED_AT), p.SUM_INSURED
      INTO v_customer_id, v_claim_amount, v_claim_pincode, v_hour, v_sum_insured
      FROM CLAIMS c JOIN POLICIES p ON c.POLICY_NUMBER = p.POLICY_NUMBER
     WHERE c.CLAIM_ID = p_claim_id;

    -- High-amount loading
    IF v_claim_amount > 2000000 THEN
        p_score := p_score + 35;
    ELSIF v_claim_amount > 500000 THEN
        p_score := p_score + 18;
    END IF;

    -- Ratio of claim to sum insured
    IF v_sum_insured > 0 THEN
        v_amount_ratio := v_claim_amount / v_sum_insured;
        IF v_amount_ratio > 0.9 THEN
            p_score := p_score + 20;
        ELSIF v_amount_ratio > 0.7 THEN
            p_score := p_score + 10;
        END IF;
    END IF;

    -- Odd hour
    IF v_hour BETWEEN 1 AND 4 THEN
        p_score := p_score + 10;
    END IF;

    -- Multiple prior claims in 6 months
    SELECT COUNT(*) INTO v_prior_claims
      FROM CLAIMS
     WHERE CUSTOMER_ID = v_customer_id
       AND CLAIM_DATE > ADD_MONTHS(SYSDATE, -6)
       AND CLAIM_ID <> p_claim_id;

    IF v_prior_claims > 3 THEN
        p_score := p_score + 25;
    ELSIF v_prior_claims > 1 THEN
        p_score := p_score + 10;
    END IF;

    -- Risk level
    IF p_score >= 60 THEN
        p_risk_level := 'HIGH';
    ELSIF p_score >= 30 THEN
        p_risk_level := 'MEDIUM';
    ELSE
        p_risk_level := 'LOW';
    END IF;

    -- log both scores side by side for reconciliation
    INSERT INTO CLAIM_FRAUD_LOG (CLAIM_ID, DB_SCORE, DB_RISK, LOGGED_AT)
    VALUES (p_claim_id, p_score, p_risk_level, SYSDATE);
    COMMIT;

END SP_FRAUD_SCORE;
/
