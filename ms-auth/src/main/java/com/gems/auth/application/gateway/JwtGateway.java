package com.gems.auth.application.gateway;

public interface JwtGateway {
  String generateToken(Long userId, String role, String institutionId);

  default String generateToken(Long userId, String role) {
    return generateToken(userId, role, null);
  }
  Boolean validateToken(String token);
  Long getUserIdFromToken(String token);
  String getRoleFromToken(String token);
}
