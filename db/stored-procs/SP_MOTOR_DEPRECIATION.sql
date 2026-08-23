-- =====================================================================
-- SP_MOTOR_DEPRECIATION
-- ---------------------------------------------------------------------
-- Returns the depreciation factor (0.0 - 1.0) for a motor policy,
-- based on vehicle age and category. Called by ClaimDAO.fetchMotorDepreciation.
--
-- Depreciation table is the standard IRDAI schedule from 2015.
-- Values have not been reviewed since.
-- =====================================================================

CREATE OR REPLACE PROCEDURE SP_MOTOR_DEPRECIATION (
    p_policy_number  IN  VARCHAR2,
    p_depreciation   OUT NUMBER
)
AS
    v_vehicle_age    NUMBER;
    v_vehicle_type   VARCHAR2(20);
BEGIN

    SELECT ROUND(MONTHS_BETWEEN(SYSDATE, REGISTRATION_DATE) / 12),
           VEHICLE_TYPE
      INTO v_vehicle_age, v_vehicle_type
      FROM MOTOR_POLICY_DETAILS
     WHERE POLICY_NUMBER = p_policy_number;

    -- ---------------------------------------------------------------
    -- Standard IRDAI depreciation schedule (2015)
    -- Same for all vehicle types except commercial (see below)
    -- ---------------------------------------------------------------
    IF v_vehicle_age < 1 THEN
        p_depreciation := 0.05;   -- 5% (first year - not zero, ops policy)
    ELSIF v_vehicle_age < 2 THEN
        p_depreciation := 0.15;
    ELSIF v_vehicle_age < 3 THEN
        p_depreciation := 0.25;
    ELSIF v_vehicle_age < 4 THEN
        p_depreciation := 0.35;
    ELSIF v_vehicle_age < 5 THEN
        p_depreciation := 0.40;
    ELSIF v_vehicle_age < 10 THEN
        p_depreciation := 0.50;
    ELSE
        p_depreciation := 0.60;
    END IF;

    -- Commercial vehicles get an additional 10% flat loading on
    -- depreciation because they're driven harder.
    IF v_vehicle_type = 'COMMERCIAL' THEN
        p_depreciation := LEAST(p_depreciation + 0.10, 0.70);
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        p_depreciation := 0.10;   -- default when we can't find the vehicle
END SP_MOTOR_DEPRECIATION;
/
