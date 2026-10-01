package com.gems.auth.infrastructure.driving.rest.request;

import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for user registration")
public class RegisterUserRequest {

  @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 50)
  @NotBlank(message = AuthInfraConstants.NAME_REQUIRED_MESSAGE)
  @Size(min = 2, max = 50, message = AuthInfraConstants.NAME_SIZE_MESSAGE)
  private String firstName;

  @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 50)
  @NotBlank(message = AuthInfraConstants.NAME_REQUIRED_MESSAGE)
  @Size(min = 2, max = 50, message = AuthInfraConstants.NAME_SIZE_MESSAGE)
  private String lastName;

  @Schema(description = "Unique username. Auto-generated from the name when omitted.", example = "john.doe")
  private String username;

  @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = AuthInfraConstants.EMAIL_REQUIRED_MESSAGE)
  @Email(message = AuthInfraConstants.EMAIL_VALID_MESSAGE)
  private String email;

  @Schema(
      description = "User's password. Must contain at least 8 characters, including one lowercase, one uppercase, one digit, and one special character. When omitted, a temporary password is generated and returned in the response.",
      example = "SecurePass123!",
      minLength = 8
  )
  @Pattern(
      regexp = AuthInfraConstants.PASSWORD_PATTERN_REGEX,
      message = AuthInfraConstants.PASSWORD_PATTERN_MESSAGE
  )
  private String password;

  @Schema(description = "User's role in the system", example = "STUDENT", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"STUDENT", "INSTRUCTOR", "ADMIN", "SUPER_ADMIN"})
  @NotBlank(message = "Role is required")
  private String role;

  @Schema(description = "User's institution ID", example = "inst-001")
  private String institutionId;

  public RegisterUserRequest() {
  }

  public RegisterUserRequest(String firstName, String lastName, String email, String password, String role, String institutionId) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.password = password;
    this.role = role;
    this.institutionId = institutionId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }
}
