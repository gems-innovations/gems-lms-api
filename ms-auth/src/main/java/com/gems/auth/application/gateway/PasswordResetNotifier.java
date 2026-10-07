package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

/** Delivers the password reset link to the user (e-mail in production). */
public interface PasswordResetNotifier {
  Mono<Void> sendResetLink(String email, String firstName, String token);
}
