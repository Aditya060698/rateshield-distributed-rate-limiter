package com.rateshield.dto.response;

public record SimulationResponse(
        String status,
        boolean allowed,
        int remainingTokens,
        long retryAfterSeconds,
        String message
) {
}
