package com.rateshield.model;

public record RateLimitPolicy(
        String policyId,
        String policyName,
        AlgorithmType algorithmType,
        int limit,
        int windowSizeInSeconds,
        boolean enabled
) {
}
