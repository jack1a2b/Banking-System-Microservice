package com.banking.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * KeyResolver based on client IP address.
     * Each unique IP will have its own rate limit bucket.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress()
        );
    }

    /**
     * Alternative: KeyResolver based on request path.
     * Uncomment if you want rate limiting per endpoint instead of per IP.
     */
    // @Bean
    // public KeyResolver pathKeyResolver() {
    //     return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    // }
}
