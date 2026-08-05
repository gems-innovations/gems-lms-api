package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

  @Test
  void shouldCreateUserWithIdNameEmailPasswordAndRole() {
    // Given
    Long id = 1L;
    String name = "John Doe";
    String email = "john.doe@example.com";
    String password = "Password123!";
    UserRole role = UserRole.STUDENT;

    // When
    User user = new User(id, name, email, password, role);

    // Then
    assertNotNull(user.getId());
    assertEquals(id, user.getId().getValue());
    assertEquals(name, user.getName().getValue());
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
    String name = "Jane Smith";
    String email = "jane.smith@example.com";
    String password = "SecurePass1!";
    UserRole role = UserRole.TEACHER;

    // When
    User user = new User(name, email, password, role);

    // Then
    assertNull(user.getId());
    assertEquals(name, user.getName().getValue());
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
    UserName name = new UserName("Admin User");
    Email email = new Email("admin@example.com");
    Password password = new Password("AdminPass123!");
    UserRole role = UserRole.ADMIN;
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 10, 0);
    Boolean active = false;

    // When
    User user = new User(id, name, email, password, role, null, createdAt, updatedAt, active);

    // Then
    assertEquals(id, user.getId());
    assertEquals(name, user.getName());
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
    String name = "Test User";
    String email = "test@example.com";
    String password = "TestPass123!";
    UserRole role = UserRole.SUPER_ADMIN;

    // When
    User user = new User(id, name, email, password, role);

    // Then
    assertNotNull(user.getId());
    assertEquals(2L, user.getId().getValue());
    assertNotNull(user.getName());
    assertEquals("Test User", user.getName().getValue());
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
    User student = new User("Student Name", "student@test.com", "Pass123!", UserRole.STUDENT);
    assertEquals(UserRole.STUDENT, student.getRole());

    // Test TEACHER role
    User teacher = new User("Teacher Name", "teacher@test.com", "Pass123!", UserRole.TEACHER);
    assertEquals(UserRole.TEACHER, teacher.getRole());

    // Test ADMIN role
    User admin = new User("Admin Name", "admin@test.com", "Pass123!", UserRole.ADMIN);
    assertEquals(UserRole.ADMIN, admin.getRole());

    // Test SUPER_ADMIN role
    User superAdmin = new User("Super Admin", "superadmin@test.com", "Pass123!", UserRole.SUPER_ADMIN);
    assertEquals(UserRole.SUPER_ADMIN, superAdmin.getRole());
  }

  @Test
  void shouldHandleTimestampsCorrectly() {
    // Given
    LocalDateTime before = LocalDateTime.now();

    // When
    User user = new User("User", "user@test.com", "Pass123!", UserRole.STUDENT);

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
    User user1 = new User(1L, "User One", "user1@test.com", "Pass123!", UserRole.STUDENT);
    User user2 = new User("User Two", "user2@test.com", "Pass123!", UserRole.TEACHER);

    // Then
    assertTrue(user1.isActive());
    assertTrue(user2.isActive());
  }
}