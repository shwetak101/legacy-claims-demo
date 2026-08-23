package com.infy.claims.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Policy {
    private String policyNumber;
    private String customerId;
    private String policyType;   // HEALTH | MOTOR | LIFE
    private Double sumInsured;
    private Double premium;
    private Double deductible;
    private LocalDate startDate;
    private LocalDate endDate;
}
