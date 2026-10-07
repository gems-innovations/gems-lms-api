package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** One-time password reset tokens. Only a hash of each token is stored. */
public interface PasswordResetGateway {
  Mono<Void> save(Long userId, String tokenHash, LocalDateTime expiresAt);

  /** The user of an unused, unexpired token; empty otherwise. */
  Mono<Long> findValidUser(String tokenHash, LocalDateTime now);

  /** Marks the token used; emits false if it was already used (lost a race). */
  Mono<Boolean> markUsed(String tokenHash, LocalDateTime now);
}
