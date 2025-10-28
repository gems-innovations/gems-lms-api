package com.gems.auth.infrastructure.driving.rest.request;

import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterUserRequest {

  @NotBlank(message = RestConstants.NAME_REQUIRED_MESSAGE)
  @Size(min = 2, max = 50, message = RestConstants.NAME_SIZE_MESSAGE)
  private String name;

  @NotBlank(message = RestConstants.EMAIL_REQUIRED_MESSAGE)
  @Email(message = RestConstants.EMAIL_VALID_MESSAGE)
  private String email;

  @NotBlank(message = RestConstants.PASSWORD_REQUIRED_MESSAGE)
  @Size(min = 8, message = RestConstants.PASSWORD_SIZE_MESSAGE)
  @Pattern(
      regexp = RestConstants.PASSWORD_PATTERN_REGEX,
      message = RestConstants.PASSWORD_PATTERN_MESSAGE
  )
  private String password;

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