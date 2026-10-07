package com.gems.education.domain.entities;

import java.time.LocalDateTime;

public record Certificate(Long id, String code, Long studentId, String studentName,
                          String institutionId, String resourceType, Long resourceId,
                          String resourceTitle, String instructorName,
                          LocalDateTime completedAt, LocalDateTime issuedAt,
                          LocalDateTime revokedAt) {
  public boolean valid() { return revokedAt == null; }
}
