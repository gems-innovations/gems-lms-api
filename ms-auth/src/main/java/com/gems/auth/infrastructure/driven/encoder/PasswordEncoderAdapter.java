package com.gems.auth.infrastructure.driven.encoder;

import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.domain.values.Password;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderAdapter implements PasswordEncoderGateway {
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  public PasswordEncoderAdapter() {
    this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
  }

  @Override
  public String encode(Password password) {
    return bCryptPasswordEncoder.encode(password.getValue());
  }

  @Override
  public boolean matches(Password rawPassword, String encodedPassword) {
    return bCryptPasswordEncoder.matches(rawPassword.getValue(), encodedPassword);
  }
}