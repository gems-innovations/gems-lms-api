package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
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

  public UserController(RegisterUserUseCase registerUserUseCase) {
    this.registerUserUseCase = registerUserUseCase;
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
}
