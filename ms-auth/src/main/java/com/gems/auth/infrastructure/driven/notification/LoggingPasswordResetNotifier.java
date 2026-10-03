package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Used when no SMTP server is configured (MAIL_HOST empty): writes the reset link to the service log
 * so it can be followed in development. See {@link NotificationConfig}.
 */
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {
  private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

  private final String frontendUrl;

  public LoggingPasswordResetNotifier(String frontendUrl) {
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
  }

  @Override
  public Mono<Void> sendResetLink(String email, String firstName, String token) {
    log.warn("[password-reset] No e-mail provider configured. Reset link for {}: {}/auth/reset-password?token={}",
      email, frontendUrl, token);
    return Mono.empty();
  }
}
