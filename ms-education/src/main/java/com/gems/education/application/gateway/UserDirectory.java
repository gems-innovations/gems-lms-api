package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

public interface UserDirectory {
  Mono<UserProfile> find(Long userId);

  /**
   * {@code firstName} y {@code lastName} por separado sirven para el nombre corto de los comentarios de Taps;
   * {@code email}, para saber si la cuenta es de invitado.
   */
  record UserProfile(Long id, String fullName, String institutionId, String firstName, String lastName, String email) {
    public UserProfile(Long id, String fullName, String institutionId) {
      this(id, fullName, institutionId, null, null, null);
    }
  }
}
