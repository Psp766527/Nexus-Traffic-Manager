package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.rate.limiting.config.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity class representing a rate limiting configuration for a specific client and route.
 * This configuration defines how many requests per time window are allowed, along with optional burst capacity.
 *
 * <p>Stored in the MongoDB collection <b>rate_limit_config</b>.</p>
 *
 * <p>A unique constraint is applied on the combination of {@code clientId} and {@code route}.</p>
 */
@Data
@Builder
@Document(collection = "rate_limit_config")
@CompoundIndexes({
        @CompoundIndex(name = "client_route_idx", def = "{'clientId': 1, 'route': 1}", unique = true)
})
public class RateLimitConfig {

    /**
     * Unique identifier for the configuration document (MongoDB document ID).
     */
    @Id
    private String id;

    /**
     * The unique identifier of the client for whom this rate limit config applies.
     */
    @Indexed(unique = true)
    private String clientId;

    /**
     * The API route/path for which this rate limiting configuration is applicable.
     */
    @Indexed
    private String route;

    /**
     * Maximum number of requests allowed per minute (base rate limit).
     */
    private long requestsPerMinute;

    /**
     * The length of the rate-limiting window (e.g., 300 seconds).
     */
    private long timeWindow;

    /**
     * The unit of time for {@code timeWindow}. Example values: "SECONDS", "MINUTES", "HOURS".
     */
    private String timeUnit;

    /**
     * Number of additional requests allowed beyond the base limit within a short burst.
     */
    private long burstCapacity;

    /**
     * Priority level for the config (e.g., 1 = highest, 5 = lowest). Can be used for sorting or overrides.
     */
    private int priority;

    /**
     * Expiration date of the configuration. Used to deactivate time-bound rules.
     */
    private LocalDateTime expirationDate;

    /**
     * Status of the configuration (e.g., "ACTIVE", "INACTIVE", "PENDING").
     */
    private String status;

    /**
     * Timestamp when the configuration was created.
     * Automatically set by Spring Data.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to the configuration.
     * Automatically maintained by Spring Data.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * A flexible map to store additional key-value pair attributes, such as retry settings or flags.
     */
    private Map<String, String> customAttributes;
}
