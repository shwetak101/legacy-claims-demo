package com.infy.claims.model;

import java.util.List;

public record FraudScore(int score, String riskLevel, List<String> flags) {}
