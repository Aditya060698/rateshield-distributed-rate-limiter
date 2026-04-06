package com.rateshield.dto.request;

import com.rateshield.model.AlgorithmType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimulateRateLimitRequest(
        @NotNull(message = "Algorithm is required")
        AlgorithmType algorithm,

        @NotBlank(message = "Subject type is required")
        String subjectType,

        @NotBlank(message = "Subject id is required")
        String subjectId,

        @NotBlank(message = "Resource is required")
        String resource,

        @NotNull(message = "Limit is required")
        @Min(value = 1, message = "Limit must be greater than zero")
        Integer limit,

        @NotNull(message = "Window size is required")
        @Min(value = 1, message = "Window size must be greater than zero")
        Integer windowSizeInSeconds
) {
}
