package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * Requests per client and window, counted in Redis. It belongs at the edge (the gateway): the
 * microservices sit behind it and set rate.limit.enabled=false, otherwise every request would be
 * counted twice. Forwarding headers are trusted only when the direct peer is a local or private
 * proxy (nginx, the gateway); a client on the internet cannot pick its own key with them.
 * <p>
 * A request with a validly signed token is counted per user, not per address: a whole campus
 * usually reaches the platform through one NAT address, and a per-address budget would let a
 * few dozen active students lock everyone else out. Anonymous requests (sign-in, password
 * reset) are still counted per address, with their own budget.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "rate.limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter implements WebFilter {

    @Value("${rate.limit.requests}")
    private int maxRequests;
    
    /** Budget per address for requests without a valid token. */
    @Value("${rate.limit.anonymous-requests:${rate.limit.requests}}")
    private int maxAnonymousRequests;

    @Value("${jwt.secret:}")
    private String jwtSecret;

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
        
        String user = jwtSecret.isBlank() ? null : signedSubject(request, jwtSecret);
        String key = keyPrefix + (user != null ? "user:" + user : getClientId(request));
        int limit = user != null ? maxRequests : maxAnonymousRequests;

        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    return redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
                        .then(Mono.just(count));
                }
                return Mono.just(count);
            })
            .flatMap(count -> {
                if (count > limit) {
                    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(limit));
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, RateLimitConstants.ZERO_REMAINING);
                    response.getHeaders().add(RateLimitConstants.RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() + windowSeconds * RateLimitConstants.MILLISECONDS_PER_SECOND));
                    return response.setComplete();
                }
                
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(limit));
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, String.valueOf(Math.max(0, limit - count)));
                response.getHeaders().add(RateLimitConstants.RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() + windowSeconds * RateLimitConstants.MILLISECONDS_PER_SECOND));
                
                return chain.filter(exchange);
            })
            .onErrorResume(error -> {
                return chain.filter(exchange);
            });
    }
    
    /** Subject of a correctly signed bearer token; null when absent, forged or expired. */
    static String signedSubject(ServerHttpRequest request, String secret) {
        String header = request.getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) return null;
        try {
            return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(JwtReactiveAuthenticationManager.signingKey(secret))
                .build()
                .parseClaimsJws(header.substring(7).trim())
                .getBody()
                .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    static String getClientId(ServerHttpRequest request) {
        java.net.InetSocketAddress remote = request.getRemoteAddress();
        java.net.InetAddress peer = remote != null ? remote.getAddress() : null;
        boolean fromProxy = peer != null && (peer.isLoopbackAddress() || peer.isSiteLocalAddress());
        if (fromProxy) {
            String xRealIp = request.getHeaders().getFirst(RateLimitConstants.X_REAL_IP_HEADER);
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
            String xForwardedFor = request.getHeaders().getFirst(RateLimitConstants.X_FORWARDED_FOR_HEADER);
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String[] hops = xForwardedFor.split(",");
                return hops[hops.length - 1].trim();
            }
        }
        return peer != null ? peer.getHostAddress() : RateLimitConstants.UNKNOWN_CLIENT;
    }
}
