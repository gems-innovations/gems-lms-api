package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/**
 * An in-app notification. It goes either to one user ({@code recipientUserId}) or to the staff of an
 * institution ({@code recipientUserId} null). {@code read} is the state for the user reading it.
 */
public record Notification(
  Long id,
  String institutionId,
  Long recipientUserId,
  String type,
  String title,
  String message,
  Long courseId,
  Long referenceId,
  LocalDateTime createdAt,
  boolean read
) {
  public static final String SUBMISSION = "submission";
  public static final String GRADED = "graded";
}
