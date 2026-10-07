package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

/** Delivers the e-mail verification link to the user. */
public interface EmailVerificationNotifier {
  Mono<Void> sendVerification(String email, String firstName, String token);
}
