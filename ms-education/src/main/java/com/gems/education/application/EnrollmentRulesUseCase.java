package com.gems.education.application;

import com.gems.education.application.exceptions.EnrollmentNotAllowedException;
import com.gems.education.application.gateway.AcademicPeriodGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.EnrollmentRulesGateway;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.EnrollmentPolicy;
import com.gems.education.domain.entities.EnrollmentPolicy.Decision;
import com.gems.education.domain.entities.EnrollmentRules;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Academic periods and the enrollment rules of courses (window, capacity, prerequisites). */
public class EnrollmentRulesUseCase {
  private static final String COMPLETED = "completed";
  private static final int MAX_NAME = 120;

  private final EnrollmentRulesGateway rules;
  private final AcademicPeriodGateway periods;
  private final CourseGateway courses;
  private final EnrollmentGateway enrollments;
  private final Clock clock;

  public EnrollmentRulesUseCase(EnrollmentRulesGateway rules, AcademicPeriodGateway periods, CourseGateway courses,
                                EnrollmentGateway enrollments, Clock clock) {
    this.rules = rules;
    this.periods = periods;
    this.courses = courses;
    this.enrollments = enrollments;
    this.clock = clock;
  }

  // ── Periods ────────────────────────────────────────────────────────────────

  public Flux<AcademicPeriod> periods(String institutionId) {
    return periods.findPeriodsOf(institutionId);
  }

  /** The period, if it belongs to the institution (any institution for a null scope). */
  public Mono<AcademicPeriod> period(Long id, String institutionId) {
    return periods.findById(id).filter(p -> institutionId == null || p.institutionId().equals(institutionId))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Academic period not found")));
  }

  public Mono<AcademicPeriod> savePeriod(Long id, String institutionId, String name, LocalDate startsOn, LocalDate endsOn) {
    if (name == null || name.isBlank() || name.trim().length() > MAX_NAME) {
      return Mono.error(new IllegalArgumentException("The name is required (max " + MAX_NAME + " characters)"));
    }
    if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
      return Mono.error(new IllegalArgumentException("The period must end on or after its start"));
    }
    Mono<AcademicPeriod> base = id == null
      ? Mono.just(new AcademicPeriod(null, institutionId, null, null, null, LocalDateTime.now(clock)))
      : period(id, institutionId);
    return base.flatMap(p -> periods.save(new AcademicPeriod(p.id(), p.institutionId(), name.trim(), startsOn, endsOn,
      p.createdAt())));
  }

  /** Courses of the period keep their rules; they lose the period (and its default closing date). */
  public Mono<Void> deletePeriod(Long id, String institutionId) {
    return period(id, institutionId).flatMap(p -> periods.delete(p.id()));
  }

  // ── Rules ──────────────────────────────────────────────────────────────────

  public Mono<EnrollmentRules> rules(Long courseId) {
    return rules.find(courseId).defaultIfEmpty(EnrollmentRules.none(courseId));
  }

  /**
   * Replaces the rules of a course. The period and the prerequisites must be of the course's
   * institution, and prerequisites cannot form a cycle.
   */
  public Mono<EnrollmentRules> saveRules(Long courseId, EnrollmentRules wanted) {
    List<Long> prereqs = wanted.prerequisiteIds() == null ? List.of()
      : wanted.prerequisiteIds().stream().filter(Objects::nonNull).distinct().toList();
    if (wanted.capacity() != null && wanted.capacity() < 1) {
      return Mono.error(new IllegalArgumentException("Capacity must be at least 1"));
    }
    if (wanted.opensAt() != null && wanted.closesAt() != null && wanted.closesAt().isBefore(wanted.opensAt())) {
      return Mono.error(new IllegalArgumentException("Enrollment must close after it opens"));
    }
    if (prereqs.contains(courseId)) {
      return Mono.error(new IllegalArgumentException("A course cannot be its own prerequisite"));
    }
    EnrollmentRules clean = new EnrollmentRules(courseId, wanted.periodId(), wanted.opensAt(), wanted.closesAt(),
      wanted.capacity(), wanted.selfEnrollment(), prereqs);
    return course(courseId).flatMap(course -> {
      Mono<Void> period = clean.periodId() == null ? Mono.empty()
        : period(clean.periodId(), course.getInstitutionId()).then();
      Mono<Void> sameInstitution = Flux.fromIterable(prereqs)
        .concatMap(id -> courses.findById(id)
          .filter(c -> Objects.equals(c.getInstitutionId(), course.getInstitutionId()))
          .switchIfEmpty(Mono.error(new IllegalArgumentException("Prerequisite " + id + " is not a course of the institution"))))
        .then();
      Mono<Void> noCycle = rules.findByInstitution(course.getInstitutionId()).collectList()
        .flatMap(all -> createsCycle(courseId, prereqs, all)
          ? Mono.error(new IllegalArgumentException("These prerequisites would create a cycle"))
          : Mono.<Void>empty());
      return period.then(sameInstitution).then(noCycle).then(Mono.defer(() -> rules.save(clean)));
    });
  }

  /** Whether the student may be enrolled, and why not. */
  public Mono<Eligibility> eligibility(Long studentId, Long courseId, boolean byStaff) {
    return rules(courseId).flatMap(r -> Mono.zip(
        r.periodId() == null ? Mono.just(Optional.<AcademicPeriod>empty())
          : periods.findById(r.periodId()).map(Optional::of).defaultIfEmpty(Optional.empty()),
        enrollments.findByCourseId(courseId).count(),
        completedCourses(studentId))
      .map(t -> {
        AcademicPeriod period = t.getT1().orElse(null);
        Decision d = EnrollmentPolicy.check(r, period, LocalDateTime.now(clock), t.getT2(), t.getT3(), byStaff);
        return new Eligibility(courseId, d.allowed(), d.reasons(), d.missingPrerequisites(), d.seatsLeft(),
          r.opensAt(), EnrollmentPolicy.closesAt(r, period), period);
      }));
  }

  /** Eligibility for several courses at once (the catalog). */
  public Flux<Eligibility> eligibility(Long studentId, Collection<Long> courseIds) {
    return Flux.fromIterable(new LinkedHashSet<>(courseIds)).concatMap(id -> eligibility(studentId, id, false));
  }

  /** Fails with the reasons when the rules do not let the student in. */
  public Mono<Void> requireAllowed(Long studentId, Long courseId, boolean byStaff) {
    return eligibility(studentId, courseId, byStaff)
      .flatMap(e -> e.allowed() ? Mono.<Void>empty() : Mono.error(new EnrollmentNotAllowedException(e.reasons())));
  }

  private Mono<Set<Long>> completedCourses(Long studentId) {
    return enrollments.findByStudentId(studentId)
      .filter(e -> COMPLETED.equals(e.getStatus()))
      .map(Enrollment::getCourseId)
      .collect(HashSet::new, Set::add);
  }

  private Mono<Course> course(Long courseId) {
    return courses.findById(courseId).switchIfEmpty(Mono.error(new IllegalArgumentException("Course not found")));
  }

  /** Would courseId → prereqs close a loop through the existing rules? */
  static boolean createsCycle(Long courseId, List<Long> prereqs, List<EnrollmentRules> all) {
    Map<Long, List<Long>> graph = new HashMap<>();
    all.forEach(r -> graph.put(r.courseId(), r.prerequisiteIds()));
    graph.put(courseId, prereqs);
    Deque<Long> pending = new ArrayDeque<>(prereqs);
    Set<Long> seen = new HashSet<>();
    while (!pending.isEmpty()) {
      Long next = pending.pop();
      if (next.equals(courseId)) return true;
      if (seen.add(next)) pending.addAll(graph.getOrDefault(next, List.of()));
    }
    return false;
  }

  /** closesAt is the effective closing time (the period's end when the course has no own bound). */
  public record Eligibility(Long courseId, boolean allowed, List<String> reasons, List<Long> missingPrerequisites,
                            Integer seatsLeft, LocalDateTime opensAt, LocalDateTime closesAt, AcademicPeriod period) {
  }
}
