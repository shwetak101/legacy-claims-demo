package com.infy.claims.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Customer lifestyle attributes used by PremiumCalculator.
 * Originally lived only in the CUSTOMER_LIFESTYLE table and was read
 * exclusively by SP_CALCULATE_PREMIUM; the Java service never surfaced it.
 */
@Entity
@Table(name = "CUSTOMER_LIFESTYLE")
public class Lifestyle {

    @Id
    private String customerId;
    private Boolean tobaccoUser;
    private Double bmi;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Boolean getTobaccoUser() { return tobaccoUser; }
    public void setTobaccoUser(Boolean tobaccoUser) { this.tobaccoUser = tobaccoUser; }
    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }
}
