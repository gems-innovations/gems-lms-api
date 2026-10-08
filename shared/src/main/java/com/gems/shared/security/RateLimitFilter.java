package com.gems.shared.security;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.OptionalLong;

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
    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

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

        String user = jwtSecret.isBlank() ? null : signedSubject(request, jwtSecret);
        String key = keyPrefix + (user != null ? "user:" + user : getClientId(request));
        int limit = user != null ? maxRequests : maxAnonymousRequests;

        // Only a Redis failure fails open. The downstream chain runs exactly once, outside of the
        // error handling: otherwise an error raised by the application would run the request again.
        return count(key)
            .map(OptionalLong::of)
            .onErrorResume(error -> {
                LOG.warn("Rate limiting skipped, Redis unavailable: {}", error.toString());
                return Mono.just(OptionalLong.empty());
            })
            .flatMap(count -> count.isEmpty() ? chain.filter(exchange) : admit(exchange, chain, count.getAsLong(), limit));
    }

    private Mono<Long> count(String key) {
        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> count == 1
                ? redisTemplate.expire(key, Duration.ofSeconds(windowSeconds)).thenReturn(count)
                : Mono.just(count));
    }

    private Mono<Void> admit(ServerWebExchange exchange, WebFilterChain chain, long count, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        String reset = String.valueOf(System.currentTimeMillis() + windowSeconds * RateLimitConstants.MILLISECONDS_PER_SECOND);
        response.getHeaders().add(RateLimitConstants.RATE_LIMIT_LIMIT_HEADER, String.valueOf(limit));
        response.getHeaders().add(RateLimitConstants.RATE_LIMIT_RESET_HEADER, reset);
        if (count > limit) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, RateLimitConstants.ZERO_REMAINING);
            return response.setComplete();
        }
        response.getHeaders().add(RateLimitConstants.RATE_LIMIT_REMAINING_HEADER, String.valueOf(Math.max(0, limit - count)));
        return chain.filter(exchange);
    }

    /** Subject of a correctly signed bearer token; null when absent, forged or expired. */
    static String signedSubject(ServerHttpRequest request, String secret) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(AuthConstants.BEARER_PREFIX)) return null;
        try {
            return Jwts.parserBuilder()
                .setSigningKey(JwtKeys.signingKey(secret))
                .build()
                .parseClaimsJws(header.substring(AuthConstants.BEARER_PREFIX.length()).trim())
                .getBody()
                .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getClientId(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        InetAddress peer = remote != null ? remote.getAddress() : null;
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
