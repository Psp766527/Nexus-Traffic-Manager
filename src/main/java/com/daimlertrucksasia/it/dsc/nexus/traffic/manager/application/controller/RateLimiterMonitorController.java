package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.application.controller;

import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.service.RateLimiterService;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for monitoring the current state of all in-memory rate limiter buckets.
 * <p>
 * Exposes endpoints to inspect available tokens and configuration capacity
 * for each client-route pair currently tracked by the rate limiter.
 */
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class RateLimiterMonitorController {

    /**
     * Service responsible for handling rate limiting logic and bucket/cache access.
     */
    private final RateLimiterService rateLimiterService;

    /**
     * Returns the current available tokens and capacity for all cached rate limiter buckets.
     *
     * @return A map where each key represents a clientId:route string, and the value is a sub-map containing:
     * <ul>
     *     <li><b>availableTokens</b> – current available tokens in the bucket</li>
     *     <li><b>capacity</b> – configured request capacity for that bucket</li>
     * </ul>
     */
    @GetMapping
    public Map<String, Object> getAllLimits() {
        Map<String, Bucket> buckets = rateLimiterService.getBucketCache();
        Map<String, Long> configs = rateLimiterService.getConfigCache();

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            String key = entry.getKey();
            Bucket bucket = entry.getValue();
            long capacity = configs.getOrDefault(key, -1L);

            result.put(key, Map.of(
                    "availableTokens", bucket.getAvailableTokens(),
                    "capacity", capacity
            ));
        }

        return result;
    }
}
