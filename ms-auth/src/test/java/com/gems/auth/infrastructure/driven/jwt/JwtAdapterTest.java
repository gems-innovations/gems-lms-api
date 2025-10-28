package com.gems.auth.infrastructure.driven.jwt;

import com.gems.auth.infrastructure.driven.jwt.JwtAdapter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAdapter Tests")
class JwtAdapterTest {

    private JwtAdapter jwtAdapter;
    private final String testSecret = "testSecretKeyThatIsLongEnoughForHS512AlgorithmMinimumRequirements";
    private final int testExpiration = 3600;

    @BeforeEach
    void setUp() {
        jwtAdapter = new JwtAdapter();
        ReflectionTestUtils.setField(jwtAdapter, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtAdapter, "jwtExpiration", testExpiration);
    }

    @Nested
    @DisplayName("Generate Token Tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid token with correct claims")
        void shouldGenerateValidTokenWithCorrectClaims() {
            Long userId = 1L;
            String role = "STUDENT";

            String token = jwtAdapter.generateToken(userId, role);

            assertNotNull(token);
            assertFalse(token.isEmpty());

            SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

            assertEquals(userId.toString(), claims.getSubject());
            assertEquals(role, claims.get("role"));
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
        }

        @Test
        @DisplayName("Should generate token with correct expiration time")
        void shouldGenerateTokenWithCorrectExpirationTime() {
            Long userId = 1L;
            String role = "TEACHER";

            String token = jwtAdapter.generateToken(userId, role);

            SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            long actualExpirationTime = expiration.getTime() - issuedAt.getTime();

            assertEquals(testExpiration * 1000L, actualExpirationTime);
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String role = "STUDENT";

            String token1 = jwtAdapter.generateToken(1L, role);
            String token2 = jwtAdapter.generateToken(2L, role);

            assertNotEquals(token1, token2);

            SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
            Claims claims1 = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token1).getBody();
            Claims claims2 = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token2).getBody();

            assertEquals("1", claims1.getSubject());
            assertEquals("2", claims2.getSubject());
        }

        @Test
        @DisplayName("Should generate different tokens for different roles")
        void shouldGenerateDifferentTokensForDifferentRoles() {
            Long userId = 1L;

            String studentToken = jwtAdapter.generateToken(userId, "STUDENT");
            String teacherToken = jwtAdapter.generateToken(userId, "TEACHER");

            assertNotEquals(studentToken, teacherToken);

            SecretKey key = Keys.hmacShaKeyFor(testSecret.getBytes());
            Claims studentClaims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(studentToken).getBody();
            Claims teacherClaims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(teacherToken).getBody();

            assertEquals("STUDENT", studentClaims.get("role"));
            assertEquals("TEACHER", teacherClaims.get("role"));
        }
    }

    @Nested
    @DisplayName("Validate Token Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtAdapter.generateToken(1L, "STUDENT");

            Boolean isValid = jwtAdapter.validateToken(token);

            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            String invalidToken = "invalid.token.here";

            Boolean isValid = jwtAdapter.validateToken(invalidToken);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should return false for null token")
        void shouldReturnFalseForNullToken() {
            Boolean isValid = jwtAdapter.validateToken(null);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            Boolean isValid = jwtAdapter.validateToken("");

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            String malformedToken = "not.a.valid.jwt.token";

            Boolean isValid = jwtAdapter.validateToken(malformedToken);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should return false for token with wrong signature")
        void shouldReturnFalseForTokenWithWrongSignature() {
            JwtAdapter otherAdapter = new JwtAdapter();
            ReflectionTestUtils.setField(otherAdapter, "jwtSecret", "differentSecretKey");
            ReflectionTestUtils.setField(otherAdapter, "jwtExpiration", testExpiration);

            String token = otherAdapter.generateToken(1L, "STUDENT");

            Boolean isValid = jwtAdapter.validateToken(token);

            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Get User ID From Token Tests")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("Should return correct user ID from valid token")
        void shouldReturnCorrectUserIdFromValidToken() {
            Long expectedUserId = 123L;
            String token = jwtAdapter.generateToken(expectedUserId, "STUDENT");

            Long actualUserId = jwtAdapter.getUserIdFromToken(token);

            assertEquals(expectedUserId, actualUserId);
        }

        @Test
        @DisplayName("Should return correct user ID for different users")
        void shouldReturnCorrectUserIdForDifferentUsers() {
            Long userId1 = 1L;
            Long userId2 = 999L;

            String token1 = jwtAdapter.generateToken(userId1, "STUDENT");
            String token2 = jwtAdapter.generateToken(userId2, "TEACHER");

            assertEquals(userId1, jwtAdapter.getUserIdFromToken(token1));
            assertEquals(userId2, jwtAdapter.getUserIdFromToken(token2));
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            String invalidToken = "invalid.token.here";

            assertThrows(Exception.class, () -> jwtAdapter.getUserIdFromToken(invalidToken));
        }

        @Test
        @DisplayName("Should throw exception for null token")
        void shouldThrowExceptionForNullToken() {
            assertThrows(Exception.class, () -> jwtAdapter.getUserIdFromToken(null));
        }
    }

    @Nested
    @DisplayName("Get Role From Token Tests")
    class GetRoleFromTokenTests {

        @Test
        @DisplayName("Should return correct role from valid token")
        void shouldReturnCorrectRoleFromValidToken() {
            String expectedRole = "TEACHER";
            String token = jwtAdapter.generateToken(1L, expectedRole);

            String actualRole = jwtAdapter.getRoleFromToken(token);

            assertEquals(expectedRole, actualRole);
        }

        @Test
        @DisplayName("Should return correct role for different roles")
        void shouldReturnCorrectRoleForDifferentRoles() {
            String studentToken = jwtAdapter.generateToken(1L, "STUDENT");
            String teacherToken = jwtAdapter.generateToken(2L, "TEACHER");

            assertEquals("STUDENT", jwtAdapter.getRoleFromToken(studentToken));
            assertEquals("TEACHER", jwtAdapter.getRoleFromToken(teacherToken));
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            String invalidToken = "invalid.token.here";

            assertThrows(Exception.class, () -> jwtAdapter.getRoleFromToken(invalidToken));
        }

        @Test
        @DisplayName("Should throw exception for null token")
        void shouldThrowExceptionForNullToken() {
            assertThrows(Exception.class, () -> jwtAdapter.getRoleFromToken(null));
        }
    }

    @Nested
    @DisplayName("Secret Key Handling Tests")
    class SecretKeyHandlingTests {

        @Test
        @DisplayName("Should handle short secret key by padding")
        void shouldHandleShortSecretKeyByPadding() {
            JwtAdapter shortSecretAdapter = new JwtAdapter();
            ReflectionTestUtils.setField(shortSecretAdapter, "jwtSecret", "short");
            ReflectionTestUtils.setField(shortSecretAdapter, "jwtExpiration", testExpiration);

            String token = shortSecretAdapter.generateToken(1L, "STUDENT");

            assertNotNull(token);
            assertTrue(shortSecretAdapter.validateToken(token));
        }

        @Test
        @DisplayName("Should handle long secret key correctly")
        void shouldHandleLongSecretKeyCorrectly() {
            String longSecret = "a".repeat(100);
            JwtAdapter longSecretAdapter = new JwtAdapter();
            ReflectionTestUtils.setField(longSecretAdapter, "jwtSecret", longSecret);
            ReflectionTestUtils.setField(longSecretAdapter, "jwtExpiration", testExpiration);

            String token = longSecretAdapter.generateToken(1L, "STUDENT");

            assertNotNull(token);
            assertTrue(longSecretAdapter.validateToken(token));
        }

        @Test
        @DisplayName("Should handle exact minimum length secret key")
        void shouldHandleExactMinimumLengthSecretKey() {
            String exactLengthSecret = "a".repeat(64);
            JwtAdapter exactLengthAdapter = new JwtAdapter();
            ReflectionTestUtils.setField(exactLengthAdapter, "jwtSecret", exactLengthSecret);
            ReflectionTestUtils.setField(exactLengthAdapter, "jwtExpiration", testExpiration);

            String token = exactLengthAdapter.generateToken(1L, "STUDENT");

            assertNotNull(token);
            assertTrue(exactLengthAdapter.validateToken(token));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero user ID")
        void shouldHandleZeroUserId() {
            String token = jwtAdapter.generateToken(0L, "STUDENT");

            assertEquals(0L, jwtAdapter.getUserIdFromToken(token));
            assertEquals("STUDENT", jwtAdapter.getRoleFromToken(token));
        }

        @Test
        @DisplayName("Should handle negative user ID")
        void shouldHandleNegativeUserId() {
            String token = jwtAdapter.generateToken(-1L, "STUDENT");

            assertEquals(-1L, jwtAdapter.getUserIdFromToken(token));
            assertEquals("STUDENT", jwtAdapter.getRoleFromToken(token));
        }

        @Test
        @DisplayName("Should handle very large user ID")
        void shouldHandleVeryLargeUserId() {
            Long largeUserId = Long.MAX_VALUE;
            String token = jwtAdapter.generateToken(largeUserId, "STUDENT");

            assertEquals(largeUserId, jwtAdapter.getUserIdFromToken(token));
            assertEquals("STUDENT", jwtAdapter.getRoleFromToken(token));
        }

        @Test
        @DisplayName("Should handle empty role")
        void shouldHandleEmptyRole() {
            String token = jwtAdapter.generateToken(1L, "");

            assertEquals("", jwtAdapter.getRoleFromToken(token));
        }

        @Test
        @DisplayName("Should handle null role")
        void shouldHandleNullRole() {
            String token = jwtAdapter.generateToken(1L, null);

            assertNull(jwtAdapter.getRoleFromToken(token));
        }
    }
}
