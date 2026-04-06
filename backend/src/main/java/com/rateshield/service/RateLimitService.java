package com.rateshield.service;

import com.rateshield.dto.request.RateLimitCheckRequest;
import com.rateshield.model.RateLimitDecision;

public interface RateLimitService {

    RateLimitDecision checkRateLimit(RateLimitCheckRequest request);
}
