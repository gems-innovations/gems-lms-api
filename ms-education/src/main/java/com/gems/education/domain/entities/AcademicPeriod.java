package com.gems.education.domain.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A term of an institution. Dates are inclusive. Once closed, the final grades of its courses are
 * frozen in the period records and no more activity is accepted in those courses.
 */
public record AcademicPeriod(
  Long id,
  String institutionId,
  String name,
  LocalDate startsOn,
  LocalDate endsOn,
  LocalDateTime createdAt,
  LocalDateTime closedAt,
  Long closedBy
) {
  public AcademicPeriod(Long id, String institutionId, String name, LocalDate startsOn, LocalDate endsOn,
                        LocalDateTime createdAt) {
    this(id, institutionId, name, startsOn, endsOn, createdAt, null, null);
  }

  public boolean closed() {
    return closedAt != null;
  }
}
