package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.exceptions.InvalidCredentialsException;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.mapper.LoginMapper;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import com.gems.auth.infrastructure.driving.rest.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "Users", description = "User management and authentication endpoints")
public class UserController {
  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUseCase loginUseCase;

  public UserController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase) {
    this.registerUserUseCase = registerUserUseCase;
    this.loginUseCase = loginUseCase;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  @Operation(
      summary = "Register a new user",
      description = "Creates a new user account with the provided information. The user must provide a valid name, email, password, and role."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "User successfully registered",
          content = @Content(schema = @Schema(implementation = UserResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data or validation errors",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "User already exists with the provided email",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
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
  @Operation(
      summary = "User login",
      description = "Authenticates a user with email and password. Returns a JWT token upon successful authentication."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Login successful, returns JWT token and user information",
          content = @Content(schema = @Schema(implementation = LoginResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid request data or validation errors",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Invalid credentials or user account is deactivated",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "User not found with the provided email",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
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
}
