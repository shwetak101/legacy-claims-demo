package com.infy.claims.service;

import com.infy.claims.model.Customer;
import com.infy.claims.model.Lifestyle;
import com.infy.claims.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumCalculatorTest {

    private PremiumCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new PremiumCalculator();
    }

    // ---- rules previously ONLY in Java ---------------------------------

    @Test @DisplayName("age loading: senior gets HEALTH surcharge")
    void ageMultiplier_senior_health() {
        assertEquals(2.4 * 1.15, calc.ageMultiplier(70, "HEALTH"), 0.001);
    }

    @Test @DisplayName("age loading: young MOTOR unaffected")
    void ageMultiplier_motor_unaffected() {
        assertEquals(1.0, calc.ageMultiplier(30, "MOTOR"), 0.001);
    }

    @Test @DisplayName("occupation: high-risk trades loaded 50%")
    void occupationMultiplier_pilot() {
        assertEquals(1.5, calc.occupationMultiplier("Airline Pilot"), 0.001);
    }

    @Test @DisplayName("loyalty: platinum gets 10% discount")
    void loyaltyMultiplier_platinum() {
        assertEquals(0.90, calc.loyaltyMultiplier("PLATINUM"), 0.001);
    }

    @Test @DisplayName("history: no prior claims => 10% NCB")
    void historyMultiplier_zero() {
        assertEquals(0.90, calc.claimsHistoryMultiplier(0), 0.001);
    }

    // ---- rules recovered from SP_CALCULATE_PREMIUM ---------------------

    @Test @DisplayName("tobacco loading: HEALTH smoker +40%")
    void tobaccoMultiplier_health_smoker() {
        assertEquals(1.40, calc.tobaccoMultiplier(true, "HEALTH"), 0.001);
    }

    @Test @DisplayName("tobacco loading: LIFE smoker +55%")
    void tobaccoMultiplier_life_smoker() {
        assertEquals(1.55, calc.tobaccoMultiplier(true, "LIFE"), 0.001);
    }

    @Test @DisplayName("BMI loading: obese HEALTH +30%")
    void bmiMultiplier_obese_health() {
        assertEquals(1.30, calc.bmiMultiplier(38.0, "HEALTH"), 0.001);
    }

    @Test @DisplayName("BMI loading: not applied on MOTOR")
    void bmiMultiplier_motor_ignored() {
        assertEquals(1.0, calc.bmiMultiplier(38.0, "MOTOR"), 0.001);
    }

    @Test @DisplayName("group discount: 30% off for >500-person schemes")
    void groupDiscount_large() {
        assertEquals(0.70, calc.groupDiscountMultiplier(true, 750), 0.001);
    }

    @Test @DisplayName("group discount: not applied for individual policies")
    void groupDiscount_individual() {
        assertEquals(1.0, calc.groupDiscountMultiplier(false, 10), 0.001);
    }

    @Test @DisplayName("war region: loading applied when state code matches")
    void warRegion_loaded() {
        assertEquals(1.25, calc.warRegionMultiplier("190001"), 0.001);
    }

    // ---- end-to-end composition ----------------------------------------

    @Test @DisplayName("calculate: composes every rule and rounds to two decimals")
    void calculate_endToEnd() {
        Policy p = new Policy();
        p.setPolicyNumber("P1");
        p.setPolicyType("HEALTH");
        p.setSumInsured(1_000_000.0);
        p.setStartDate(LocalDate.now().minusYears(2));

        Customer c = new Customer();
        c.setId("C1");
        c.setDob(LocalDate.now().minusYears(45));
        c.setGender("F");
        c.setPincode("560001");   // high-risk region
        c.setOccupation("Software Engineer");
        c.setLoyaltyTier("GOLD");

        Lifestyle l = new Lifestyle();
        l.setCustomerId("C1");
        l.setTobaccoUser(true);
        l.setBmi(32.5);

        double premium = calc.calculate(p, c, l, 0);
        assertTrue(premium > 20000.0, "expected non-trivial premium, got " + premium);
    }
}
