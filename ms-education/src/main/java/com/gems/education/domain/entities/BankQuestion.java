package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A reusable question of an institution's question bank. payload is the question JSON of quiz blocks. */
public record BankQuestion(
  Long id,
  String institutionId,
  String category,
  String type,
  String payload,
  Long createdBy,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {
}
