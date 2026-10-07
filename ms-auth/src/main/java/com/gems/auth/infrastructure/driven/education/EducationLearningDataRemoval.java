package com.gems.auth.infrastructure.driven.education;

import com.gems.auth.application.gateway.LearningDataRemovalGateway;
import com.gems.auth.domain.values.UserId;
import com.gems.shared.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class EducationLearningDataRemoval implements LearningDataRemovalGateway {
  private final WebClient client;

  public EducationLearningDataRemoval(@Value("${EDUCATION_SERVICE_URL:http://localhost:8083}") String baseUrl) {
    client = WebClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public Mono<Void> remove(UserId userId) {
    return CurrentUser.token().switchIfEmpty(Mono.error(new IllegalStateException("Caller token required")))
      .flatMap(token -> client.delete().uri("/api/v1/students/{id}/learning-data", userId.getValue())
        .headers(headers -> headers.setBearerAuth(token)).retrieve().toBodilessEntity().then())
      .timeout(Duration.ofSeconds(10))
      .onErrorMap(error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "Learning data could not be removed; the account has been kept. Retry deletion.", error));
  }
}
