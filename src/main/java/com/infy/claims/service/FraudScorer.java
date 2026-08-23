package com.infy.claims.service;

import com.infy.claims.model.Claim;
import com.infy.claims.model.Customer;
import com.infy.claims.model.Policy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FraudScorer
 * ===========
 * Fraud rules extracted from {@code ClaimService.computeFraudScore} and
 * {@code SP_FRAUD_SCORE.sql}. Where the two disagreed, the modern rules
 * follow the Java version and add the amount-to-sum-insured ratio check
 * that only the PL/SQL version had.
 *
 * The offensive "watch-list surnames" rule from the legacy Java was
 * removed — see AI review comment on the migration PR.
 */
@Component
public class FraudScorer {

    private static final double HIGH_VALUE_THRESHOLD = 500_000.0;
    private static final double VERY_HIGH_VALUE_THRESHOLD = 2_000_000.0;
    private static final int SUSPICIOUS_HOUR_START = 1;
    private static final int SUSPICIOUS_HOUR_END = 4;
    private static final Set<String> HIGH_RISK_PINCODE_PREFIXES =
            Set.of("110", "400", "560", "600", "700");

    public com.infy.claims.model.FraudScore score(Claim claim, Policy policy,
                                                  Customer customer, long previousClaims) {
        int score = 0;
        List<String> flags = new ArrayList<>();

        Double amount = claim.getClaimAmount();
        if (amount != null) {
            if (amount > VERY_HIGH_VALUE_THRESHOLD) {
                score += 30; flags.add("VERY_HIGH_VALUE");
            } else if (amount > HIGH_VALUE_THRESHOLD) {
                score += 15; flags.add("HIGH_VALUE");
            }
        }

        // ratio to sum insured — from SP_FRAUD_SCORE
        if (amount != null && policy.getSumInsured() != null && policy.getSumInsured() > 0) {
            double ratio = amount / policy.getSumInsured();
            if (ratio > 0.9) { score += 20; flags.add("HIGH_AMOUNT_RATIO"); }
            else if (ratio > 0.7) { score += 10; flags.add("ELEVATED_AMOUNT_RATIO"); }
        }

        LocalDateTime submittedAt = claim.getSubmittedAt() != null
                ? claim.getSubmittedAt() : LocalDateTime.now();
        int hour = submittedAt.getHour();
        if (hour >= SUSPICIOUS_HOUR_START && hour <= SUSPICIOUS_HOUR_END) {
            score += 10; flags.add("ODD_HOUR_SUBMISSION");
        }

        String pin = customer.getPincode();
        if (pin != null && pin.length() >= 3
                && HIGH_RISK_PINCODE_PREFIXES.contains(pin.substring(0, 3))) {
            score += 15; flags.add("HIGH_RISK_REGION");
        }

        if (previousClaims > 3) {
            score += 20; flags.add("MULTIPLE_RECENT_CLAIMS");
        }

        if (policy.getStartDate() != null) {
            long days = ChronoUnit.DAYS.between(policy.getStartDate(), LocalDate.now());
            if (days < 30) {
                score += 15; flags.add("EARLY_POLICY_CLAIM");
            }
        }

        String risk = score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        return new com.infy.claims.model.FraudScore(score, risk, flags);
    }
}
