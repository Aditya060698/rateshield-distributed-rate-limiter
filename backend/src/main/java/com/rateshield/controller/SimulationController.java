package com.rateshield.controller;

import com.rateshield.dto.request.SimulateRateLimitRequest;
import com.rateshield.dto.response.ApiResponse;
import com.rateshield.dto.response.SimulationResponse;
import com.rateshield.service.RateLimitingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulationController {

    private final RateLimitingService rateLimitingService;

    public SimulationController(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/simulate")
    public ApiResponse<SimulationResponse> simulate(@Valid @RequestBody SimulateRateLimitRequest request) {
        SimulationResponse response = rateLimitingService.simulate(request);
        return ApiResponse.success("Simulation completed", response);
    }
}
