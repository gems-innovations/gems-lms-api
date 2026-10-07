package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A notice the course staff publish to the course's students. */
public record Announcement(
  Long id,
  Long courseId,
  Long authorId,
  String authorName,
  String title,
  String body,
  boolean pinned,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {
}
