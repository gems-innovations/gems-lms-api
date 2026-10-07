package com.gems.education.infrastructure.driving.rest;

import com.gems.education.domain.entities.PeriodGradeRecord;
import com.gems.education.application.PeriodClosingUseCase;
import com.gems.education.application.EnrollmentRulesUseCase;
import com.gems.education.application.EnrollmentRulesUseCase.Eligibility;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.EnrollmentRules;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Academic periods (managed by the institution's administrators, read by its staff) and the
 * enrollment rules of courses (managed by whoever can edit the course).
 */
@RestController
@RequestMapping("/api/v1")
public class EnrollmentRulesController {
  private static final int MAX_COURSES = 100;

  private final EnrollmentRulesUseCase useCase;
  private final EducationAccess access;
  private final PeriodClosingUseCase closing;

  public EnrollmentRulesController(EnrollmentRulesUseCase useCase, EducationAccess access, PeriodClosingUseCase closing) {
    this.useCase = useCase;
    this.access = access;
    this.closing = closing;
  }

  // ── Periods ────────────────────────────────────────────────────────────────

  /** Any member of the institution can list its periods (students see them in the catalog). */
  @GetMapping("/academic-periods")
  public Mono<ResponseEntity<List<AcademicPeriod>>> periods(@RequestParam(required = false) String institutionId) {
    return CurrentUser.get().flatMap(caller -> {
      String inst = caller.isSuperAdmin() ? institutionId : caller.institutionId();
      if (inst == null || inst.isBlank()) return Mono.error(new IllegalArgumentException("institutionId is required"));
      return useCase.periods(inst).collectList();
    }).map(ResponseEntity::ok);
  }

  @PostMapping("/academic-periods")
  public Mono<ResponseEntity<AcademicPeriod>> createPeriod(@RequestParam(required = false) String institutionId,
                                                           @RequestBody PeriodRequest body) {
    return admin().flatMap(scope -> {
        String inst = scope.orElse(institutionId);
        if (inst == null || inst.isBlank()) return Mono.error(new IllegalArgumentException("institutionId is required"));
        return useCase.savePeriod(null, inst, body.name(), body.startsOn(), body.endsOn());
      })
      .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p));
  }

  @PutMapping("/academic-periods/{id}")
  public Mono<ResponseEntity<AcademicPeriod>> updatePeriod(@PathVariable Long id, @RequestBody PeriodRequest body) {
    return admin().flatMap(scope -> useCase.savePeriod(id, scope.orElse(null), body.name(), body.startsOn(), body.endsOn()))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/academic-periods/{id}")
  public Mono<ResponseEntity<Void>> deletePeriod(@PathVariable Long id) {
    return admin().flatMap(scope -> useCase.deletePeriod(id, scope.orElse(null)))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  // ── Closing (acta) ─────────────────────────────────────────────────────────

  /** Freezes the final grades of the period's courses and stops their activity. */
  @PostMapping("/academic-periods/{id}/close")
  public Mono<ResponseEntity<PeriodClosingUseCase.CloseSummary>> closePeriod(@PathVariable Long id) {
    return CurrentUser.require(c -> c.isSuperAdmin() || c.isAdmin(), "Only administrators close academic periods")
      .flatMap(caller -> useCase.period(id, caller.isSuperAdmin() ? null : caller.institutionId())
        .flatMap(p -> closing.close(p, caller.userId())))
      .map(ResponseEntity::ok);
  }

  @PostMapping("/academic-periods/{id}/reopen")
  public Mono<ResponseEntity<AcademicPeriod>> reopenPeriod(@PathVariable Long id) {
    return admin().flatMap(scope -> useCase.period(id, scope.orElse(null))).flatMap(closing::reopen)
      .map(ResponseEntity::ok);
  }

  /** The acta: one line per student and course. Administrators of the institution only. */
  @GetMapping("/academic-periods/{id}/records")
  public Mono<ResponseEntity<List<PeriodGradeRecord>>> periodRecords(@PathVariable Long id) {
    return admin().flatMap(scope -> useCase.period(id, scope.orElse(null)))
      .flatMap(p -> closing.records(p).collectList())
      .map(ResponseEntity::ok);
  }

  // ── Course rules ───────────────────────────────────────────────────────────

  @GetMapping("/courses/{courseId}/enrollment-rules")
  public Mono<ResponseEntity<EnrollmentRules>> rules(@PathVariable Long courseId) {
    return access.readableCourse(courseId).then(Mono.defer(() -> useCase.rules(courseId))).map(ResponseEntity::ok);
  }

  @PutMapping("/courses/{courseId}/enrollment-rules")
  public Mono<ResponseEntity<EnrollmentRules>> saveRules(@PathVariable Long courseId, @RequestBody RulesRequest body) {
    return access.editableCourse(courseId)
      .then(Mono.defer(() -> useCase.saveRules(courseId, new EnrollmentRules(courseId, body.periodId(), body.opensAt(),
        body.closesAt(), body.capacity(), body.selfEnrollment() == null || body.selfEnrollment(),
        body.prerequisiteIds() == null ? List.of() : body.prerequisiteIds()))))
      .map(ResponseEntity::ok);
  }

  /** Whether the caller may enroll themselves in the course, and why not. */
  @GetMapping("/courses/{courseId}/eligibility")
  public Mono<ResponseEntity<Eligibility>> eligibility(@PathVariable Long courseId) {
    return access.readableCourse(courseId).then(CurrentUser.get())
      .flatMap(caller -> useCase.eligibility(caller.userId(), courseId, false))
      .map(ResponseEntity::ok);
  }

  /** The same for several courses (the catalog); courses the caller cannot see are left out. */
  @GetMapping("/courses/eligibility")
  public Mono<ResponseEntity<List<Eligibility>>> eligibilities(@RequestParam List<Long> courseIds) {
    if (courseIds.size() > MAX_COURSES) {
      return Mono.error(new IllegalArgumentException("At most " + MAX_COURSES + " courses"));
    }
    return CurrentUser.get().flatMap(caller -> Flux.fromIterable(courseIds)
        .concatMap(id -> access.readableCourse(id).then(Mono.defer(() -> useCase.eligibility(caller.userId(), id, false)))
          .onErrorResume(ForbiddenException.class, e -> Mono.empty()))
        .collectList())
      .map(ResponseEntity::ok);
  }

  /** Administrators of an institution (scope = their institution) or the super admin (empty scope). */
  private Mono<Optional<String>> admin() {
    return CurrentUser.require(c -> c.isSuperAdmin() || c.isAdmin(), "Only administrators manage academic periods")
      .map(c -> Optional.ofNullable(c.isSuperAdmin() ? null : c.institutionId()));
  }

  public record PeriodRequest(String name, LocalDate startsOn, LocalDate endsOn) {
  }

  /** Null bounds and capacity mean no limit; selfEnrollment defaults to true. */
  public record RulesRequest(Long periodId, LocalDateTime opensAt, LocalDateTime closesAt, Integer capacity,
                             Boolean selfEnrollment, List<Long> prerequisiteIds) {
  }
}
