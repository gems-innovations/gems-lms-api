package com.gems.auth.application;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.exceptions.UserDeactivatedException;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import reactor.core.publisher.Mono;

public class LoginUseCase {
  private final UserGateway userGateway;
  private final PasswordEncoderGateway passwordEncoderGateway;
  private final JwtGateway jwtGateway;

  public LoginUseCase(UserGateway userGateway,
                      PasswordEncoderGateway passwordEncoderGateway,
                      JwtGateway jwtGateway) {
    this.userGateway = userGateway;
    this.passwordEncoderGateway = passwordEncoderGateway;
    this.jwtGateway = jwtGateway;
  }

  public Mono<LoginResponse> execute(LoginCommand command) {
    return userGateway.findByEmail(new Email(command.email()))
      .switchIfEmpty(Mono.error(new UserNotFoundException(String.format(AuthAppConstants.USER_NOT_FOUND_MESSAGE, command.email()))))
      .flatMap(user -> {
        if (Boolean.FALSE.equals(user.isActive())) {
          return Mono.error(new UserDeactivatedException(AuthAppConstants.USER_DEACTIVATED_MESSAGE));
        }

        String userPassword = new Password(command.password()).getValue();

        boolean passwordMatches = passwordEncoderGateway.matches(
          userPassword,
          user.getPassword().getValue()
        );

        if (!passwordMatches) {
          return Mono.error(new InvalidCredentialsException(AuthAppConstants.INVALID_CREDENTIALS_MESSAGE));
        }

        String token = jwtGateway.generateToken(user.getId().getValue(), user.getRole().name());

        return Mono.just(new LoginResponse(
          user.getId().getValue(),
          user.getName().getValue(),
          user.getEmail().getValue(),
          user.getRole().name(),
          token,
          user.getInstitutionId()
        ));
      });
  }
}
