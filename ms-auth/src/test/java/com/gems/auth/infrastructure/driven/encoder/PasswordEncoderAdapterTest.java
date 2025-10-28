package com.gems.auth.infrastructure.driven.encoder;

import com.gems.auth.infrastructure.driven.encoder.PasswordEncoderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordEncoderAdapter Tests")
class PasswordEncoderAdapterTest {

    private PasswordEncoderAdapter passwordEncoderAdapter;

    @BeforeEach
    void setUp() {
        passwordEncoderAdapter = new PasswordEncoderAdapter();
    }

    @Nested
    @DisplayName("Encode Tests")
    class EncodeTests {

        @Test
        @DisplayName("Should encode password successfully")
        void shouldEncodePasswordSuccessfully() {
            String rawPassword = "SecurePass123!";

            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            assertNotNull(encodedPassword);
            assertFalse(encodedPassword.isEmpty());
            assertNotEquals(rawPassword, encodedPassword);
            assertTrue(encodedPassword.startsWith("$2a$"));
        }

        @Test
        @DisplayName("Should encode different passwords differently")
        void shouldEncodeDifferentPasswordsDifferently() {
            String password1 = "SecurePass123!";
            String password2 = "DifferentPass456!";

            String encoded1 = passwordEncoderAdapter.encode(password1);
            String encoded2 = passwordEncoderAdapter.encode(password2);

            assertNotEquals(encoded1, encoded2);
            assertNotEquals(password1, encoded1);
            assertNotEquals(password2, encoded2);
        }

        @Test
        @DisplayName("Should encode same password differently each time")
        void shouldEncodeSamePasswordDifferentlyEachTime() {
            String password = "SecurePass123!";

            String encoded1 = passwordEncoderAdapter.encode(password);
            String encoded2 = passwordEncoderAdapter.encode(password);

            assertNotEquals(encoded1, encoded2);
            assertTrue(passwordEncoderAdapter.matches(password, encoded1));
            assertTrue(passwordEncoderAdapter.matches(password, encoded2));
        }

        @Test
        @DisplayName("Should handle empty password")
        void shouldHandleEmptyPassword() {
            String emptyPassword = "";

            String encodedPassword = passwordEncoderAdapter.encode(emptyPassword);

            assertNotNull(encodedPassword);
            assertFalse(encodedPassword.isEmpty());
            assertTrue(passwordEncoderAdapter.matches(emptyPassword, encodedPassword));
        }

        @Test
        @DisplayName("Should handle password with special characters")
        void shouldHandlePasswordWithSpecialCharacters() {
            String password = "SecurePass123!@#$%^&*()";

            String encodedPassword = passwordEncoderAdapter.encode(password);

            assertNotNull(encodedPassword);
            assertTrue(passwordEncoderAdapter.matches(password, encodedPassword));
        }

        @Test
        @DisplayName("Should handle very long password")
        void shouldHandleVeryLongPassword() {
            String longPassword = "a".repeat(100);

            assertThrows(IllegalArgumentException.class, () -> {
                passwordEncoderAdapter.encode(longPassword);
            });
        }

        @Test
        @DisplayName("Should handle password with whitespace")
        void shouldHandlePasswordWithWhitespace() {
            String password = "  SecurePass123!  ";

            String encodedPassword = passwordEncoderAdapter.encode(password);

            assertNotNull(encodedPassword);
            assertTrue(passwordEncoderAdapter.matches(password, encodedPassword));
        }
    }

    @Nested
    @DisplayName("Matches Tests")
    class MatchesTests {

        @Test
        @DisplayName("Should return true for matching password")
        void shouldReturnTrueForMatchingPassword() {
            String rawPassword = "SecurePass123!";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            Boolean matches = passwordEncoderAdapter.matches(rawPassword, encodedPassword);

            assertTrue(matches);
        }

        @Test
        @DisplayName("Should return false for non-matching password")
        void shouldReturnFalseForNonMatchingPassword() {
            String rawPassword = "SecurePass123!";
            String wrongPassword = "WrongPassword123!";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            Boolean matches = passwordEncoderAdapter.matches(wrongPassword, encodedPassword);

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should return false for null raw password")
        void shouldReturnFalseForNullRawPassword() {
            String rawPassword = "SecurePass123!";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            assertThrows(IllegalArgumentException.class, () -> {
                passwordEncoderAdapter.matches(null, encodedPassword);
            });
        }

        @Test
        @DisplayName("Should return false for null encoded password")
        void shouldReturnFalseForNullEncodedPassword() {
            String rawPassword = "SecurePass123!";

            Boolean matches = passwordEncoderAdapter.matches(rawPassword, null);

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should return false for empty raw password")
        void shouldReturnFalseForEmptyRawPassword() {
            String rawPassword = "SecurePass123!";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            Boolean matches = passwordEncoderAdapter.matches("", encodedPassword);

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should return false for empty encoded password")
        void shouldReturnFalseForEmptyEncodedPassword() {
            String rawPassword = "SecurePass123!";

            Boolean matches = passwordEncoderAdapter.matches(rawPassword, "");

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should return false for malformed encoded password")
        void shouldReturnFalseForMalformedEncodedPassword() {
            String rawPassword = "SecurePass123!";
            String malformedEncoded = "not.a.valid.bcrypt.hash";

            Boolean matches = passwordEncoderAdapter.matches(rawPassword, malformedEncoded);

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should handle case sensitivity")
        void shouldHandleCaseSensitivity() {
            String rawPassword = "SecurePass123!";
            String wrongCasePassword = "securepass123!";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            Boolean matches = passwordEncoderAdapter.matches(wrongCasePassword, encodedPassword);

            assertFalse(matches);
        }

        @Test
        @DisplayName("Should handle whitespace differences")
        void shouldHandleWhitespaceDifferences() {
            String rawPassword = "SecurePass123!";
            String passwordWithWhitespace = "  SecurePass123!  ";
            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

            Boolean matches = passwordEncoderAdapter.matches(passwordWithWhitespace, encodedPassword);

            assertFalse(matches);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should encode and match password correctly")
        void shouldEncodeAndMatchPasswordCorrectly() {
            String rawPassword = "SecurePass123!";

            String encodedPassword = passwordEncoderAdapter.encode(rawPassword);
            Boolean matches = passwordEncoderAdapter.matches(rawPassword, encodedPassword);

            assertTrue(matches);
        }

        @Test
        @DisplayName("Should work with different password strengths")
        void shouldWorkWithDifferentPasswordStrengths() {
            String[] passwords = {
                "123456",
                "password",
                "Password123",
                "SecurePass123!",
                "VerySecurePassword123!@#$%^&*()"
            };

            for (String password : passwords) {
                String encoded = passwordEncoderAdapter.encode(password);
                Boolean matches = passwordEncoderAdapter.matches(password, encoded);
                
                assertTrue(matches, "Password matching failed for: " + password);
            }
        }

        @Test
        @DisplayName("Should handle multiple encodings of same password")
        void shouldHandleMultipleEncodingsOfSamePassword() {
            String rawPassword = "SecurePass123!";
            String[] encodedPasswords = new String[5];

            for (int i = 0; i < encodedPasswords.length; i++) {
                encodedPasswords[i] = passwordEncoderAdapter.encode(rawPassword);
            }

            for (String encoded : encodedPasswords) {
                Boolean matches = passwordEncoderAdapter.matches(rawPassword, encoded);
                assertTrue(matches);
            }

            for (int i = 0; i < encodedPasswords.length; i++) {
                for (int j = i + 1; j < encodedPasswords.length; j++) {
                    assertNotEquals(encodedPasswords[i], encodedPasswords[j]);
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle unicode characters in password")
        void shouldHandleUnicodeCharactersInPassword() {
            String password = "SecurePass123!ñáéíóú";

            String encodedPassword = passwordEncoderAdapter.encode(password);
            Boolean matches = passwordEncoderAdapter.matches(password, encodedPassword);

            assertTrue(matches);
        }

        @Test
        @DisplayName("Should handle password with only special characters")
        void shouldHandlePasswordWithOnlySpecialCharacters() {
            String password = "!@#$%^&*()";

            String encodedPassword = passwordEncoderAdapter.encode(password);
            Boolean matches = passwordEncoderAdapter.matches(password, encodedPassword);

            assertTrue(matches);
        }

        @Test
        @DisplayName("Should handle password with only numbers")
        void shouldHandlePasswordWithOnlyNumbers() {
            String password = "1234567890";

            String encodedPassword = passwordEncoderAdapter.encode(password);
            Boolean matches = passwordEncoderAdapter.matches(password, encodedPassword);

            assertTrue(matches);
        }

        @Test
        @DisplayName("Should handle password with only letters")
        void shouldHandlePasswordWithOnlyLetters() {
            String password = "abcdefghijklmnopqrstuvwxyz";

            String encodedPassword = passwordEncoderAdapter.encode(password);
            Boolean matches = passwordEncoderAdapter.matches(password, encodedPassword);

            assertTrue(matches);
        }
    }
}
