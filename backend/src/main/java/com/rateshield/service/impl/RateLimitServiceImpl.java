package com.rateshield.service.impl;

import com.rateshield.dto.request.RateLimitCheckRequest;
import com.rateshield.model.RateLimitDecision;
import com.rateshield.service.RateLimitService;
import org.springframework.stereotype.Service;

@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Override
    public RateLimitDecision checkRateLimit(RateLimitCheckRequest request) {
        return new RateLimitDecision(true, 0, 0, "Rate limiter not implemented yet");
    }
}
