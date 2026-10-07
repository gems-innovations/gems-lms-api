package com.gems.education.infrastructure.driven.auth;

import com.gems.education.application.gateway.EmailNoticeGateway;
import com.gems.shared.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Sends the notice to ms-auth's internal endpoint with the caller's token and the shared internal key. */
@Component
public class AuthEmailNotices implements EmailNoticeGateway {
  private static final Logger log = LoggerFactory.getLogger(AuthEmailNotices.class);

  private final WebClient client;
  private final String internalKey;

  public AuthEmailNotices(WebClient.Builder builder,
                          @Value("${services.auth.url:http://localhost:8081}") String authUrl,
                          @Value("${jwt.secret}") String internalKey) {
    this.client = builder.baseUrl(authUrl).build();
    this.internalKey = internalKey;
  }

  @Override
  public Mono<Void> send(List<Long> userIds, String subject, String message, String linkPath, String linkLabel) {
    if (userIds == null || userIds.isEmpty()) return Mono.empty();
    return CurrentUser.token()
      .flatMap(token -> client.post()
        .uri("/internal/notifications/email")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .header("X-Internal-Key", internalKey)
        .bodyValue(Map.of("userIds", userIds, "subject", subject, "message", message,
          "linkPath", linkPath, "linkLabel", linkLabel))
        .retrieve()
        .toBodilessEntity()
        .timeout(Duration.ofSeconds(3)))
      .then()
      .onErrorResume(error -> {
        log.warn("Course notice could not be handed to ms-auth for e-mail: {}", error.getMessage());
        return Mono.empty();
      });
  }
}
