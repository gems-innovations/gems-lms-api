package com.gems.auth.infrastructure.driving.rest.request;

import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for user registration")
public class RegisterUserRequest {

  @Schema(description = "User's full name", example = "John Doe", required = true)
  @NotBlank(message = RestConstants.NAME_REQUIRED_MESSAGE)
  @Size(min = 2, max = 50, message = RestConstants.NAME_SIZE_MESSAGE)
  private String name;

  @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
  @NotBlank(message = RestConstants.EMAIL_REQUIRED_MESSAGE)
  @Email(message = RestConstants.EMAIL_VALID_MESSAGE)
  private String email;

  @Schema(description = "User's password (must contain uppercase, lowercase, digit, and special character)", example = "MySecure123!", required = true)
  @NotBlank(message = RestConstants.PASSWORD_REQUIRED_MESSAGE)
  @Size(min = 8, message = RestConstants.PASSWORD_SIZE_MESSAGE)
  @Pattern(
      regexp = RestConstants.PASSWORD_PATTERN_REGEX,
      message = RestConstants.PASSWORD_PATTERN_MESSAGE
  )
  private String password;

  public RegisterUserRequest() {
  }

  public RegisterUserRequest(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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