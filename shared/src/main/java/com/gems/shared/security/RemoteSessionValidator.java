package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Other services ask ms-auth directly; ms-auth replaces this with a local database validator.
 * <p>
 * Every request passes the gateway and then a service, and each would ask ms-auth again. Answers
 * are kept for a few seconds per token, so a deactivated account or a revoked session stops
 * working at most {@code session.cache.seconds} later (0 turns the cache off).
 */
@Component
public class RemoteSessionValidator implements SessionValidator {
  private static final int MAX_ENTRIES = 50_000;

  private final WebClient client;
  private final long ttlMillis;
  private final Map<String, Cached> cache = new ConcurrentHashMap<>();

  private record Cached(State state, long expiresAt) {}

  public RemoteSessionValidator(@Value("${AUTH_SERVICE_URL:http://localhost:8081}") String baseUrl,
                                @Value("${session.cache.seconds:5}") long ttlSeconds) {
    client = WebClient.builder().baseUrl(baseUrl).build();
    ttlMillis = ttlSeconds * 1000;
  }

  @Override
  public Mono<State> state(Long userId, String token) {
    if (ttlMillis <= 0) return fetch(token);
    long now = System.currentTimeMillis();
    Cached hit = cache.get(token);
    if (hit != null && hit.expiresAt() > now) return Mono.just(hit.state());
    return fetch(token).doOnNext(state -> {
      if (cache.size() >= MAX_ENTRIES) cache.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
      if (cache.size() < MAX_ENTRIES) cache.put(token, new Cached(state, now + ttlMillis));
    });
  }

  private Mono<State> fetch(String token) {
    return client.get().uri("/api/v1/auth/session").headers(h -> h.setBearerAuth(token))
      .retrieve().bodyToMono(State.class).timeout(Duration.ofSeconds(3))
      .onErrorMap(error -> {
        if (error instanceof WebClientResponseException response
            && (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403)) {
          return new BadCredentialsException("Session no longer valid");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable", error);
      });
  }
}
