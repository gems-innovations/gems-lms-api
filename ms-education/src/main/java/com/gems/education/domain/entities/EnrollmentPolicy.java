package com.gems.education.domain.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Decides whether a student may be enrolled in a course. Students enrolling themselves must meet
 * every rule; staff enrolling a student skip the window, self-enrollment and prerequisites, but
 * never exceed the capacity.
 */
public final class EnrollmentPolicy {
  public static final String SELF_ENROLLMENT_DISABLED = "SELF_ENROLLMENT_DISABLED";
  public static final String NOT_OPEN_YET = "NOT_OPEN_YET";
  public static final String CLOSED = "CLOSED";
  public static final String FULL = "FULL";
  public static final String MISSING_PREREQUISITES = "MISSING_PREREQUISITES";

  private EnrollmentPolicy() {
  }

  /** When enrollment closes: the rule's own bound, else the end of the period (end of that day). */
  public static LocalDateTime closesAt(EnrollmentRules rules, AcademicPeriod period) {
    if (rules.closesAt() != null) return rules.closesAt();
    return period != null ? period.endsOn().plusDays(1).atStartOfDay().minusNanos(1) : null;
  }

  public static Decision check(EnrollmentRules rules, AcademicPeriod period, LocalDateTime now, long enrolled,
                               Set<Long> completedCourseIds, boolean byStaff) {
    List<String> reasons = new ArrayList<>();
    List<Long> missing = rules.prerequisiteIds().stream().filter(id -> !completedCourseIds.contains(id)).toList();
    if (!byStaff) {
      if (!rules.selfEnrollment()) reasons.add(SELF_ENROLLMENT_DISABLED);
      if (rules.opensAt() != null && now.isBefore(rules.opensAt())) reasons.add(NOT_OPEN_YET);
      LocalDateTime closes = closesAt(rules, period);
      if (closes != null && now.isAfter(closes)) reasons.add(CLOSED);
      if (!missing.isEmpty()) reasons.add(MISSING_PREREQUISITES);
    }
    Integer seatsLeft = rules.capacity() == null ? null : (int) Math.max(0, rules.capacity() - enrolled);
    if (seatsLeft != null && seatsLeft == 0) reasons.add(FULL);
    return new Decision(reasons.isEmpty(), reasons, missing, seatsLeft);
  }

  /** seatsLeft is null when the course has no capacity. */
  public record Decision(boolean allowed, List<String> reasons, List<Long> missingPrerequisites, Integer seatsLeft) {
  }
}
