package com.rateshield.controller;

import com.rateshield.dto.response.ApiResponse;
import com.rateshield.dto.response.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        HealthResponse response = new HealthResponse("UP", "RateShield backend is running");
        return ApiResponse.success("Health check successful", response);
    }
}
