package com.gems.shared.security;

/**
 * Caller identity taken from a validated JWT. It is the principal of every authenticated
 * request in the services that use {@link JwtReactiveAuthenticationManager}.
 */
public record AuthenticatedUser(Long userId, String role, String institutionId, boolean mustChangePassword) {

  public AuthenticatedUser(Long userId, String role, String institutionId) {
    this(userId, role, institutionId, false);
  }

  public static final String SUPER_ADMIN = "SUPER_ADMIN";
  public static final String ADMIN = "ADMIN";
  public static final String INSTRUCTOR = "INSTRUCTOR";
  public static final String STUDENT = "STUDENT";
  /** Independent Taps creator: makes Taps but is not staff of any institution. */
  public static final String CREATOR = "CREATOR";

  public boolean isSuperAdmin() {
    return SUPER_ADMIN.equals(role);
  }

  public boolean isAdmin() {
    return ADMIN.equals(role);
  }

  public boolean isStudent() {
    return STUDENT.equals(role);
  }

  public boolean isCreator() {
    return CREATOR.equals(role);
  }

  /**
   * Admins and instructors manage content and people of their institution. A CREATOR is not
   * staff: no courses, question bank, groups, files or users.
   */
  public boolean isStaff() {
    return isSuperAdmin() || isAdmin() || INSTRUCTOR.equals(role);
  }

  /** Staff of an institution and independent creators may author Taps. */
  public boolean createsTaps() {
    return isStaff() || isCreator();
  }

  /** The super admin reaches every institution; everyone else only their own. */
  public boolean belongsTo(String institution) {
    return isSuperAdmin() || (institutionId != null && institutionId.equals(institution));
  }

  public boolean isUser(Long id) {
    return userId != null && userId.equals(id);
  }
}
