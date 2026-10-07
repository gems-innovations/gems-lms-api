package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public class GetAllUsersUseCase {
  private final UserGateway userGateway;

  public GetAllUsersUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Flux<UserResponse> execute() {
    return userGateway.findAll()
      .map(user -> new UserResponse(
        user.getId().getValue(),
        user.getFirstName().getValue(),
        user.getLastName().getValue(),
        user.getUsername(),
        user.getEmail().getValue(),
        user.getRole().name(),
        user.getInstitutionId(),
        user.getAvatarUrl(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.isActive()
      ));
  }

  /** Active users per institution id. */
  public Mono<Map<String, Long>> countByInstitution() {
    return userGateway.countActiveUsersByInstitution();
  }
}
