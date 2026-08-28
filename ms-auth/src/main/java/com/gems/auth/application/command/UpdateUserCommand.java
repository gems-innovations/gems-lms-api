package com.gems.auth.application.command;

public class UpdateUserCommand {
  private final Long id;
  private final String name;
  private final String role;
  private final String institutionId;

  public UpdateUserCommand(Long id, String name, String role, String institutionId) {
    this.id = id;
    this.name = name;
    this.role = role;
    this.institutionId = institutionId;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getRole() { return role; }
  public String getInstitutionId() { return institutionId; }
}
