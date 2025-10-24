package com.gems.api.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        response.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.getHeaders().add("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
        response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        response.getHeaders().add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.getHeaders().add("X-Permitted-Cross-Domain-Policies", "none");
        
        return chain.filter(exchange);
    }
}
