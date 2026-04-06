package com.rateshield.dto.response;

public record PolicyResponse(
        String policyId,
        String policyName,
        String algorithm,
        Integer limit,
        Integer windowSizeInSeconds,
        boolean enabled
) {
}
