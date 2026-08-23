package com.infy.claims.service;

import com.infy.claims.model.Claim;
import com.infy.claims.model.Customer;
import com.infy.claims.model.EligibilityResult;
import com.infy.claims.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityValidatorTest {

    private EligibilityValidator validator;
    private Policy policy;
    private Customer customer;
    private Claim claim;

    @BeforeEach
    void setUp() {
        validator = new EligibilityValidator();

        policy = new Policy();
        policy.setPolicyNumber("P1");
        policy.setPolicyType("HEALTH");
        policy.setSumInsured(500_000.0);
        policy.setStartDate(LocalDate.now().minusYears(2));
        policy.setEndDate(LocalDate.now().plusYears(1));

        customer = new Customer();
        customer.setId("C1");
        customer.setDob(LocalDate.now().minusYears(35));
        customer.setKycStatus("COMPLETE");

        claim = new Claim();
        claim.setId("CL1");
        claim.setCustomerId("C1");
        claim.setPolicyNumber("P1");
        claim.setClaimAmount(100_000.0);
        claim.setClaimType("ILLNESS");
    }

    @Test
    void rejects_incomplete_kyc() {
        customer.setKycStatus("PENDING");
        EligibilityResult r = validator.validate(claim, policy, customer);
        assertFalse(r.eligible());
        assertEquals("CUSTOMER_KYC_INCOMPLETE", r.reason());
    }

    @Test
    void rejects_expired_policy() {
        policy.setEndDate(LocalDate.now().minusDays(1));
        assertFalse(validator.validate(claim, policy, customer).eligible());
    }

    @Test
    void health_within_waiting_period_non_accident_rejected() {
        policy.setStartDate(LocalDate.now().minusDays(10));
        assertFalse(validator.validate(claim, policy, customer).eligible());
    }

    @Test
    void health_within_waiting_period_accident_allowed() {
        policy.setStartDate(LocalDate.now().minusDays(10));
        claim.setClaimType("ACCIDENT");
        assertTrue(validator.validate(claim, policy, customer).eligible());
    }

    @Test
    void maternity_gender_check() {
        claim.setClaimType("MATERNITY");
        customer.setGender("M");
        assertEquals("MATERNITY_NOT_APPLICABLE",
                validator.validate(claim, policy, customer).reason());
    }

    @Test
    void motor_theft_probation() {
        policy.setPolicyType("MOTOR");
        policy.setStartDate(LocalDate.now().minusDays(30));
        claim.setClaimType("THEFT");
        assertEquals("THEFT_PROBATION_PERIOD",
                validator.validate(claim, policy, customer).reason());
    }

    @Test
    void sum_insured_exceeded_no_privilege() {
        claim.setClaimAmount(600_000.0);
        assertEquals("EXCEEDS_SUM_INSURED",
                validator.validate(claim, policy, customer).reason());
    }

    @Test
    void sum_insured_grace_applied_for_platinum() {
        claim.setClaimAmount(510_000.0);
        customer.setLoyaltyTier("PLATINUM");
        assertTrue(validator.validate(claim, policy, customer).eligible());
    }
}
