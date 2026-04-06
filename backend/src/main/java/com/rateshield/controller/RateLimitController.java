package com.rateshield.controller;

import com.rateshield.dto.request.RateLimitCheckRequest;
import com.rateshield.dto.response.ApiResponse;
import com.rateshield.model.RateLimitDecision;
import com.rateshield.service.RateLimitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rate-limits")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/check")
    public ApiResponse<RateLimitDecision> checkRateLimit(@Valid @RequestBody RateLimitCheckRequest request) {
        RateLimitDecision decision = rateLimitService.checkRateLimit(request);
        return ApiResponse.success("Rate limit check completed", decision);
    }
}
