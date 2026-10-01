package com.gems.auth.application;

import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

public class ToggleUserStatusUseCase {
  private final UserGateway userGateway;

  public ToggleUserStatusUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Mono<UserResponse> execute(UserId userId) {
    return userGateway.findById(userId)
      .switchIfEmpty(Mono.error(new UserNotFoundException(
        String.format(AuthAppConstants.USER_NOT_FOUND_MESSAGE, userId.getValue())
      )))
      .flatMap(user -> {
        if (Boolean.TRUE.equals(user.isActive())) {
          user.deactivate();
        } else {
          user.activate();
        }
        return userGateway.save(user);
      })
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
}
