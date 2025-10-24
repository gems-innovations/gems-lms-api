package com.gems.auth.application.command;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;

public class LoginCommand {
  private final Email email;
  private final Password password;

  public LoginCommand(String email, String password) {
    this.email = new Email(email);
    this.password = new Password(password);
  }

  public Email getEmail() {
    return email;
  }

  public Password getPassword() {
    return password;
  }
}
