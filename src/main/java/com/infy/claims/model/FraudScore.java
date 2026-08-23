package com.infy.claims.model;

import lombok.Data;

import java.util.List;

@Data
public class FraudScore {
    private int score;
    private String riskLevel;   // LOW | MEDIUM | HIGH
    private List<String> flags;
}
