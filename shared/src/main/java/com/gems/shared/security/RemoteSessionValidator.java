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

/** Other services ask ms-auth directly; ms-auth replaces this with a local database validator. */
@Component
public class RemoteSessionValidator implements SessionValidator {
  private final WebClient client;

  public RemoteSessionValidator(@Value("${AUTH_SERVICE_URL:http://localhost:8081}") String baseUrl) {
    client = WebClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public Mono<State> state(Long userId, String token) {
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
