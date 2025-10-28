package com.gems.auth.infrastructure.driven.jwt;

import com.gems.auth.application.gateway.JwtGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
  public String generateToken(Long userId, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000L);

    SecretKey key = getSigningKey();

    return Jwts.builder()
      .setSubject(userId.toString())
      .claim("role", role)
      .setIssuedAt(now)
      .setExpiration(expiryDate)
      .signWith(key, SignatureAlgorithm.HS512)
      .compact();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes();
    
    if (keyBytes.length * 8 < 512) {
      byte[] paddedKey = new byte[64];
      System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
      return Keys.hmacShaKeyFor(paddedKey);
    }
    
    return Keys.hmacShaKeyFor(keyBytes);
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
