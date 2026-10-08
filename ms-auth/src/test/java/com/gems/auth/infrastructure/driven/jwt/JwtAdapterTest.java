package com.gems.auth.infrastructure.driven.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtAdapterTest {

  private JwtAdapter jwtAdapter;

  @BeforeEach
  void setUp() {
    jwtAdapter = new JwtAdapter();
    ReflectionTestUtils.setField(jwtAdapter, "jwtSecret", "mySecretKeyForTestingPurposesThisIsALongSecretKeyToEnsureItMeetsTheMinimumLengthRequirement");
    ReflectionTestUtils.setField(jwtAdapter, "jwtExpiration", 3600000); // 1 hour in milliseconds
  }

  @Test
  void shouldGenerateTokenSuccessfully() {
    // Given
    Long userId = 123L;
    String role = "STUDENT";

    // When
    String token = jwtAdapter.generateToken(userId, role);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals(3, token.split("\\.").length); // JWT has 3 parts
  }

  @Test
  void tokenLifetimeUsesMillisecondsAndIncludesTheAccountRevision() {
    String token = jwtAdapter.generateToken(123L, "STUDENT", "inst-1", "revision-1");
    String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
      java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(payload.contains("\"sessionRevision\":\"revision-1\""));
    var claims = io.jsonwebtoken.Jwts.parserBuilder()
      .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
        "mySecretKeyForTestingPurposesThisIsALongSecretKeyToEnsureItMeetsTheMinimumLengthRequirement".getBytes()))
      .build().parseClaimsJws(token).getBody();
    assertEquals(3600000, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
  }

  @Test
  void shouldValidateValidToken() {
    // Given
    Long userId = 123L;
    String role = "STUDENT";
    String token = jwtAdapter.generateToken(userId, role);

    // When
    Boolean isValid = jwtAdapter.validateToken(token);

    // Then
    assertTrue(isValid);
  }

  @Test
  void shouldNotValidateInvalidToken() {
    // Given
    String invalidToken = "invalid.token.value";

    // When
    Boolean isValid = jwtAdapter.validateToken(invalidToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  void shouldNotValidateMalformedToken() {
    // Given
    String malformedToken = "not-a-jwt-token";

    // When
    Boolean isValid = jwtAdapter.validateToken(malformedToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  void shouldExtractUserIdFromToken() {
    // Given
    Long userId = 456L;
    String role = "INSTRUCTOR";
    String token = jwtAdapter.generateToken(userId, role);

    // When
    Long extractedUserId = jwtAdapter.getUserIdFromToken(token);

    // Then
    assertEquals(userId, extractedUserId);
  }

  @Test
  void shouldExtractRoleFromToken() {
    // Given
    Long userId = 789L;
    String role = "ADMIN";
    String token = jwtAdapter.generateToken(userId, role);

    // When
    String extractedRole = jwtAdapter.getRoleFromToken(token);

    // Then
    assertEquals(role, extractedRole);
  }

  @Test
  void shouldRejectShortSecretInsteadOfPaddingIt() {
    // Given
    JwtAdapter shortSecretAdapter = new JwtAdapter();
    ReflectionTestUtils.setField(shortSecretAdapter, "jwtSecret", "short");
    ReflectionTestUtils.setField(shortSecretAdapter, "jwtExpiration", 3600);

    // When / Then
    assertThrows(RuntimeException.class, () -> shortSecretAdapter.generateToken(100L, "USER"));
  }

  @Test
  void shouldHandleDifferentRoles() {
    // Test different role types
    String[] roles = {"STUDENT", "INSTRUCTOR", "ADMIN", "SUPER_ADMIN"};

    for (String role : roles) {
      // When
      String token = jwtAdapter.generateToken(1L, role);
      String extractedRole = jwtAdapter.getRoleFromToken(token);

      // Then
      assertEquals(role, extractedRole);
    }
  }

  @Test
  void shouldGenerateDifferentTokensForDifferentUsers() {
    // Given
    Long userId1 = 1L;
    Long userId2 = 2L;
    String role = "STUDENT";

    // When
    String token1 = jwtAdapter.generateToken(userId1, role);
    String token2 = jwtAdapter.generateToken(userId2, role);

    // Then
    assertNotEquals(token1, token2);
  }
}
