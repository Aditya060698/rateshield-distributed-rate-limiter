package com.rateshield.service;

import com.rateshield.dto.request.SimulateRateLimitRequest;
import com.rateshield.dto.response.SimulationResponse;

public interface RateLimitingService {

    SimulationResponse simulate(SimulateRateLimitRequest request);
}
