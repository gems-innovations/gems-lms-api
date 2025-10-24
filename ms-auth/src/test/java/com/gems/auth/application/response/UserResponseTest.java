package com.gems.auth.application.response;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.UserName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserResponse Tests")
class UserResponseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user response with all parameters")
        void shouldCreateUserResponseWithAllParameters() {
            Long id = 1L;
            UserName name = new UserName("John Doe");
            Email email = new Email("john.doe@example.com");
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();
            boolean active = true;

            UserResponse response = new UserResponse(id, name, email, createdAt, updatedAt, active);

            assertNotNull(response);
            assertEquals(id, response.getId());
            assertEquals(name.getValue(), response.getName());
            assertEquals(email.getValue(), response.getEmail());
            assertEquals(createdAt, response.getCreatedAt());
            assertEquals(updatedAt, response.getUpdatedAt());
            assertEquals(active, response.isActive());
        }

        @Test
        @DisplayName("Should create user response with inactive user")
        void shouldCreateUserResponseWithInactiveUser() {
            Long id = 2L;
            UserName name = new UserName("Jane Doe");
            Email email = new Email("jane.doe@example.com");
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();
            boolean active = false;

            UserResponse response = new UserResponse(id, name, email, createdAt, updatedAt, active);

            assertNotNull(response);
            assertEquals(id, response.getId());
            assertEquals(name.getValue(), response.getName());
            assertEquals(email.getValue(), response.getEmail());
            assertEquals(createdAt, response.getCreatedAt());
            assertEquals(updatedAt, response.getUpdatedAt());
            assertEquals(active, response.isActive());
        }

        @Test
        @DisplayName("Should throw exception when creating user response with null values")
        void shouldThrowExceptionWhenCreatingUserResponseWithNullValues() {
            assertThrows(NullPointerException.class, () -> 
                new UserResponse(null, null, null, null, null, false));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct ID")
        void shouldReturnCorrectId() {
            Long id = 1L;
            UserResponse response = new UserResponse(id, new UserName("John Doe"), new Email("john@example.com"), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(id, response.getId());
        }

        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            String name = "John Doe";
            UserResponse response = new UserResponse(1L, new UserName(name), new Email("john@example.com"), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(name, response.getName());
        }

        @Test
        @DisplayName("Should return correct email")
        void shouldReturnCorrectEmail() {
            String email = "john.doe@example.com";
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email(email), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(email, response.getEmail());
        }

        @Test
        @DisplayName("Should return correct creation time")
        void shouldReturnCorrectCreationTime() {
            LocalDateTime createdAt = LocalDateTime.now();
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email("john@example.com"), 
                createdAt, LocalDateTime.now(), true);

            assertEquals(createdAt, response.getCreatedAt());
        }

        @Test
        @DisplayName("Should return correct update time")
        void shouldReturnCorrectUpdateTime() {
            LocalDateTime updatedAt = LocalDateTime.now();
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email("john@example.com"), 
                LocalDateTime.now(), updatedAt, true);

            assertEquals(updatedAt, response.getUpdatedAt());
        }

        @Test
        @DisplayName("Should return correct active status")
        void shouldReturnCorrectActiveStatus() {
            boolean active = true;
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email("john@example.com"), 
                LocalDateTime.now(), LocalDateTime.now(), active);

            assertEquals(active, response.isActive());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String name = "José María O'Connor-Smith";
            UserResponse response = new UserResponse(1L, new UserName(name), new Email("john@example.com"), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(name, response.getName());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String email = "user+tag@example-domain.com";
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email(email), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(email, response.getEmail());
        }

        @Test
        @DisplayName("Should throw exception for very long name")
        void shouldThrowExceptionForVeryLongName() {
            String name = "a".repeat(100);
            
            assertThrows(IllegalArgumentException.class, () -> 
                new UserResponse(1L, new UserName(name), new Email("john@example.com"), 
                    LocalDateTime.now(), LocalDateTime.now(), true));
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            String email = "a".repeat(100) + "@example.com";
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email(email), 
                LocalDateTime.now(), LocalDateTime.now(), true);

            assertEquals(email, response.getEmail());
        }

        @Test
        @DisplayName("Should handle future timestamps")
        void shouldHandleFutureTimestamps() {
            LocalDateTime futureTime = LocalDateTime.now().plusYears(1);
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email("john@example.com"), 
                futureTime, futureTime, true);

            assertEquals(futureTime, response.getCreatedAt());
            assertEquals(futureTime, response.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle past timestamps")
        void shouldHandlePastTimestamps() {
            LocalDateTime pastTime = LocalDateTime.now().minusYears(1);
            UserResponse response = new UserResponse(1L, new UserName("John Doe"), new Email("john@example.com"), 
                pastTime, pastTime, true);

            assertEquals(pastTime, response.getCreatedAt());
            assertEquals(pastTime, response.getUpdatedAt());
        }
    }
}
