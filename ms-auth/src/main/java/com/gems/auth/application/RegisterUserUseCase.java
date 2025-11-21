package com.gems.auth.application;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserRole;
import reactor.core.publisher.Mono;

public class RegisterUserUseCase {
  private final UserGateway userGateway;
  private final PasswordEncoderGateway passwordEncoderGateway;

  public RegisterUserUseCase(UserGateway userGateway, PasswordEncoderGateway passwordEncoderGateway) {
    this.userGateway = userGateway;
    this.passwordEncoderGateway = passwordEncoderGateway;
  }

  public Mono<UserResponse> execute(RegisterUserCommand command) {
    return userGateway.existsByEmail(new Email(command.email()))
      .flatMap(exists -> {
        if (Boolean.TRUE.equals(exists)) {
          return Mono.error(new UserAlreadyExistsException(
            String.format(AuthAppConstants.USER_ALREADY_EXISTS_MESSAGE, command.email())
          ));
        }

        String userPassword = new Password(command.password()).getValue();
        String encodedPassword = passwordEncoderGateway.encode(userPassword);

        User user = new User(command.name(), command.email(), encodedPassword, UserRole.fromString(command.role()));

        return userGateway.save(user)
          .map(savedUser -> new UserResponse(
            savedUser.getId().getValue(),
            savedUser.getName().getValue(),
            savedUser.getEmail().getValue(),
            savedUser.getRole().name(),
            savedUser.getCreatedAt(),
            savedUser.getUpdatedAt(),
            savedUser.isActive()
          ));
      });
  }
}
