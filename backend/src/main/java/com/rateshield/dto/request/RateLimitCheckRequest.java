package com.rateshield.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTOs are used to expose only the data needed by the API layer instead of leaking
 * internal request-processing objects directly to clients.
 */
public record RateLimitCheckRequest(
        @NotBlank(message = "Subject type is required")
        String subjectType,

        @NotBlank(message = "Subject id is required")
        String subjectId,

        @NotBlank(message = "Resource is required")
        String resource
) {
}
