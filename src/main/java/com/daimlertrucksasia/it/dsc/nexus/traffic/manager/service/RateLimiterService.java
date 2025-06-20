package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.service;

import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.infrastructure.RateLimitConfigRepository;
import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.rate.limiting.config.entity.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service responsible for handling rate limiting logic using {@link Bucket4j}.
 * <p>
 * Each client and route combination is associated with a {@link Bucket} that
 * enforces rate limits based on MongoDB-configured {@link RateLimitConfig}.
 * This service supports caching for performance and provides methods to
 * retrieve or invalidate rate limiting configurations dynamically.
 * </p>
 */
@Service("rateLimiterService")
public class RateLimiterService {

    /**
     * Repository to retrieve rate limit configurations from MongoDB.
     */
    @Autowired
    private RateLimitConfigRepository configRepository;

    /**
     * Cache that stores rate limiter buckets for each client:route pair.
     */
    @Getter
    private final ConcurrentMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * Cache that stores precomputed request limits (requestsPerMinute + burstCapacity).
     */
    @Getter
    private final ConcurrentMap<String, Long> configCache = new ConcurrentHashMap<>();

    /**
     * Resolves the {@link Bucket} associated with the given client and route.
     * If a bucket does not exist in the cache, it will be created and cached.
     *
     * @param clientId The client identifier.
     * @param route    The route/path being accessed.
     * @return A configured {@link Bucket} for rate limiting.
     */
    public Bucket resolveBucket(String clientId, String route) {
        String cacheKey = clientId + ":" + route;

        return bucketCache.computeIfAbsent(cacheKey, k -> {
            Bucket bucket = createNewBucket(clientId, route);

            RateLimitConfig config = findConfig(clientId, route);
            if (config != null) {
                long rpm = config.getRequestsPerMinute();
                long burst = Math.max(0, config.getBurstCapacity());
                configCache.put(cacheKey, rpm + burst);
            }

            return bucket;
        });
    }

    /**
     * Creates a new {@link Bucket} using the provided clientId and route.
     * Falls back to a default configuration if no active config is found.
     *
     * @param clientId The client identifier.
     * @param route    The route/path.
     * @return A newly configured {@link Bucket} instance.
     */
    private Bucket createNewBucket(String clientId, String route) {
        RateLimitConfig config = findConfig(clientId, route);
        if (config == null) {
            // Default fallback config
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                    .build();
        }

        long capacity = config.getRequestsPerMinute() + Math.max(0, config.getBurstCapacity());
        Duration duration = Duration.of(config.getTimeWindow(), ChronoUnit.valueOf(config.getTimeUnit()));
        Refill refill = Refill.greedy(config.getRequestsPerMinute(), duration);

        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, refill))
                .build();
    }

    /**
     * Finds the active {@link RateLimitConfig} for the given client and route.
     *
     * @param clientId The client identifier.
     * @param route    The route/path.
     * @return The active {@link RateLimitConfig}, or {@code null} if not found.
     */
    private RateLimitConfig findConfig(String clientId, String route) {
        return configRepository
                .findFirstByClientIdAndRouteAndStatus(clientId, route, "ACTIVE")
                .orElse(null);
    }

    /**
     * Public method to get the current rate limit configuration for a given client and route.
     *
     * @param clientId The client identifier.
     * @param route    The route/path.
     * @return The active {@link RateLimitConfig}, or {@code null} if not found.
     */
    public RateLimitConfig getConfig(String clientId, String route) {
        return findConfig(clientId, route);
    }

    /**
     * Invalidates the cached bucket and config values for a specific client:route pair.
     * Useful when configurations change dynamically and must be refreshed.
     *
     * @param clientId The client identifier.
     * @param route    The route/path.
     */
    public void invalidateCache(String clientId, String route) {
        bucketCache.remove(clientId + ":" + route);
        configCache.remove(clientId + ":" + route);
    }
}
