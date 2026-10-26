package com.gems.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

  @Test
  void creatorAuthorsTapsButIsNotStaff() {
    AuthenticatedUser creator = new AuthenticatedUser(8L, AuthenticatedUser.CREATOR, null);
    assertTrue(creator.isCreator());
    assertTrue(creator.createsTaps());
    assertFalse(creator.isStaff());
    assertFalse(creator.isStudent());
    assertFalse(creator.isAdmin());
    assertFalse(creator.isSuperAdmin());
    assertFalse(creator.belongsTo("inst-1"));
  }

  @Test
  void staffAuthorTapsAndStudentsDoNot() {
    assertTrue(new AuthenticatedUser(1L, AuthenticatedUser.SUPER_ADMIN, null).createsTaps());
    assertTrue(new AuthenticatedUser(2L, AuthenticatedUser.ADMIN, "inst-1").createsTaps());
    assertTrue(new AuthenticatedUser(3L, AuthenticatedUser.INSTRUCTOR, "inst-1").createsTaps());
    assertFalse(new AuthenticatedUser(5L, AuthenticatedUser.STUDENT, "inst-1").createsTaps());
    assertFalse(new AuthenticatedUser(6L, "UNKNOWN", "inst-1").createsTaps());
    assertFalse(new AuthenticatedUser(6L, "UNKNOWN", "inst-1").isStaff());
  }
}
