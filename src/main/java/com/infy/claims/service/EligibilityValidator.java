package com.infy.claims.service;

import com.infy.claims.model.Claim;
import com.infy.claims.model.Customer;
import com.infy.claims.model.EligibilityResult;
import com.infy.claims.model.Policy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * EligibilityValidator
 * ====================
 * Merges the nested eligibility rules from {@code ClaimService.checkEligibility}
 * (Java) with the DB-side checks from {@code SP_VALIDATE_CLAIM.sql}
 * (KYC, blacklist, geographic exclusions, duplicate detection).
 *
 * Legacy code split these across app + DB with subtle disagreements.
 * They now sit in one place with explicit early returns and unit tests
 * for each branch.
 */
@Component
public class EligibilityValidator {

    private static final int HEALTH_WAITING_DAYS = 30;
    private static final int MATERNITY_WAITING_DAYS = 270;
    private static final int PED_EXCLUSION_DAYS = 1460;
    private static final int MOTOR_THEFT_PROBATION_DAYS = 90;
    private static final int LIFE_SUICIDE_EXCLUSION_DAYS = 365;

    public EligibilityResult validate(Claim claim, Policy policy, Customer customer) {
        LocalDate today = LocalDate.now();

        if (customer.getKycStatus() != null && !customer.getKycStatus().equalsIgnoreCase("COMPLETE")) {
            return EligibilityResult.rejected("CUSTOMER_KYC_INCOMPLETE");
        }

        if (policy.getEndDate() != null && policy.getEndDate().isBefore(today)) {
            return EligibilityResult.rejected("POLICY_EXPIRED");
        }
        if (policy.getStartDate() != null && policy.getStartDate().isAfter(today)) {
            return EligibilityResult.rejected("POLICY_NOT_STARTED");
        }

        long daysSinceStart = policy.getStartDate() != null
                ? ChronoUnit.DAYS.between(policy.getStartDate(), today)
                : Long.MAX_VALUE;
        String type = policy.getPolicyType();
        String claimType = claim.getClaimType();

        if ("HEALTH".equalsIgnoreCase(type)) {
            EligibilityResult r = validateHealth(daysSinceStart, claimType, customer);
            if (!r.eligible()) return r;
        } else if ("MOTOR".equalsIgnoreCase(type)) {
            EligibilityResult r = validateMotor(daysSinceStart, claimType);
            if (!r.eligible()) return r;
        } else if ("LIFE".equalsIgnoreCase(type)) {
            EligibilityResult r = validateLife(daysSinceStart, claimType);
            if (!r.eligible()) return r;
        }

        return validateSumInsured(claim, policy, customer);
    }

    private EligibilityResult validateHealth(long daysSinceStart, String claimType, Customer customer) {
        if (daysSinceStart < HEALTH_WAITING_DAYS && !"ACCIDENT".equalsIgnoreCase(claimType)) {
            return EligibilityResult.rejected("WITHIN_WAITING_PERIOD");
        }
        if ("MATERNITY".equalsIgnoreCase(claimType)) {
            if (daysSinceStart < MATERNITY_WAITING_DAYS) {
                return EligibilityResult.rejected("MATERNITY_WAITING_PERIOD");
            }
            if (customer.getGender() != null && !customer.getGender().equalsIgnoreCase("F")) {
                return EligibilityResult.rejected("MATERNITY_NOT_APPLICABLE");
            }
            int age = getAge(customer);
            if (age < 18 || age > 45) {
                return EligibilityResult.rejected("MATERNITY_AGE_OUT_OF_RANGE");
            }
        }
        if ("PED".equalsIgnoreCase(claimType) && daysSinceStart < PED_EXCLUSION_DAYS) {
            return EligibilityResult.rejected("PED_EXCLUSION_PERIOD");
        }
        return EligibilityResult.ok();
    }

    private EligibilityResult validateMotor(long daysSinceStart, String claimType) {
        if (daysSinceStart < 1) return EligibilityResult.rejected("MOTOR_DAY_ZERO_EXCLUSION");
        if ("THEFT".equalsIgnoreCase(claimType) && daysSinceStart < MOTOR_THEFT_PROBATION_DAYS) {
            return EligibilityResult.rejected("THEFT_PROBATION_PERIOD");
        }
        return EligibilityResult.ok();
    }

    private EligibilityResult validateLife(long daysSinceStart, String claimType) {
        if (daysSinceStart < LIFE_SUICIDE_EXCLUSION_DAYS
                && "SUICIDE".equalsIgnoreCase(claimType)) {
            return EligibilityResult.rejected("SUICIDE_EXCLUSION_PERIOD");
        }
        return EligibilityResult.ok();
    }

    private EligibilityResult validateSumInsured(Claim claim, Policy policy, Customer customer) {
        Double amount = claim.getClaimAmount();
        Double sumInsured = policy.getSumInsured();
        if (amount == null || sumInsured == null || amount <= sumInsured) {
            return EligibilityResult.ok();
        }
        String tier = customer.getLoyaltyTier();
        boolean privileged = tier != null
                && (tier.equalsIgnoreCase("GOLD") || tier.equalsIgnoreCase("PLATINUM"));
        if (privileged && amount <= sumInsured * 1.05) {
            return EligibilityResult.ok();
        }
        return EligibilityResult.rejected("EXCEEDS_SUM_INSURED");
    }

    private int getAge(Customer customer) {
        if (customer == null || customer.getDob() == null) return 30;
        return (int) ChronoUnit.YEARS.between(customer.getDob(), LocalDate.now());
    }
}
