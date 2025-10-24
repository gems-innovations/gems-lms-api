package com.gems.auth.application;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.exceptions.InvalidCredentialsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
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
    return userGateway.findByEmail(command.getEmail())
      .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
      .flatMap(user -> {
        if (!user.isActive()) {
          return Mono.error(new InvalidCredentialsException("User account is deactivated"));
        }

        boolean passwordMatches = passwordEncoderGateway.matches(
          command.getPassword().getValue(), 
          user.getPassword().getValue()
        );

        if (!passwordMatches) {
          return Mono.error(new InvalidCredentialsException("Invalid credentials"));
        }

        String token = jwtGateway.generateToken(user.getId().getValue(), user.getRole().name());
        
        return Mono.just(new LoginResponse(
          user.getId().getValue(),
          user.getName().getValue(),
          user.getEmail().getValue(),
          user.getRole(),
          token
        ));
      });
  }
}
