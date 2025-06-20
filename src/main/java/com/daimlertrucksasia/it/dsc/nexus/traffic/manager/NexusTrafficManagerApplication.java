package com.daimlertrucksasia.it.dsc.nexus.traffic.manager;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the Nexus Traffic Manager Spring Boot application.
 * <p>
 * This application acts as an API Gateway with support for:
 * <ul>
 *   <li>Dynamic rate limiting</li>
 *   <li>Custom Gateway filters</li>
 *   <li>Service discovery via Spring Cloud (e.g., Eureka)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Packages are explicitly defined for component scanning to ensure that
 * services and filters are properly registered.
 * </p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.daimlertrucksasia.it.dsc.nexus.traffic.manager",
        "com.daimlertrucksasia.it.dsc.nexus.traffic.manager.service",
        "com.daimlertrucksasia.it.dsc.nexus.traffic.manager.filter"
})
@EnableDiscoveryClient
public class NexusTrafficManagerApplication {

    /**
     * Main method that launches the Spring Boot application.
     * Also performs a sanity check to verify if the custom rate limiter bean is registered.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {

        ApplicationContext applicationContextFactory = SpringApplication.run(NexusTrafficManagerApplication.class, args);

        if (applicationContextFactory.containsBean("RateLimiterFilter")) {
            System.out.println("✅ RateLimiter bean is registered in Spring context.");
        } else {
            System.err.println("❌ RateLimiter bean is NOT registered in Spring context.");
        }
    }

    /**
     * CommandLineRunner bean that runs at application startup and logs all
     * registered {@link GatewayFilterFactory} beans in the Spring context.
     * <p>
     * This is useful for debugging and verifying custom filters.
     * </p>
     *
     * @param ctx Spring's application context injected automatically
     * @return a {@link CommandLineRunner} that logs filter beans
     */
    @Bean
    public CommandLineRunner logGatewayFilters(ApplicationContext ctx) {
        return args -> {
            System.out.println("🧩 Registered GatewayFilterFactory beans:");
            for (String name : ctx.getBeanNamesForType(GatewayFilterFactory.class)) {
                System.out.println("🔹 " + name);
            }
        };
    }
}
