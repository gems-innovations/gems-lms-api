package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.PathEnrollmentUseCase;
import com.gems.education.domain.entities.PathEnrollment;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Enrollments in learning paths. A user enrolls themselves in a path they can see (students:
 * published paths); staff of the path's institution enroll others and list who is enrolled.
 */
@RestController
@RequestMapping("/api/v1/learning-paths")
public class PathEnrollmentController {
  private final PathEnrollmentUseCase pathEnrollmentUseCase;
  private final EducationAccess access;

  public PathEnrollmentController(PathEnrollmentUseCase pathEnrollmentUseCase, EducationAccess access) {
    this.pathEnrollmentUseCase = pathEnrollmentUseCase;
    this.access = access;
  }

  /** Without {@code studentIds}, enrolls the caller. */
  @PostMapping("/{id}/enrollments")
  public Mono<ResponseEntity<List<PathEnrollment>>> enroll(@PathVariable Long id,
                                                         @RequestBody(required = false) PathEnrollmentRequest request) {
    List<Long> studentIds = request == null || request.studentIds() == null ? List.of() : request.studentIds();
    return CurrentUser.get().flatMap(caller -> {
      boolean self = studentIds.isEmpty() || (studentIds.size() == 1 && caller.isUser(studentIds.get(0)));
      Mono<?> allowed = self ? access.readablePath(id) : access.editablePath(id);
      List<Long> targets = studentIds.isEmpty() ? List.of(caller.userId()) : studentIds;
      return allowed.then(Mono.defer(() -> pathEnrollmentUseCase.enroll(id, targets).collectList()));
    }).map(list -> ResponseEntity.status(HttpStatus.CREATED).body(list));
  }

  /** The caller's path enrollments with their progress (completed courses, current course, %). */
  @GetMapping("/enrollments/me")
  public Mono<ResponseEntity<List<PathProgressBody>>> mine() {
    return CurrentUser.get()
      .flatMap(caller -> pathEnrollmentUseCase.progressOf(caller.userId()).map(PathProgressBody::from).collectList())
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}/enrollments")
  public Mono<ResponseEntity<List<PathEnrollment>>> ofPath(@PathVariable Long id) {
    return access.editablePath(id)
      .then(Mono.defer(() -> pathEnrollmentUseCase.ofPath(id).collectList()))
      .map(ResponseEntity::ok);
  }

  public record PathEnrollmentRequest(List<Long> studentIds) {
  }

  public record PathProgressBody(Long id, Long learningPathId, Long studentId, String status,
                                 java.time.LocalDateTime enrolledAt, java.time.LocalDateTime completedAt,
                                 List<Long> completedCourseIds, Long currentCourseId, int overallPercentage) {
    static PathProgressBody from(PathEnrollmentUseCase.Progress p) {
      PathEnrollment e = p.enrollment();
      return new PathProgressBody(e.id(), e.learningPathId(), e.studentId(), e.status(), e.enrolledAt(),
        e.completedAt(), p.completedCourseIds(), p.currentCourseId(), p.overallPercentage());
    }
  }
}
