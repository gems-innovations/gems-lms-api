package com.gems.auth.application;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.constants.UserConstants;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import reactor.core.publisher.Mono;

public class RegisterUserUseCase {
  private final UserGateway userGateway;
  private final PasswordEncoderGateway passwordEncoderGateway;

  public RegisterUserUseCase(UserGateway userGateway, PasswordEncoderGateway passwordEncoderGateway) {
    this.userGateway = userGateway;
    this.passwordEncoderGateway = passwordEncoderGateway;
  }

  public Mono<UserResponse> execute(RegisterUserCommand command) {
    return userGateway.existsByEmail(command.getEmail())
      .flatMap(exists -> {
        if (exists) {
          return Mono.error(new UserAlreadyExistsException(
            String.format(UserConstants.USER_ALREADY_EXISTS_MESSAGE, command.getEmail().getValue())
          ));
        }

        String encodedPassword = passwordEncoderGateway.encode(command.getPassword().getValue());

        User user = new User(command.getName().getValue(), command.getEmail().getValue(), encodedPassword, command.getRole());

        return userGateway.save(user)
          .map(savedUser -> new UserResponse(
            savedUser.getId().getValue(),
            savedUser.getName(),
            savedUser.getEmail(),
            savedUser.getRole(),
            savedUser.getCreatedAt(),
            savedUser.getUpdatedAt(),
            savedUser.isActive()
          ));
      });
  }
}
