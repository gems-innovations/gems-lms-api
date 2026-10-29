package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

public interface UserDirectory {
  Mono<UserProfile> find(Long userId);

  /** {@code firstName} y {@code lastName} por separado sirven para el nombre corto de los comentarios de Taps. */
  record UserProfile(Long id, String fullName, String institutionId, String firstName, String lastName) {
    public UserProfile(Long id, String fullName, String institutionId) {
      this(id, fullName, institutionId, null, null);
    }
  }
}
