package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** Safe fallback that never writes a reset token or personal data to application logs. */
public class UndeliveredPasswordResetNotifier implements PasswordResetNotifier {
  private static final Logger log = LoggerFactory.getLogger(UndeliveredPasswordResetNotifier.class);

  @Override
  public Mono<Void> sendResetLink(String email, String firstName, String token) {
    log.warn("[password-reset] Delivery skipped because no mail provider is configured");
    return Mono.empty();
  }
}
