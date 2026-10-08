package com.gems.auth.infrastructure.driven.jwt;

import com.gems.auth.application.gateway.JwtGateway;
import com.gems.shared.security.JwtKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtAdapter implements JwtGateway {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private int jwtExpiration;

  @Override
  public String generateToken(Long userId, String role, String institutionId) {
    return generateToken(userId, role, institutionId, null);
  }

  @Override
  public String generateToken(Long userId, String role, String institutionId, String sessionRevision) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    SecretKey key = getSigningKey();

    return Jwts.builder()
      .setSubject(userId.toString())
      .claim("role", role)
      .claim("institutionId", institutionId)
      .claim("sessionRevision", sessionRevision)
      .setIssuedAt(now)
      .setExpiration(expiryDate)
      .signWith(key, SignatureAlgorithm.HS512)
      .compact();
  }

  private SecretKey getSigningKey() {
    return JwtKeys.signingKey(jwtSecret);
  }

  @Override
  public Boolean validateToken(String token) {
    try {
      SecretKey key = getSigningKey();
      Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Long getUserIdFromToken(String token) {
    SecretKey key = getSigningKey();
    Claims claims = Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(token)
      .getBody();
    
    return Long.parseLong(claims.getSubject());
  }

  @Override
  public String getRoleFromToken(String token) {
    SecretKey key = getSigningKey();
    Claims claims = Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(token)
      .getBody();
    
    return claims.get("role", String.class);
  }
}
