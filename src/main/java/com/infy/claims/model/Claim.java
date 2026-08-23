package com.infy.claims.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Claim {
    private String id;
    private String customerId;
    private String policyNumber;
    private Double claimAmount;
    private Double approvedAmount;
    private String claimType;
    private String status;
    private String description;
    private LocalDateTime submittedAt;
}
