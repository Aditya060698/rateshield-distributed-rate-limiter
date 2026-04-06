package com.rateshield.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs are used to keep the external API contract separate from internal domain models.
 * This makes validation, versioning, and future refactoring much easier.
 */
public record CreatePolicyRequest(
        @NotBlank(message = "Policy name is required")
        String policyName,

        @NotBlank(message = "Algorithm is required")
        String algorithm,

        @NotNull(message = "Limit is required")
        @Min(value = 1, message = "Limit must be greater than zero")
        Integer limit,

        @NotNull(message = "Window size is required")
        @Min(value = 1, message = "Window size must be greater than zero")
        Integer windowSizeInSeconds
) {
}
