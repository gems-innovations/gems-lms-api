package com.gems.education.infrastructure.driving.rest;

import com.gems.education.infrastructure.driven.auth.InstitutionMembers;
import com.gems.education.application.BulkEnrollStudentsUseCase;
import com.gems.education.application.DeleteEnrollmentUseCase;
import com.gems.education.application.EnrollStudentUseCase;
import com.gems.education.application.GetEnrollmentByIdUseCase;
import com.gems.education.application.GetEnrollmentsByInstitutionUseCase;
import com.gems.education.application.GetEnrollmentsByCourseUseCase;
import com.gems.education.application.GetStudentEnrollmentsUseCase;
import com.gems.education.application.UpdateEnrollmentProgressUseCase;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.mapper.EnrollmentMapper;
import com.gems.education.infrastructure.driving.rest.request.BulkEnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentProgressRequest;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import com.gems.shared.web.Paging;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {
  private final EnrollStudentUseCase enrollStudentUseCase;
  private final BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase;
  private final GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase;
  private final GetEnrollmentsByCourseUseCase getEnrollmentsByCourseUseCase;
  private final UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase;
  private final DeleteEnrollmentUseCase deleteEnrollmentUseCase;
  private final GetEnrollmentByIdUseCase getEnrollmentByIdUseCase;
  private final GetEnrollmentsByInstitutionUseCase getEnrollmentsByInstitutionUseCase;
  private final EducationAccess access;
  private final InstitutionMembers members;

  public EnrollmentController(EnrollStudentUseCase enrollStudentUseCase,
                               BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase,
                               GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase,
                               GetEnrollmentsByCourseUseCase getEnrollmentsByCourseUseCase,
                               UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase,
                               DeleteEnrollmentUseCase deleteEnrollmentUseCase,
                               GetEnrollmentByIdUseCase getEnrollmentByIdUseCase,
                               GetEnrollmentsByInstitutionUseCase getEnrollmentsByInstitutionUseCase,
                               EducationAccess access,
                               InstitutionMembers members) {
    this.enrollStudentUseCase = enrollStudentUseCase;
    this.bulkEnrollStudentsUseCase = bulkEnrollStudentsUseCase;
    this.getStudentEnrollmentsUseCase = getStudentEnrollmentsUseCase;
    this.getEnrollmentsByCourseUseCase = getEnrollmentsByCourseUseCase;
    this.updateEnrollmentProgressUseCase = updateEnrollmentProgressUseCase;
    this.deleteEnrollmentUseCase = deleteEnrollmentUseCase;
    this.getEnrollmentByIdUseCase = getEnrollmentByIdUseCase;
    this.getEnrollmentsByInstitutionUseCase = getEnrollmentsByInstitutionUseCase;
    this.access = access;
    this.members = members;
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<EnrollmentResponse>> getEnrollmentById(@PathVariable Long id) {
    return access.accessibleEnrollment(id)
      .map(ResponseEntity::ok);
  }

  /** Students enroll themselves in published courses; staff enroll anyone in their institution's courses. */
  @PostMapping
  public Mono<ResponseEntity<EnrollmentResponse>> enrollStudent(@Valid @RequestBody EnrollmentRequest request) {
    return CurrentUser.get()
      .flatMap(caller -> caller.isUser(request.getStudentId())
        ? access.readableCourse(request.getCourseId())
        : access.editableCourse(request.getCourseId())
          .flatMap(course -> members.requireMembers(List.of(request.getStudentId()), course.getInstitutionId())
            .thenReturn(course)))
      .flatMap(course -> enrollStudentUseCase.execute(EnrollmentMapper.toCommand(request)))
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/bulk")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> bulkEnrollStudents(@Valid @RequestBody BulkEnrollmentRequest request) {
    return access.editableCourse(request.getCourseId())
      .flatMap(course -> members.requireMembers(request.getStudentIds(), course.getInstitutionId()).thenReturn(course))
      .map(course -> ResponseEntity.status(HttpStatus.CREATED)
        .body(bulkEnrollStudentsUseCase.execute(EnrollmentMapper.toCommand(request))));
  }

  /** A student sees their own enrollments; staff see the ones in their institution's courses. */
  @GetMapping("/student/{studentId}")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> getStudentEnrollments(@PathVariable Long studentId) {
    return CurrentUser.get().flatMap(caller -> {
      Flux<EnrollmentResponse> all = getStudentEnrollmentsUseCase.execute(studentId);
      if (caller.isUser(studentId) || caller.isSuperAdmin()) return Mono.just(ResponseEntity.ok(all));
      if (!caller.isStaff()) return Mono.error(new ForbiddenException("You can only see your own enrollments"));
      return Mono.just(ResponseEntity.ok(all.filterWhen(e ->
        access.editableCourse(e.getCourseId()).hasElement().onErrorReturn(false))));
    });
  }

  /** Every enrollment in the institution's courses (staff dashboards). */
  /** Optional page/limit (total in X-Total-Count). */
  @GetMapping("/institution/{institutionId}")
  public Mono<ResponseEntity<java.util.List<EnrollmentResponse>>> getEnrollmentsByInstitution(
      @PathVariable String institutionId,
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer limit) {
    return access.staffOf(institutionId)
      .flatMap(caller -> Paging.of(getEnrollmentsByInstitutionUseCase.execute(institutionId), page, limit));
  }

  @GetMapping("/course/{courseId}")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> getEnrollmentsByCourse(@PathVariable Long courseId) {
    return access.editableCourse(courseId)
      .map(course -> ResponseEntity.ok(getEnrollmentsByCourseUseCase.execute(courseId)));
  }

  @PutMapping("/{id}/progress")
  public Mono<ResponseEntity<EnrollmentResponse>> updateEnrollmentProgress(
      @PathVariable Long id,
      @RequestParam(required = false) Integer progress,
      @Valid @RequestBody(required = false) EnrollmentProgressRequest body) {
    // Accepts the legacy ?progress= query param or a JSON body with progress + progressData.
    Integer value = body != null && body.getProgress() != null ? body.getProgress() : progress;
    String progressData = body != null ? body.getProgressData() : null;
    return access.accessibleEnrollment(id)
      .flatMap(enrollment -> updateEnrollmentProgressUseCase.execute(id, value, progressData))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteEnrollment(@PathVariable Long id) {
    return access.manageableEnrollment(id)
      .flatMap(enrollment -> deleteEnrollmentUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }
}
