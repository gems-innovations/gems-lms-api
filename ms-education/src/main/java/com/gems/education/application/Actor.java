package com.gems.education.application;

/** Who is writing in a course: their id, display name, role and whether they are course staff. */
public record Actor(Long userId, String name, String role, boolean staff) {
}
