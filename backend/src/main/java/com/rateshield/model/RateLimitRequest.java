package com.rateshield.model;

public record RateLimitRequest(
        String subjectType,
        String subjectId,
        String resource
) {
}
