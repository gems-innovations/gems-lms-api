package com.gems.auth.infrastructure.driven.notification;

import com.gems.auth.application.gateway.EmailVerificationNotifier;
import reactor.core.publisher.Mono;

/** Used when no SMTP server is configured: the message is dropped without logging the token. */
public class UndeliveredEmailVerificationNotifier implements EmailVerificationNotifier {
  @Override
  public Mono<Void> sendVerification(String email, String firstName, String token) {
    return Mono.empty();
  }
}
