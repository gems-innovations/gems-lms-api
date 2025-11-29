package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Order(2)
public class RateLimitFilter implements WebFilter {

    @Value("${rate.limit.requests}")
    private int maxRequests;
    
    @Value("${rate.limit.window}")
    private int windowSeconds;
    
    @Value("${rate.limit.key.prefix}")
    private String keyPrefix;
    
    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String clientId = getClientId(request);
        String key = keyPrefix + clientId;
        
        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    return redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                        .then(Mono.just(count));
                }
                return Mono.just(count);
            })
            .flatMap(count -> {
                if (count > maxRequests) {
                    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(maxRequests));
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, RateLimitConstants.ZERO_REMAINING);
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() + windowSeconds * RateLimitConstants.MILLISECONDS_PER_SECOND));
                    return response.setComplete();
                }
                
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(maxRequests));
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, String.valueOf(Math.max(0, maxRequests - count)));
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() + windowSeconds * RateLimitConstants.MILLISECONDS_PER_SECOND));
                
                return chain.filter(exchange);
            })
            .onErrorResume(error -> {
                return chain.filter(exchange);
            });
    }
    
    private String getClientId(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst(RateLimitConstants.X_FORWARDED_FOR_HEADER);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst(RateLimitConstants.X_REAL_IP_HEADER);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : RateLimitConstants.UNKNOWN_CLIENT;
    }
}

