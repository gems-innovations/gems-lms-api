package com.gems.auth.infrastructure.driving.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for user login")
public class LoginRequest {
  @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  @Schema(description = "User's password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  private String password;

  public LoginRequest() {}

  public LoginRequest(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
