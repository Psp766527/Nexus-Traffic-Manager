package com.daimlertrucksasia.it.dsc.nexus.traffic.manager.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

//TODO: this class is not required as of now. For the future it will be helpful.
@Configuration
public class IPKeyResolver {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
