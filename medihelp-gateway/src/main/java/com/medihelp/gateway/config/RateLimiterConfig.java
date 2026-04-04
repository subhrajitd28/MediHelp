package com.medihelp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown"
        );
    }

    @Bean("defaultRateLimiter")
    public RedisRateLimiter defaultRateLimiter() {
        // 100 requests per second, burst of 200
        return new RedisRateLimiter(100, 200);
    }

    @Bean("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        // Stricter for auth endpoints: 10 requests per second, burst of 20
        return new RedisRateLimiter(10, 20);
    }

    @Bean("aiRateLimiter")
    public RedisRateLimiter aiRateLimiter() {
        // AI endpoints: 20 requests per second, burst of 30
        return new RedisRateLimiter(20, 30);
    }
}
