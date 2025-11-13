package com.gems.auth.infrastructure.driving.rest.request;

import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for user registration")
public class RegisterUserRequest {

  @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 50)
  @NotBlank(message = RestConstants.NAME_REQUIRED_MESSAGE)
  @Size(min = 2, max = 50, message = RestConstants.NAME_SIZE_MESSAGE)
  private String name;

  @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = RestConstants.EMAIL_REQUIRED_MESSAGE)
  @Email(message = RestConstants.EMAIL_VALID_MESSAGE)
  private String email;

  @Schema(
      description = "User's password. Must contain at least 8 characters, including one lowercase, one uppercase, one digit, and one special character",
      example = "SecurePass123!",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 8
  )
  @NotBlank(message = RestConstants.PASSWORD_REQUIRED_MESSAGE)
  @Size(min = 8, message = RestConstants.PASSWORD_SIZE_MESSAGE)
  @Pattern(
      regexp = RestConstants.PASSWORD_PATTERN_REGEX,
      message = RestConstants.PASSWORD_PATTERN_MESSAGE
  )
  private String password;

  @Schema(description = "User's role in the system", example = "STUDENT", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"STUDENT", "TEACHER", "ADMIN"})
  @NotBlank(message = "Role is required")
  private String role;

  public RegisterUserRequest() {
  }

  public RegisterUserRequest(String name, String email, String password, String role) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
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

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}