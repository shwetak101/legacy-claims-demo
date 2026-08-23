package com.infy.claims.model;

public record EligibilityResult(boolean eligible, String reason) {
    public static EligibilityResult ok() {
        return new EligibilityResult(true, "ELIGIBLE");
    }
    public static EligibilityResult rejected(String reason) {
        return new EligibilityResult(false, reason);
    }
}
