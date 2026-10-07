package com.gems.education.infrastructure.driven.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Asks ms-auth for the institution's users with the caller's own token (staff may list their
 * institution). Fails closed: if ms-auth cannot answer, the operation is refused with a 503.
 */
@Component
public class AuthServiceInstitutionMembers implements InstitutionMembers {
  private final WebClient client;

  public AuthServiceInstitutionMembers(WebClient.Builder builder,
                                       @Value("${services.auth.url:http://localhost:8081}") String authUrl) {
    this.client = builder.baseUrl(authUrl).build();
  }

  @Override
  public Mono<Void> requireMembers(Collection<Long> userIds, String institutionId) {
    List<Long> ids = userIds == null ? List.of() : userIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return Mono.empty();
    return CurrentUser.token()
      .switchIfEmpty(Mono.error(new ForbiddenException("Authentication required")))
      .flatMap(token -> client.get()
        .uri("/api/v1/users/institution/{id}", institutionId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .bodyToFlux(JsonNode.class)
        .map(user -> user.path("userId").asLong())
        .collect(Collectors.toSet())
        .timeout(Duration.ofSeconds(5))
        .onErrorMap(e -> e instanceof WebClientResponseException.Forbidden,
          e -> new ForbiddenException("You cannot manage the users of this institution"))
        .onErrorMap(e -> !(e instanceof ForbiddenException),
          e -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify the users with ms-auth")))
      .flatMap(members -> missing(ids, members).isEmpty()
        ? Mono.<Void>empty()
        : Mono.error(new ForbiddenException("These users do not belong to the institution: " + missing(ids, members))));
  }

  private static List<Long> missing(List<Long> ids, Set<Long> members) {
    return ids.stream().filter(id -> !members.contains(id)).toList();
  }
}
