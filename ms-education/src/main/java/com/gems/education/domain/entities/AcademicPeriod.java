package com.gems.education.domain.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A term of an institution. Dates are inclusive. */
public record AcademicPeriod(
  Long id,
  String institutionId,
  String name,
  LocalDate startsOn,
  LocalDate endsOn,
  LocalDateTime createdAt
) {
}
