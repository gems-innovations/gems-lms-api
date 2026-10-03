package com.gems.auth.application.command;

public class UpdateUserCommand {
  private final Long id;
  private final String firstName;
  private final String lastName;
  private final String username;
  private final String role;
  private final String institutionId;
  private final String avatarUrl;

  public UpdateUserCommand(Long id, String firstName, String lastName, String username, String role, String institutionId) {
    this(id, firstName, lastName, username, role, institutionId, null);
  }

  public UpdateUserCommand(Long id, String firstName, String lastName, String username, String role, String institutionId, String avatarUrl) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.role = role;
    this.institutionId = institutionId;
    this.avatarUrl = avatarUrl;
  }

  public Long getId() { return id; }
  public String getFirstName() { return firstName; }
  public String getLastName() { return lastName; }
  public String getUsername() { return username; }
  public String getRole() { return role; }
  public String getInstitutionId() { return institutionId; }
  public String getAvatarUrl() { return avatarUrl; }
}
