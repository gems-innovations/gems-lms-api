package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Stand-in until an e-mail provider is configured: writes the reset link to the service log.
 * Replace with an e-mail implementation of {@link PasswordResetNotifier} for production.
 */
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {
  private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

  private final String frontendUrl;

  public LoggingPasswordResetNotifier(@Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  @Override
  public Mono<Void> sendResetLink(String email, String firstName, String token) {
    log.warn("[password-reset] No e-mail provider configured. Reset link for {}: {}/auth/reset-password?token={}",
      email, frontendUrl, token);
    return Mono.empty();
  }
}
