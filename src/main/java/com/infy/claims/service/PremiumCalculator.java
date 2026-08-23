package com.infy.claims.service;

import com.infy.claims.model.Customer;
import com.infy.claims.model.Lifestyle;
import com.infy.claims.model.Policy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * PremiumCalculator
 * =================
 * Single home for premium calculation rules. In the legacy system these
 * rules lived in TWO places:
 *
 *  - Java: {@code ClaimService.calculatePremium} — age, region, occupation,
 *          loyalty, claims history, gender rules
 *  - PL/SQL: {@code SP_CALCULATE_PREMIUM} — tobacco, BMI, group discount,
 *          war/disturbed-region rules (never surfaced in Java)
 *
 * All are now first-class, testable methods on this class. Each returns a
 * multiplier applied to the base premium (which is 2% of the sum insured).
 *
 * Rule sources are cited on each method with the original file:line ref.
 */
@Component
public class PremiumCalculator {

    // ---- constants pulled out of the legacy code ------------------------

    /** Pincode prefixes historically associated with higher fraud/loss ratios. */
    private static final Set<String> HIGH_RISK_PINCODE_PREFIXES =
            Set.of("110", "400", "560", "600", "700");

    /**
     * State codes (first two digits of pincode) with regulatory risk loading.
     * Legacy PL/SQL read this from {@code RISK_STATES}; hoisted here for the
     * demo. Real deployments should still read from configuration.
     */
    private static final List<String> WAR_REGION_STATE_CODES =
            List.of("18", "19"); // placeholder — align with RISK_STATES

    // ---- rules originally in ClaimService.calculatePremium --------------

    /** Age-band multiplier — {@code ClaimService:189-215}. */
    public double ageMultiplier(int age, String policyType) {
        if (!"HEALTH".equalsIgnoreCase(policyType) && !"LIFE".equalsIgnoreCase(policyType)) {
            return 1.0;
        }
        double m;
        if (age < 25) m = 0.85;
        else if (age < 40) m = 1.0;
        else if (age < 55) m = 1.35;
        else if (age < 65) m = 1.75;
        else if (age < 75) m = 2.4;
        else m = 3.2;

        if ("HEALTH".equalsIgnoreCase(policyType) && age >= 65 && age < 75) m *= 1.15;
        if ("HEALTH".equalsIgnoreCase(policyType) && age >= 75) m *= 1.25;
        return m;
    }

    /** Region multiplier — {@code ClaimService:218-224}. */
    public double regionMultiplier(String pincode) {
        if (pincode == null || pincode.length() < 3) return 1.0;
        return HIGH_RISK_PINCODE_PREFIXES.contains(pincode.substring(0, 3)) ? 1.12 : 1.0;
    }

    /** Occupation loading — {@code ClaimService:227-238}. */
    public double occupationMultiplier(String occupation) {
        if (occupation == null) return 1.0;
        String occ = occupation.toLowerCase();
        if (occ.contains("miner") || occ.contains("pilot") || occ.contains("diver")) return 1.5;
        if (occ.contains("driver") || occ.contains("construction")) return 1.25;
        if (occ.contains("teacher") || occ.contains("clerk") || occ.contains("software")) return 0.95;
        if (occ.contains("defence") || occ.contains("defense") || occ.contains("army")) return 0.85;
        return 1.0;
    }

    /** Loyalty discount — {@code ClaimService:241-249}. */
    public double loyaltyMultiplier(String tier) {
        if (tier == null) return 1.0;
        return switch (tier.toUpperCase()) {
            case "SILVER" -> 0.98;
            case "GOLD" -> 0.95;
            case "PLATINUM" -> 0.90;
            default -> 1.0;
        };
    }

    /** Claims-history multiplier — {@code ClaimService:252-263}. */
    public double claimsHistoryMultiplier(int previousClaims) {
        if (previousClaims == 0) return 0.90;
        if (previousClaims == 1) return 1.0;
        if (previousClaims == 2) return 1.15;
        if (previousClaims <= 5) return 1.35;
        return 1.6;
    }

    /** Gender-specific HEALTH discount — {@code ClaimService:266-270}. */
    public double genderMultiplier(String gender, String policyType) {
        if ("HEALTH".equalsIgnoreCase(policyType)
                && gender != null && gender.equalsIgnoreCase("F")) {
            return 0.97;
        }
        return 1.0;
    }

    // ---- rules recovered from SP_CALCULATE_PREMIUM (PL/SQL) -------------

    /** Tobacco loading — {@code SP_CALCULATE_PREMIUM.sql:52-63}. */
    public double tobaccoMultiplier(boolean isTobaccoUser, String policyType) {
        if (!isTobaccoUser) return 1.0;
        if ("HEALTH".equalsIgnoreCase(policyType)) return 1.40;
        if ("LIFE".equalsIgnoreCase(policyType)) return 1.55;
        return 1.0;
    }

    /** BMI-based loading for HEALTH policies — {@code SP_CALCULATE_PREMIUM.sql:67-84}. */
    public double bmiMultiplier(Double bmi, String policyType) {
        if (bmi == null || !"HEALTH".equalsIgnoreCase(policyType)) return 1.0;
        if (bmi > 35) return 1.30;
        if (bmi > 30) return 1.15;
        if (bmi < 18) return 1.10;
        return 1.0;
    }

    /** Group-scheme discount — {@code SP_CALCULATE_PREMIUM.sql:87-107}. */
    public double groupDiscountMultiplier(boolean isGroup, int groupSize) {
        if (!isGroup) return 1.0;
        if (groupSize > 500) return 0.70;
        if (groupSize > 100) return 0.80;
        if (groupSize > 25)  return 0.90;
        return 1.0;
    }

    /**
     * War / disturbed-region loading — {@code SP_CALCULATE_PREMIUM.sql:111-124}.
     * Legacy PL/SQL cursored over RISK_STATES; simplified here to a static list.
     */
    public double warRegionMultiplier(String pincode) {
        if (pincode == null || pincode.length() < 2) return 1.0;
        String stateCode = pincode.substring(0, 2);
        return WAR_REGION_STATE_CODES.contains(stateCode) ? 1.25 : 1.0;
    }

    // ---- composition ---------------------------------------------------

    /**
     * Composes every rule above and returns the final annual premium.
     * Called by {@code ClaimService.recalculatePremium}.
     */
    public double calculate(Policy policy, Customer customer, Lifestyle lifestyle,
                            int previousClaims) {
        double base = policy.getSumInsured() != null ? policy.getSumInsured() * 0.02 : 5000.0;

        int age = getAge(customer);
        boolean tobacco = lifestyle != null && Boolean.TRUE.equals(lifestyle.getTobaccoUser());
        Double bmi = lifestyle != null ? lifestyle.getBmi() : null;
        boolean group = Boolean.TRUE.equals(policy.getIsGroupPolicy());
        int groupSize = policy.getGroupSize() != null ? policy.getGroupSize() : 0;

        double multiplier = ageMultiplier(age, policy.getPolicyType())
                * regionMultiplier(customer.getPincode())
                * occupationMultiplier(customer.getOccupation())
                * loyaltyMultiplier(customer.getLoyaltyTier())
                * claimsHistoryMultiplier(previousClaims)
                * genderMultiplier(customer.getGender(), policy.getPolicyType())
                * tobaccoMultiplier(tobacco, policy.getPolicyType())
                * bmiMultiplier(bmi, policy.getPolicyType())
                * groupDiscountMultiplier(group, groupSize)
                * warRegionMultiplier(customer.getPincode());

        return Math.round(base * multiplier * 100.0) / 100.0;
    }

    private int getAge(Customer customer) {
        if (customer == null || customer.getDob() == null) return 30;
        return (int) ChronoUnit.YEARS.between(customer.getDob(), LocalDate.now());
    }
}
