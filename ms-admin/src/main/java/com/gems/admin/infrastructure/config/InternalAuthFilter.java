package com.gems.admin.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class InternalAuthFilter implements WebFilter {

    @Value("${gateway.internal.secret}")
    private String internalSecret;
    
    @Value("${gateway.internal.header}")
    private String internalHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String internalToken = request.getHeaders().getFirst(internalHeader);
        
        if (internalToken == null || !internalToken.equals(internalSecret)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }
}
