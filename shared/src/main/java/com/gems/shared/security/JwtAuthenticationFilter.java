package com.gems.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
@Order(3)
public class JwtAuthenticationFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    @Value("${auth.login.path}")
    private String loginPath;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        if (shouldSkipAuthentication(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        if (!validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        try {
            Claims claims = getClaimsFromToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = request.mutate()
                .header(AuthConstants.X_USER_ID_HEADER, userId.toString())
                .header(AuthConstants.X_USER_ROLE_HEADER, role)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception _) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    private boolean shouldSkipAuthentication(String path) {
        if (path.equals(loginPath) || path.equals("/api/v1/auth/register")) {
            return true;
        }
        
        return path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars") ||
               path.equals("/swagger-ui.html") ||
               path.equals("/swagger-ui/index.html");
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(AuthConstants.AUTHORIZATION_HEADER);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authHeaders.getFirst();
        if (authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            return authHeader.substring(AuthConstants.BEARER_PREFIX.length());
        }

        return null;
    }

    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception _) {
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}

