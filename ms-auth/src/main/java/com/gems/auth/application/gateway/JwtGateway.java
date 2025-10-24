package com.gems.auth.application.gateway;

public interface JwtGateway {
  String generateToken(Long userId, String role);
  Boolean validateToken(String token);
  Long getUserIdFromToken(String token);
  String getRoleFromToken(String token);
}
