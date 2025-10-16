package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.dto.UserResponseDto;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(RestConstants.USERS_API_BASE_PATH)
@Tag(name = "Users", description = "User management operations")
public class UserController {
  private final RegisterUserUseCase registerUserUseCase;

  public UserController(RegisterUserUseCase registerUserUseCase) {
    this.registerUserUseCase = registerUserUseCase;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully registered"),
      @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Mono<ResponseEntity<UserResponseDto>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
    RegisterUserCommand command = new RegisterUserCommand(
      request.getName(),
      request.getEmail(),
      request.getPassword()
    );

    return registerUserUseCase.execute(command)
      .map(userResponse -> {
        UserResponseDto dto = UserMapper.toDto(userResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
      })
      .onErrorResume(UserAlreadyExistsException.class, ex ->
        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
      )
      .onErrorResume(IllegalArgumentException.class, ex ->
        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
      );
  }
}
