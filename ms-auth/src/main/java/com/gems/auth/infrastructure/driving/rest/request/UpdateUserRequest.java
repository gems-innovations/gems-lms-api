package com.gems.auth.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

  @NotBlank(message = "First name is required")
  @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
  private String lastName;

  private String username;

  @NotBlank(message = "Role is required")
  private String role;

  private String institutionId;

  @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
  private String avatarUrl;

  public UpdateUserRequest() {}

  public UpdateUserRequest(String firstName, String lastName, String username, String role, String institutionId) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.role = role;
    this.institutionId = institutionId;
  }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public String getInstitutionId() { return institutionId; }
  public void setInstitutionId(String institutionId) { this.institutionId = institutionId; }

  public String getAvatarUrl() { return avatarUrl; }
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
