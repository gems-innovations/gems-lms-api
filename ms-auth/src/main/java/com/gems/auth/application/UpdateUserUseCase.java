package com.gems.auth.application;

import com.gems.auth.application.command.UpdateUserCommand;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateUserUseCase {
  private final UserGateway userGateway;

  public UpdateUserUseCase(UserGateway userGateway) {
    this.userGateway = userGateway;
  }

  public Mono<UserResponse> execute(UpdateUserCommand command) {
    return userGateway.findById(new UserId(command.getId()))
      .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + command.getId())))
      .flatMap(existing -> {
        String username = command.getUsername() != null && !command.getUsername().isBlank()
          ? command.getUsername()
          : existing.getUsername();

        User updated = new User(
          existing.getId(),
          new UserName(command.getFirstName()),
          new UserName(command.getLastName()),
          username,
          existing.getEmail(),
          existing.getPassword(),
          UserRole.valueOf(command.getRole()),
          command.getInstitutionId(),
          existing.getAvatarUrl(),
          existing.getCreatedAt(),
          LocalDateTime.now(),
          existing.isActive()
        );
        return userGateway.save(updated);
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
