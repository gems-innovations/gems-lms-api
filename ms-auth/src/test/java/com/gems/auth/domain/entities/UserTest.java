package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

  @Test
  void shouldCreateUserWithIdNamesEmailPasswordAndRole() {
    // Given
    Long id = 1L;
    String firstName = "John";
    String lastName = "Doe";
    String username = "john.doe";
    String email = "john.doe@example.com";
    String password = "Password123!";
    UserRole role = UserRole.STUDENT;

    // When
    User user = new User(id, firstName, lastName, username, email, password, role, null, null);

    // Then
    assertNotNull(user.getId());
    assertEquals(id, user.getId().getValue());
    assertEquals(firstName, user.getFirstName().getValue());
    assertEquals(lastName, user.getLastName().getValue());
    assertEquals(username, user.getUsername());
    assertEquals(email.toLowerCase(), user.getEmail().getValue());
    assertEquals(password, user.getPassword().getValue());
    assertEquals(role, user.getRole());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
    assertTrue(user.isActive());
  }

  @Test
  void shouldCreateUserWithoutId() {
    // Given
    String firstName = "Jane";
    String lastName = "Smith";
    String username = "jane.smith";
    String email = "jane.smith@example.com";
    String password = "SecurePass1!";
    UserRole role = UserRole.INSTRUCTOR;

    // When
    User user = new User(firstName, lastName, username, email, password, role, null, null);

    // Then
    assertNull(user.getId());
    assertEquals(firstName, user.getFirstName().getValue());
    assertEquals(lastName, user.getLastName().getValue());
    assertEquals(username, user.getUsername());
    assertEquals(email.toLowerCase(), user.getEmail().getValue());
    assertEquals(password, user.getPassword().getValue());
    assertEquals(role, user.getRole());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
    assertTrue(user.isActive());
  }

  @Test
  void shouldCreateUserWithAllValueObjects() {
    // Given
    UserId id = new UserId(100L);
    UserName firstName = new UserName("Admin");
    UserName lastName = new UserName("User");
    String username = "admin.user";
    Email email = new Email("admin@example.com");
    Password password = new Password("AdminPass123!");
    UserRole role = UserRole.ADMIN;
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 10, 0);
    Boolean active = false;

    // When
    User user = new User(id, firstName, lastName, username, email, password, role, null, null, createdAt, updatedAt, active);

    // Then
    assertEquals(id, user.getId());
    assertEquals(firstName, user.getFirstName());
    assertEquals(lastName, user.getLastName());
    assertEquals(username, user.getUsername());
    assertEquals(email, user.getEmail());
    assertEquals(password, user.getPassword());
    assertEquals(role, user.getRole());
    assertEquals(createdAt, user.getCreatedAt());
    assertEquals(updatedAt, user.getUpdatedAt());
    assertFalse(user.isActive());
  }

  @Test
  void shouldGetAllFieldsCorrectly() {
    // Given
    Long id = 2L;
    String firstName = "Test";
    String lastName = "User";
    String username = "test.user";
    String email = "test@example.com";
    String password = "TestPass123!";
    UserRole role = UserRole.SUPER_ADMIN;

    // When
    User user = new User(id, firstName, lastName, username, email, password, role, null, null);

    // Then
    assertNotNull(user.getId());
    assertEquals(2L, user.getId().getValue());
    assertNotNull(user.getFirstName());
    assertEquals("Test", user.getFirstName().getValue());
    assertNotNull(user.getLastName());
    assertEquals("User", user.getLastName().getValue());
    assertEquals("test.user", user.getUsername());
    assertNotNull(user.getEmail());
    assertEquals("test@example.com", user.getEmail().getValue());
    assertNotNull(user.getPassword());
    assertEquals("TestPass123!", user.getPassword().getValue());
    assertNotNull(user.getRole());
    assertEquals(UserRole.SUPER_ADMIN, user.getRole());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
    assertNotNull(user.isActive());
    assertTrue(user.isActive());
  }

  @Test
  void shouldCreateUserWithDifferentRoles() {
    // Test STUDENT role
    User student = new User("Student", "Name", "student.name", "student@test.com", "Pass123!", UserRole.STUDENT, null, null);
    assertEquals(UserRole.STUDENT, student.getRole());

    // Test INSTRUCTOR role
    User instructor = new User("Instructor", "Name", "instructor.name", "instructor@test.com", "Pass123!", UserRole.INSTRUCTOR, null, null);
    assertEquals(UserRole.INSTRUCTOR, instructor.getRole());

    // Test ADMIN role
    User admin = new User("Admin", "Name", "admin.name", "admin@test.com", "Pass123!", UserRole.ADMIN, null, null);
    assertEquals(UserRole.ADMIN, admin.getRole());

    // Test SUPER_ADMIN role
    User superAdmin = new User("Super", "Admin", "super.admin", "superadmin@test.com", "Pass123!", UserRole.SUPER_ADMIN, null, null);
    assertEquals(UserRole.SUPER_ADMIN, superAdmin.getRole());
  }

  @Test
  void shouldHandleTimestampsCorrectly() {
    // Given
    LocalDateTime before = LocalDateTime.now();

    // When
    User user = new User("User", "Name", "user.name", "user@test.com", "Pass123!", UserRole.STUDENT, null, null);

    // Then
    LocalDateTime after = LocalDateTime.now();
    assertTrue(user.getCreatedAt().isAfter(before) || user.getCreatedAt().isEqual(before));
    assertTrue(user.getCreatedAt().isBefore(after) || user.getCreatedAt().isEqual(after));
    assertTrue(user.getUpdatedAt().isAfter(before) || user.getUpdatedAt().isEqual(before));
    assertTrue(user.getUpdatedAt().isBefore(after) || user.getUpdatedAt().isEqual(after));
  }

  @Test
  void shouldBeActiveByDefault() {
    // When
    User user1 = new User(1L, "User", "One", "user.one", "user1@test.com", "Pass123!", UserRole.STUDENT, null, null);
    User user2 = new User("User", "Two", "user.two", "user2@test.com", "Pass123!", UserRole.INSTRUCTOR, null, null);

    // Then
    assertTrue(user1.isActive());
    assertTrue(user2.isActive());
  }

  @Test
  void shouldToggleActiveStateWithActivateAndDeactivate() {
    // When
    User user = new User("User", "Toggle", "user.toggle", "toggle@test.com", "Pass123!", UserRole.STUDENT, null, null);

    // Then
    assertTrue(user.isActive());
    user.deactivate();
    assertFalse(user.isActive());
    user.activate();
    assertTrue(user.isActive());
  }
}
