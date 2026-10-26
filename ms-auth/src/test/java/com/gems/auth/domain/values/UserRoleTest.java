package com.gems.auth.domain.values;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserRole")
class UserRoleTest {

  @Test
  @DisplayName("fromString accepts every role, case-insensitively, including CREATOR")
  void fromStringAcceptsEveryRole() {
    assertThat(UserRole.fromString("creator")).isEqualTo(UserRole.CREATOR);
    assertThat(UserRole.fromString("CREATOR")).isEqualTo(UserRole.CREATOR);
    assertThat(UserRole.fromString("Super_Admin")).isEqualTo(UserRole.SUPER_ADMIN);
    assertThat(UserRole.fromString("student")).isEqualTo(UserRole.STUDENT);
  }

  @Test
  @DisplayName("fromString rejects unknown roles")
  void fromStringRejectsUnknownRoles() {
    assertThatThrownBy(() -> UserRole.fromString("creador")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UserRole.fromString("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> UserRole.fromString(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("CREATOR is appended last so existing ordinals do not move")
  void creatorIsAppendedLast() {
    assertThat(UserRole.values()).containsExactly(UserRole.STUDENT, UserRole.INSTRUCTOR, UserRole.ADMIN,
      UserRole.SUPER_ADMIN, UserRole.CREATOR);
    assertThat(UserRole.valueOf("CREATOR").name()).isEqualTo("CREATOR");
  }
}
