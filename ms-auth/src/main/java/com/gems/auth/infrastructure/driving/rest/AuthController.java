package com.gems.auth.infrastructure.driving.rest;

import com.gems.shared.security.CurrentUser;
import com.gems.auth.application.ChangePasswordUseCase;
import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.PasswordRecoveryUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.auth.infrastructure.driving.rest.mapper.LoginMapper;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import com.gems.auth.infrastructure.driving.rest.response.ErrorResponse;
import com.gems.auth.infrastructure.driving.rest.schemas.LoginResponseSchema;
import com.gems.auth.infrastructure.driving.rest.schemas.UserResponseSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH)
@Tag(name = "Auth", description = "User management and authentication endpoints")
public class AuthController {
  private final RegisterUserUseCase registerUserUseCase;
  private final LoginUseCase loginUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;
  private final PasswordRecoveryUseCase passwordRecoveryUseCase;

  public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase,
                        ChangePasswordUseCase changePasswordUseCase, PasswordRecoveryUseCase passwordRecoveryUseCase) {
    this.registerUserUseCase = registerUserUseCase;
    this.loginUseCase = loginUseCase;
    this.changePasswordUseCase = changePasswordUseCase;
    this.passwordRecoveryUseCase = passwordRecoveryUseCase;
  }

  /** The signed-in user replaces their password (required after signing in with a temporary one). */
  @PostMapping("/change-password")
  @Operation(summary = "Change the signed-in user's password")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<ResponseEntity<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
    return CurrentUser.get()
      .flatMap(caller -> changePasswordUseCase.execute(caller.userId(), request.currentPassword(), request.newPassword()))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  /** Always 202, whether or not the e-mail has an account, so accounts cannot be discovered. */
  @PostMapping("/forgot-password")
  @Operation(summary = "Send a password reset link")
  public Mono<ResponseEntity<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    return passwordRecoveryUseCase.requestReset(request.email())
      .thenReturn(ResponseEntity.accepted().<Void>build());
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Set a new password with a reset link token")
  public Mono<ResponseEntity<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
    return passwordRecoveryUseCase.reset(request.token(), request.newPassword())
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  public record ChangePasswordRequest(String currentPassword, String newPassword) {
  }

  public record ForgotPasswordRequest(String email) {
  }

  public record ResetPasswordRequest(String token, String newPassword) {
  }

  @PostMapping(AuthInfraConstants.REGISTER_ENDPOINT)
  @Operation(
      summary = "Register a new user",
      description = "Creates a new user account with the provided information. The user must provide a valid name, email, password, and role."
  )
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "User successfully registered",
          content = @Content(schema = @Schema(implementation = UserResponseSchema.class))
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
    // Accounts are created by administrators: an admin inside their own institution (defaulting
    // to it) and never as SUPER_ADMIN. The first super admin comes from SuperAdminBootstrap.
    return CurrentUser.get()
      .flatMap(caller -> {
        if (caller.isAdmin() && request.getInstitutionId() == null) {
          request.setInstitutionId(caller.institutionId());
        }
        UserController.ensureCanAssign(caller, request.getRole(), request.getInstitutionId());
        return registerUserUseCase.execute(UserMapper.toDomain(request));
      })
      .map(userResponse -> ResponseEntity.status(HttpStatus.CREATED).body(userResponse));
  }

  @PostMapping(AuthInfraConstants.LOGIN_ENDPOINT)
  @Operation(
      summary = "User login",
      description = "Authenticates a user with email and password. Returns a JWT token upon successful authentication."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Login successful, returns JWT token and user information",
          content = @Content(schema = @Schema(implementation = LoginResponseSchema.class))
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
    return loginUseCase.execute(command).map(ResponseEntity::ok);
  }
}
