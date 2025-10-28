package com.gems.auth.application.command;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;

public class RegisterUserCommand {
  private final UserName name;
  private final Email email;
  private final Password password;
  private final UserRole role;

  public RegisterUserCommand(String name, String email, String password, String role) {
    this.name = new UserName(name);
    this.email = new Email(email);
    this.password = new Password(password);
    this.role = UserRole.valueOf(role.toUpperCase());
  }

  public UserName getName() {
    return name;
  }

  public Email getEmail() {
    return email;
  }

  public Password getPassword() {
    return password;
  }

  public UserRole getRole() {
    return role;
  }
}
