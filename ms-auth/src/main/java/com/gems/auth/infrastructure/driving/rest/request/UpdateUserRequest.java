package com.gems.auth.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
  private String name;

  @NotBlank(message = "Role is required")
  private String role;

  private String institutionId;

  public UpdateUserRequest() {}

  public UpdateUserRequest(String name, String role, String institutionId) {
    this.name = name;
    this.role = role;
    this.institutionId = institutionId;
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public String getInstitutionId() { return institutionId; }
  public void setInstitutionId(String institutionId) { this.institutionId = institutionId; }
}
