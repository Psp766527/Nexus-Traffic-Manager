package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.filter;

import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.rate.limiting.config.entity.RateLimitConfig;
import com.daimlertrucksasia.it.dsc.nexus.traffic.manager.service.RateLimiterService;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom rate limiter filter for Spring Cloud Gateway using Bucket4j.
 * This filter enforces request rate limits per client and route.
 */
@Slf4j
@Component("RateLimiterFilter")
public class RateLimiterFilter extends AbstractGatewayFilterFactory<RateLimiterFilter.Config> {

    /**
     * Service responsible for providing rate limiting configurations and resolving token buckets.
     */
    private final RateLimiterService rateLimiterService;

    /**
     * Constructor initializing the filter with a given {@link RateLimiterService}.
     *
     * @param rateLimiterService the rate limiter service to be used
     */
    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        super(Config.class);
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Logs that the filter has been loaded after bean construction.
     */
    @PostConstruct
    public void init() {
        log.info("✅ {} loaded", this.getClass().getSimpleName());
    }

    /**
     * Configuration class for this filter. Currently has no fields,
     * but can be extended for future configuration needs.
     */
    public static class Config {
        // No specific fields for now, can be extended later
    }

    /**
     * Returns the gateway filter that applies rate limiting logic.
     *
     * @param config the filter configuration (currently unused)
     * @return the gateway filter to be applied
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = exchange.getRequest().getHeaders().getFirst("X-Client-Id");
            String path = exchange.getRequest().getPath().toString();

            log.info("Applying RateLimiter filter for clientId: {}, path: {}", clientId, path);

            if (clientId == null || clientId.isBlank()) {
                log.warn("Missing X-Client-Id header for request to {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }

            RateLimitConfig configObj = rateLimiterService.getConfig(clientId, path);

            int maxRetries = 0;
            long retryDelay = 1000; // Default: 1 second

            if (configObj != null && configObj.getCustomAttributes() != null) {
                try {
                    String maxRetriesStr = configObj.getCustomAttributes().get("maxRetries");
                    String retryDelayStr = configObj.getCustomAttributes().get("retryDelay");

                    if (maxRetriesStr != null) maxRetries = Integer.parseInt(maxRetriesStr);
                    if (retryDelayStr != null) retryDelay = Long.parseLong(retryDelayStr);
                } catch (NumberFormatException e) {
                    log.error("Error parsing customAttributes for clientId {}: {}", clientId, e.getMessage());
                }
            }

            return applyRateLimitWithRetry(exchange, chain, clientId, path, maxRetries, retryDelay, 0);
        };
    }

    /**
     * Attempts to apply rate limiting with retries based on configuration.
     *
     * @param exchange   the current server exchange
     * @param chain      the current gateway filter chain
     * @param clientId   the client identifier
     * @param path       the request path
     * @param maxRetries the maximum number of retry attempts
     * @param retryDelay the delay between retries (in milliseconds)
     * @param attempt    the current retry attempt count
     * @return a {@link Mono<Void>} indicating the result of the request handling
     */
    private Mono<Void> applyRateLimitWithRetry(ServerWebExchange exchange, GatewayFilterChain chain,
                                               String clientId, String path, int maxRetries, long retryDelay, int attempt) {

        Bucket bucket = rateLimiterService.resolveBucket(clientId, path);

        return Mono.defer(() -> {
            if (bucket.tryConsume(1)) {
                log.debug("✅ Request allowed - clientId: {}, path: {}, attempt: {}", clientId, path, attempt);
                return chain.filter(exchange);
            } else {
                log.warn("❌ Rate limit exceeded - clientId: {}, path: {}, attempt: {}", clientId, path, attempt);
                if (attempt < maxRetries) {
                    return Mono.delay(Duration.ofMillis(retryDelay))
                            .then(applyRateLimitWithRetry(exchange, chain, clientId, path, maxRetries, retryDelay, attempt + 1));
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            }
        });
    }
}
