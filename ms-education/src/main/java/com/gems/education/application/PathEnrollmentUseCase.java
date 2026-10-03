package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.PathEnrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enrollments in learning paths. Enrolling is idempotent: a student already in the path keeps
 * their enrollment. The path's courses are enrolled through the course enrollments API.
 * Progress comes from the student's course enrollments: a path is completed when every
 * required course is.
 */
public class PathEnrollmentUseCase {
  private static final String COMPLETED = "completed";

  private final PathEnrollmentGateway gateway;
  private final LearningPathGateway learningPathGateway;
  private final EnrollmentGateway enrollmentGateway;

  public PathEnrollmentUseCase(PathEnrollmentGateway gateway, LearningPathGateway learningPathGateway,
                               EnrollmentGateway enrollmentGateway) {
    this.gateway = gateway;
    this.learningPathGateway = learningPathGateway;
    this.enrollmentGateway = enrollmentGateway;
  }

  /** A path enrollment with the student's progress in it. */
  public record Progress(PathEnrollment enrollment, List<Long> completedCourseIds, Long currentCourseId,
                         int overallPercentage) {
  }

  public Flux<PathEnrollment> enroll(Long learningPathId, List<Long> studentIds) {
    return Flux.fromIterable(studentIds.stream().filter(Objects::nonNull).distinct().toList())
      .concatMap(studentId -> gateway.find(learningPathId, studentId)
        .switchIfEmpty(Mono.defer(() -> gateway.save(new PathEnrollment(null, learningPathId, studentId,
          PathEnrollment.ACTIVE, LocalDateTime.now(), null)))));
  }

  public Flux<PathEnrollment> ofStudent(Long studentId) {
    return gateway.findByStudent(studentId);
  }

  /** The student's path enrollments with progress; newly completed paths are marked completed. */
  public Flux<Progress> progressOf(Long studentId) {
    return enrollmentGateway.findByStudentId(studentId)
      .collectMap(Enrollment::getCourseId, e -> COMPLETED.equals(e.getStatus()))
      .flatMapMany(courseDone -> gateway.findByStudent(studentId)
        .concatMap(pe -> learningPathGateway.findById(pe.learningPathId())
          .flatMap(path -> withProgress(pe, path, courseDone))
          .defaultIfEmpty(new Progress(pe, List.of(), null, 0))));
  }

  public Flux<PathEnrollment> ofPath(Long learningPathId) {
    return gateway.findByPath(learningPathId);
  }

  private Mono<Progress> withProgress(PathEnrollment pe, LearningPath path, Map<Long, Boolean> courseDone) {
    List<Long> courseIds = path.getCourses() == null ? List.of()
      : path.getCourses().stream().map(Course::getId).toList();
    List<Long> completed = courseIds.stream().filter(id -> Boolean.TRUE.equals(courseDone.get(id))).toList();
    Set<Long> required = courseIds.stream().filter(id -> path.stepFor(id).required()).collect(Collectors.toSet());
    long requiredDone = required.stream().filter(completed::contains).count();
    int percentage = required.isEmpty() ? (courseIds.isEmpty() ? 0 : 100)
      : (int) Math.round(100.0 * requiredDone / required.size());
    Long current = courseIds.stream().filter(id -> !completed.contains(id)).findFirst().orElse(null);

    Mono<PathEnrollment> saved = Mono.just(pe);
    if (percentage == 100 && !COMPLETED.equals(pe.status())) {
      saved = gateway.save(new PathEnrollment(pe.id(), pe.learningPathId(), pe.studentId(), COMPLETED,
        pe.enrolledAt(), LocalDateTime.now()));
    } else if (percentage < 100 && COMPLETED.equals(pe.status())) {
      saved = gateway.save(new PathEnrollment(pe.id(), pe.learningPathId(), pe.studentId(), PathEnrollment.ACTIVE,
        pe.enrolledAt(), null));
    }
    return saved.map(e -> new Progress(e, completed, current, percentage));
  }
}
