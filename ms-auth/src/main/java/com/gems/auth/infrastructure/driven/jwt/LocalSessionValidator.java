package com.gems.auth.infrastructure.driven.jwt;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.UserId;
import com.gems.shared.security.SessionValidator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Reads account state locally to avoid recursive HTTP authentication. */
@Primary
@Component
public class LocalSessionValidator implements SessionValidator {
  private final UserGateway users;

  public LocalSessionValidator(UserGateway users) {
    this.users = users;
  }

  @Override
  public Mono<State> state(Long userId, String token) {
    return users.findById(new UserId(userId)).map(user -> new State(user.getUpdatedAt().toString(),
      user.getRole().name(), user.getInstitutionId(), Boolean.TRUE.equals(user.isActive()), user.mustChangePassword()));
  }
}
