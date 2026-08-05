package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import reactor.core.publisher.Flux;

public class GetUsersByInstitutionUseCase {
  private final UserGateway userGateway;

  public GetUsersByInstitutionUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Flux<UserResponse> execute(String institutionId) {
    return userGateway.findByInstitutionId(institutionId)
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
