package com.gems.auth.application;

import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

public class GetUserByIdUseCase {
  private final UserGateway userGateway;

  public GetUserByIdUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Mono<UserResponse> execute(Long id) {
    return userGateway.findById(new UserId(id))
      .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)))
      .map(user -> new UserResponse(
        user.getId().getValue(),
        user.getName().getValue(),
        user.getEmail().getValue(),
        user.getRole().name(),
        user.getInstitutionId(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.isActive()
      ));
  }
}
