package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.mapper.LoginMapper;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(RestConstants.USERS_API_BASE_PATH)
public class UserController {
  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUseCase loginUseCase;
  private final DisableUserUseCase disableUserUseCase;

  public UserController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase, DisableUserUseCase disableUserUseCase) {
    this.registerUserUseCase = registerUserUseCase;
    this.loginUseCase = loginUseCase;
    this.disableUserUseCase = disableUserUseCase;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  public Mono<ResponseEntity<UserResponse>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
    RegisterUserCommand command = UserMapper.toDomain(request);

    return registerUserUseCase.execute(command)
      .map(userResponse -> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
      })
      .onErrorResume(UserAlreadyExistsException.class, ex ->
        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
      );
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginCommand command = LoginMapper.toCommand(request);

    return loginUseCase.execute(command)
      .map(ResponseEntity::ok)
      .onErrorResume(InvalidCredentialsException.class, error ->
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
      .onErrorResume(UserNotFoundException.class, error ->
        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
      .onErrorResume(Exception.class, error ->
        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> disableUser(@PathVariable("id") Long id) {
    UserId userId = new UserId(id);

    return disableUserUseCase.execute(userId)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorResume(UserNotFoundException.class,
                    ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
            .onErrorResume(Exception.class,
                    ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
  }
}
