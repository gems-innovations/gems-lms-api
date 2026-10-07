package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class GetUsersByInstitutionUseCase {
  private final UserGateway userGateway;

  public GetUsersByInstitutionUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Flux<UserResponse> execute(String institutionId) {
    return userGateway.findByInstitutionId(institutionId).map(GetUsersByInstitutionUseCase::toResponse);
  }

  /** One page of the institution's users matching {@code search}, with the total that match. */
  public Mono<UserPage> search(String institutionId, String search, int limit, long offset) {
    return Mono.zip(
      userGateway.searchByInstitution(institutionId, search, limit, offset).map(GetUsersByInstitutionUseCase::toResponse).collectList(),
      userGateway.countByInstitution(institutionId, search)
    ).map(t -> new UserPage(t.getT1(), t.getT2()));
  }

  public record UserPage(List<UserResponse> users, long total) {
  }

  private static UserResponse toResponse(User user) {
    return new UserResponse(
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
      );
  }
}
