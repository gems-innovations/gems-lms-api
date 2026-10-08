package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.AcademicPeriodGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.EnrollmentRulesGateway;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.EnrollmentRules;
import com.gems.education.domain.entities.PeriodGradeRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Closing an academic period: the final grade of every student in every course of the period is
 * frozen as the period's records (the acta) and the courses stop accepting attempts, submissions,
 * grading and weight changes. An administrator can reopen it to correct something; reopening
 * discards the records, which are taken again on the next close.
 */
public class PeriodClosingUseCase {
  /** Final grade (0–100) from which a student passes the course. */
  public static final double PASSING_GRADE = 60.0;

  private final AcademicPeriodGateway periods;
  private final EnrollmentRulesGateway rules;
  private final CourseGateway courses;
  private final EnrollmentGateway enrollments;
  private final GradebookUseCase gradebook;
  private final Clock clock;

  public PeriodClosingUseCase(AcademicPeriodGateway periods, EnrollmentRulesGateway rules, CourseGateway courses,
                              EnrollmentGateway enrollments, GradebookUseCase gradebook, Clock clock) {
    this.periods = periods;
    this.rules = rules;
    this.courses = courses;
    this.enrollments = enrollments;
    this.gradebook = gradebook;
    this.clock = clock;
  }

  public record CloseSummary(AcademicPeriod period, int courses, int students, int passed, int failed) {
  }

  public Mono<CloseSummary> close(AcademicPeriod period, Long closedBy) {
    if (period.closed()) return Mono.error(new IllegalArgumentException("The period is already closed"));
    LocalDateTime now = LocalDateTime.now(clock);
    return coursesOf(period)
      .concatMap(courseId -> records(period.id(), courseId, now))
      .collectList()
      .flatMap(records -> periods.replaceRecords(period.id(), records)
        .then(periods.setClosed(period.id(), now, closedBy))
        .map(closed -> new CloseSummary(closed,
          (int) records.stream().map(PeriodGradeRecord::courseId).distinct().count(),
          records.size(),
          (int) records.stream().filter(PeriodGradeRecord::passed).count(),
          (int) records.stream().filter(r -> !r.passed()).count())));
  }

  public Mono<AcademicPeriod> reopen(AcademicPeriod period) {
    if (!period.closed()) return Mono.error(new IllegalArgumentException("The period is not closed"));
    return periods.deleteRecords(period.id()).then(periods.setClosed(period.id(), null, null));
  }

  public Flux<PeriodGradeRecord> records(AcademicPeriod period) {
    return periods.findRecords(period.id());
  }

  /** Fails with PERIOD_CLOSED when the course belongs to a closed period. */
  public Mono<Void> requireOpen(Long courseId) {
    return rules.find(courseId)
      .filter(r -> r.periodId() != null)
      .flatMap(r -> periods.findById(r.periodId()))
      .filter(AcademicPeriod::closed)
      .flatMap(p -> Mono.<Void>error(new CourseActivityException(CourseActivityException.PERIOD_CLOSED,
        "The academic period " + p.name() + " is closed")))
      .then();
  }

  private Flux<Long> coursesOf(AcademicPeriod period) {
    return rules.findByInstitution(period.institutionId())
      .filter(r -> Objects.equals(r.periodId(), period.id()))
      .map(EnrollmentRules::courseId)
      .distinct();
  }

  private Flux<PeriodGradeRecord> records(Long periodId, Long courseId, LocalDateTime now) {
    return Mono.zip(
        courses.findHeaderById(courseId).map(c -> c.getTitle() == null ? "Curso " + courseId : c.getTitle()).defaultIfEmpty("Curso " + courseId),
        gradebook.course(courseId),
        enrollments.findByCourseId(courseId).collectMap(Enrollment::getId, Function.identity()))
      .flatMapMany(t -> {
        Map<Long, Enrollment> byId = t.getT3();
        List<PeriodGradeRecord> list = t.getT2().rows().stream().map(row -> {
          Enrollment e = row.enrollmentId() == null ? null : byId.get(row.enrollmentId());
          Double finalGrade = row.finalGrade();
          return new PeriodGradeRecord(periodId, courseId, t.getT1(), row.studentId(), row.enrollmentId(),
            finalGrade, row.currentGrade(), e == null ? null : e.getProgress(),
            finalGrade != null && finalGrade >= PASSING_GRADE, now);
        }).collect(Collectors.toList());
        return Flux.fromIterable(list);
      });
  }
}
