package com.gems.education.infrastructure.driven.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class AuthUserDirectory implements UserDirectory {
  private final WebClient client;

  public AuthUserDirectory(WebClient.Builder builder,
      @Value("${services.auth.url:http://localhost:8081}") String authUrl) {
    this.client = builder.baseUrl(authUrl).build();
  }

  @Override
  public Mono<UserProfile> find(Long userId) {
    return CurrentUser.token().switchIfEmpty(Mono.error(new ForbiddenException("Authentication required")))
      .flatMap(token -> client.get().uri("/api/v1/users/{id}", userId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve().bodyToMono(JsonNode.class))
      .map(user -> new UserProfile(user.path("userId").asLong(),
        (user.path("firstName").asText("") + " " + user.path("lastName").asText("")).trim(),
        user.path("institutionId").asText()))
      .timeout(Duration.ofSeconds(5))
      .onErrorMap(e -> !(e instanceof ForbiddenException), e -> new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Could not load the certificate holder"));
  }
}
