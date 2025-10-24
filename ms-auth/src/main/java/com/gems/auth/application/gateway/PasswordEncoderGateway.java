package com.gems.auth.application.gateway;

public interface PasswordEncoderGateway {
  String encode(String rawPassword);
  Boolean matches(String rawPassword, String encodedPassword);
}