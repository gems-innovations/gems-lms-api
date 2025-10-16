package com.gems.auth.application.gateway;

import com.gems.auth.domain.values.Password;

public interface PasswordEncoderGateway {
  String encode(Password password);

  boolean matches(Password rawPassword, String encodedPassword);
}
