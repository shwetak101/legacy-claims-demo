package com.infy.claims.service;

import com.infy.claims.model.Claim;
import com.infy.claims.model.Customer;
import com.infy.claims.model.EligibilityResult;
import com.infy.claims.model.FraudScore;
import com.infy.claims.model.Lifestyle;
import com.infy.claims.model.Policy;
import com.infy.claims.repository.ClaimRepository;
import com.infy.claims.repository.CustomerRepository;
import com.infy.claims.repository.LifestyleRepository;
import com.infy.claims.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin orchestrator. Real work is delegated to the three domain services:
 * {@link EligibilityValidator}, {@link PremiumCalculator}, {@link FraudScorer}.
 */
@Service
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);
    private static final int FRAUD_REVIEW_THRESHOLD = 80;

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final LifestyleRepository lifestyleRepository;
    private final EligibilityValidator eligibilityValidator;
    private final PremiumCalculator premiumCalculator;
    private final FraudScorer fraudScorer;

    public ClaimService(ClaimRepository claimRepository,
                        PolicyRepository policyRepository,
                        CustomerRepository customerRepository,
                        LifestyleRepository lifestyleRepository,
                        EligibilityValidator eligibilityValidator,
                        PremiumCalculator premiumCalculator,
                        FraudScorer fraudScorer) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.customerRepository = customerRepository;
        this.lifestyleRepository = lifestyleRepository;
        this.eligibilityValidator = eligibilityValidator;
        this.premiumCalculator = premiumCalculator;
        this.fraudScorer = fraudScorer;
    }

    @Transactional
    public Map<String, Object> submitClaim(Claim claim) {
        log.info("Submitting claim customer={} amount={}", claim.getCustomerId(), claim.getClaimAmount());

        Map<String, Object> response = new HashMap<>();
        Policy policy = policyRepository.findById(claim.getPolicyNumber()).orElse(null);
        Customer customer = customerRepository.findById(claim.getCustomerId()).orElse(null);

        if (policy == null) return reject(response, "policy not found");
        if (customer == null) return reject(response, "customer not found");

        EligibilityResult eligibility = eligibilityValidator.validate(claim, policy, customer);
        if (!eligibility.eligible()) return reject(response, eligibility.reason());

        long previousClaims = claimRepository.countByCustomerIdAndStatus(customer.getId(), "APPROVED");
        FraudScore fraud = fraudScorer.score(claim, policy, customer, previousClaims);

        if (fraud.score() > FRAUD_REVIEW_THRESHOLD) {
            log.warn("High fraud score claim={} score={}", claim.getId(), fraud.score());
            claim.setStatus("PENDING_REVIEW");
            claimRepository.save(claim);
            response.put("status", "PENDING_REVIEW");
            response.put("fraudScore", fraud.score());
            return response;
        }

        Lifestyle lifestyle = lifestyleRepository.findByCustomerId(customer.getId()).orElse(null);
        double newPremium = premiumCalculator.calculate(policy, customer, lifestyle, (int) previousClaims);
        policy.setPremium(newPremium);
        policyRepository.save(policy);

        double payout = computePayout(claim, policy, customer);
        claim.setStatus("APPROVED");
        claim.setApprovedAmount(payout);
        claimRepository.save(claim);

        response.put("status", "APPROVED");
        response.put("payout", payout);
        response.put("fraudScore", fraud.score());
        return response;
    }

    public Claim getClaim(String id) {
        return claimRepository.findById(id).orElse(null);
    }

    public FraudScore scoreFraud(String claimId) {
        Claim c = claimRepository.findById(claimId).orElse(null);
        if (c == null) return null;
        Policy p = policyRepository.findById(c.getPolicyNumber()).orElse(null);
        Customer cust = customerRepository.findById(c.getCustomerId()).orElse(null);
        if (p == null || cust == null) return null;
        long prev = claimRepository.countByCustomerIdAndStatus(cust.getId(), "APPROVED");
        return fraudScorer.score(c, p, cust, prev);
    }

    @Transactional
    public void reprocessClaim(String claimId) {
        Claim c = claimRepository.findById(claimId).orElse(null);
        if (c == null) return;
        c.setStatus("REPROCESSING");
        claimRepository.save(c);
        submitClaim(c);
    }

    private double computePayout(Claim claim, Policy policy, Customer customer) {
        double base = claim.getClaimAmount() != null ? claim.getClaimAmount() : 0.0;

        if ("HEALTH".equalsIgnoreCase(policy.getPolicyType())) {
            int age = customer.getDob() != null
                    ? (int) ChronoUnit.YEARS.between(customer.getDob(), LocalDate.now())
                    : 30;
            if (age > 60) base *= 0.80;
        }

        if (policy.getDeductible() != null) {
            base = Math.max(0.0, base - policy.getDeductible());
        }
        if (policy.getSumInsured() != null && base > policy.getSumInsured()) {
            base = policy.getSumInsured();
        }
        return Math.round(base * 100.0) / 100.0;
    }

    private Map<String, Object> reject(Map<String, Object> response, String reason) {
        response.put("status", "REJECTED");
        response.put("reason", reason);
        return response;
    }
}
