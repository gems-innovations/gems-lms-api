package com.gems.shared.security;

import reactor.core.publisher.Mono;

/** Checks current account state so signed tokens cannot retain stale permissions. */
public interface SessionValidator {
  Mono<State> state(Long userId, String token);

  record State(String revision, String role, String institutionId, boolean active, boolean mustChangePassword) {}
}
