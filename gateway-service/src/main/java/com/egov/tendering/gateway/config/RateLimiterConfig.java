package com.egov.tendering.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiter configuration for the API Gateway
 * Uses Redis to track and limit request rates
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Defines how to determine the key for rate limiting
     * Here we use the request path as the key
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }

    /**
     * Defines a Redis-based rate limiter for general API endpoints
     * Allows 10 requests per second with a burst of 20 requests
     */
    @Bean
    @Primary
    public RedisRateLimiter defaultRedisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /**
     * Defines a more permissive rate limiter for public endpoints
     * Allows 50 requests per second with a burst of 100 requests
     */
    @Bean
    public RedisRateLimiter publicEndpointRateLimiter() {
        return new RedisRateLimiter(50, 100);
    }

    /**
     * Defines a stricter rate limiter for sensitive operations
     * Allows 5 requests per second with a burst of 10 requests
     */
    @Bean
    public RedisRateLimiter sensitiveOperationRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }
}
