package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A reply in a forum thread. */
public record ForumPost(
  Long id,
  Long threadId,
  Long authorId,
  String authorName,
  String authorRole,
  String body,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {
}
