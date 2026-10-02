package com.gems.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Validates the JWT extracted by {@link JwtServerAuthenticationConverter} and, when valid,
 * produces an authenticated {@link Authentication} with a ROLE_* authority so that Spring
 * Security's {@code authorizeExchange().anyExchange().authenticated()} recognizes the request.
 */
@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    String token = (String) authentication.getCredentials();

    try {
      Claims claims = Jwts.parserBuilder()
        .setSigningKey(signingKey())
        .build()
        .parseClaimsJws(token)
        .getBody();

      Long userId = Long.parseLong(claims.getSubject());
      String role = claims.get("role", String.class);
      String institutionId = claims.get("institutionId", String.class);

      return Mono.just(new UsernamePasswordAuthenticationToken(
        new AuthenticatedUser(userId, role, institutionId),
        token,
        List.of(new SimpleGrantedAuthority("ROLE_" + role))
      ));
    } catch (Exception e) {
      return Mono.error(new BadCredentialsException("Invalid or expired JWT", e));
    }
  }

  private SecretKey signingKey() {
    byte[] keyBytes = jwtSecret.getBytes();
    if (keyBytes.length * 8 < 512) {
      byte[] paddedKey = new byte[64];
      System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
      return Keys.hmacShaKeyFor(paddedKey);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
