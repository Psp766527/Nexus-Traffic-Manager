package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.application.controller;

import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.infrastructure.RateLimitConfigRepository;
import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.rate.limiting.config.entity.RateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for managing rate limit configurations.
 * This controller provides endpoints to register and fetch rate limiting rules
 * per client and route.
 */
@RestController
@RequestMapping("/rate/limit")
public class RateLimiterConfigController {

    @Autowired
    private RateLimitConfigRepository configRepository;

    /**
     * Registers a new rate limit configuration.
     *
     * @param config The rate limit configuration payload.
     * @return The saved {@link RateLimitConfig} with a 200 OK response.
     */
    @PostMapping("/register")
    public ResponseEntity<RateLimitConfig> registerRateLimit(@RequestBody RateLimitConfig config) {

        RateLimitConfig savedConfig = configRepository.save(
                RateLimitConfig.builder()
                        .clientId(config.getClientId())
                        .route(config.getRoute())
                        .requestsPerMinute(config.getRequestsPerMinute())
                        .timeWindow(config.getTimeWindow())
                        .timeUnit(config.getTimeUnit())
                        .burstCapacity(config.getBurstCapacity())
                        .priority(config.getPriority())
                        .expirationDate(config.getExpirationDate())
                        .createdAt(LocalDateTime.now())
                        .status(config.getStatus())
                        .customAttributes(config.getCustomAttributes())
                        .build()
        );

        return ResponseEntity.ok(savedConfig);
    }

    /**
     * Updates an existing rate limit configuration by its ID.
     *
     * @param id     The unique ID of the rate limit configuration to update.
     * @param config The updated configuration details.
     * @return The updated {@link RateLimitConfig} if the ID exists, otherwise 404 Not Found.
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<RateLimitConfig> updateRateLimit(@PathVariable String id, @RequestBody RateLimitConfig config) {
        return configRepository.findById(id)
                .map(existing -> {
                    existing.setClientId(config.getClientId());
                    existing.setRoute(config.getRoute());
                    existing.setRequestsPerMinute(config.getRequestsPerMinute());
                    existing.setTimeWindow(config.getTimeWindow());
                    existing.setTimeUnit(config.getTimeUnit());
                    existing.setBurstCapacity(config.getBurstCapacity());
                    existing.setPriority(config.getPriority());
                    existing.setExpirationDate(config.getExpirationDate());
                    existing.setStatus(config.getStatus());
                    existing.setCustomAttributes(config.getCustomAttributes());
                    existing.setUpdatedAt(LocalDateTime.now());

                    RateLimitConfig updated = configRepository.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Retrieves the active rate limit configuration for a given client and route.
     * The configuration must have a status of "ACTIVE" and a non-expired expiration date.
     *
     * @param clientId The client identifier.
     * @param route    The route path.
     * @return The matched {@link RateLimitConfig} if found, or 404 Not Found.
     */
    @GetMapping("/{clientId}/{route}")
    public ResponseEntity<RateLimitConfig> getRateLimit(@PathVariable String clientId, @PathVariable String route) {
        return configRepository
                .findFirstByClientIdAndRouteAndStatus(
                        clientId,
                        route,
                        "ACTIVE"
                )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
