package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A discussion of a course forum. Locked threads take no new replies from students. */
public record ForumThread(
  Long id,
  Long courseId,
  Long authorId,
  String authorName,
  String authorRole,
  String title,
  String body,
  boolean pinned,
  boolean locked,
  int replyCount,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  LocalDateTime lastActivityAt
) {
}
