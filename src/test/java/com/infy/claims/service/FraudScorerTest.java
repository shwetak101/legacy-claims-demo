package com.infy.claims.service;

import com.infy.claims.model.Claim;
import com.infy.claims.model.Customer;
import com.infy.claims.model.FraudScore;
import com.infy.claims.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FraudScorerTest {

    private FraudScorer scorer;
    private Policy policy;
    private Customer customer;
    private Claim claim;

    @BeforeEach
    void setUp() {
        scorer = new FraudScorer();

        policy = new Policy();
        policy.setPolicyNumber("P1");
        policy.setSumInsured(1_000_000.0);
        policy.setStartDate(LocalDate.now().minusYears(1));

        customer = new Customer();
        customer.setId("C1");
        customer.setPincode("560001");

        claim = new Claim();
        claim.setId("CL1");
        claim.setCustomerId("C1");
        claim.setPolicyNumber("P1");
        claim.setClaimAmount(300_000.0);
        claim.setSubmittedAt(LocalDateTime.now().withHour(10));
    }

    @Test
    void low_risk_claim_scores_low() {
        customer.setPincode("400001"); // still high-risk region, but only that flag
        FraudScore s = scorer.score(claim, policy, customer, 0L);
        assertTrue(s.score() < 30);
        assertEquals("LOW", s.riskLevel());
    }

    @Test
    void very_high_value_flag() {
        claim.setClaimAmount(2_500_000.0);
        assertTrue(scorer.score(claim, policy, customer, 0L).flags().contains("VERY_HIGH_VALUE"));
    }

    @Test
    void odd_hour_submission_flag() {
        claim.setSubmittedAt(LocalDateTime.now().withHour(3));
        assertTrue(scorer.score(claim, policy, customer, 0L).flags().contains("ODD_HOUR_SUBMISSION"));
    }

    @Test
    void amount_ratio_flag_from_sp_rules() {
        claim.setClaimAmount(950_000.0);   // 95% of sum insured
        assertTrue(scorer.score(claim, policy, customer, 0L).flags().contains("HIGH_AMOUNT_RATIO"));
    }

    @Test
    void multiple_recent_claims_flag() {
        assertTrue(scorer.score(claim, policy, customer, 5L).flags().contains("MULTIPLE_RECENT_CLAIMS"));
    }

    @Test
    void early_policy_claim_flag() {
        policy.setStartDate(LocalDate.now().minusDays(10));
        assertTrue(scorer.score(claim, policy, customer, 0L).flags().contains("EARLY_POLICY_CLAIM"));
    }
}
