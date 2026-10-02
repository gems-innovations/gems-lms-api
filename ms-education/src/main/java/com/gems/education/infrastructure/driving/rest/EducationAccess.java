package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.GetCourseByIdUseCase;
import com.gems.education.application.GetEnrollmentByIdUseCase;
import com.gems.education.application.GetLearningPathByIdUseCase;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Authorization checks shared by the education controllers.
 *
 * <ul>
 *   <li>Everyone works inside their institution; the super admin reaches all of them.</li>
 *   <li>Staff (admin, instructor) manage courses, paths and enrollments of their institution.</li>
 *   <li>Students read published courses and only touch their own enrollments.</li>
 * </ul>
 */
@Component
public class EducationAccess {
  private static final String PUBLISHED = "published";

  private final GetCourseByIdUseCase getCourseByIdUseCase;
  private final GetEnrollmentByIdUseCase getEnrollmentByIdUseCase;
  private final GetLearningPathByIdUseCase getLearningPathByIdUseCase;

  public EducationAccess(GetCourseByIdUseCase getCourseByIdUseCase,
                         GetEnrollmentByIdUseCase getEnrollmentByIdUseCase,
                         GetLearningPathByIdUseCase getLearningPathByIdUseCase) {
    this.getCourseByIdUseCase = getCourseByIdUseCase;
    this.getEnrollmentByIdUseCase = getEnrollmentByIdUseCase;
    this.getLearningPathByIdUseCase = getLearningPathByIdUseCase;
  }

  public Mono<AuthenticatedUser> staff() {
    return CurrentUser.require(AuthenticatedUser::isStaff, "Only administrators and instructors can do this");
  }

  public Mono<AuthenticatedUser> staffOf(String institutionId) {
    return CurrentUser.require(caller -> caller.isStaff() && caller.belongsTo(institutionId),
      "Only staff of the institution can do this");
  }

  /** The course, if the caller may read it (students: only published courses). */
  public Mono<CourseResponse> readableCourse(Long courseId) {
    return CurrentUser.get().flatMap(caller -> getCourseByIdUseCase.execute(courseId).flatMap(course -> {
      boolean allowed = caller.belongsTo(course.getInstitutionId())
        && (!caller.isStudent() || PUBLISHED.equals(course.getStatus()));
      return allowed ? Mono.just(course) : Mono.error(new ForbiddenException("You cannot see this course"));
    }));
  }

  /** The course, if the caller is staff of its institution. */
  public Mono<CourseResponse> editableCourse(Long courseId) {
    return CurrentUser.get().flatMap(caller -> getCourseByIdUseCase.execute(courseId).flatMap(course ->
      caller.isStaff() && caller.belongsTo(course.getInstitutionId())
        ? Mono.just(course)
        : Mono.error(new ForbiddenException("You cannot manage this course"))));
  }

  /** The enrollment, if it is the caller's own or the caller is staff of the course's institution. */
  public Mono<EnrollmentResponse> accessibleEnrollment(Long enrollmentId) {
    return CurrentUser.get().flatMap(caller -> getEnrollmentByIdUseCase.execute(enrollmentId).flatMap(enrollment -> {
      if (caller.isUser(enrollment.getStudentId())) return Mono.just(enrollment);
      return editableCourse(enrollment.getCourseId()).thenReturn(enrollment);
    }));
  }

  /** The enrollment, if the caller is staff of the course's institution. */
  public Mono<EnrollmentResponse> manageableEnrollment(Long enrollmentId) {
    return getEnrollmentByIdUseCase.execute(enrollmentId)
      .flatMap(enrollment -> editableCourse(enrollment.getCourseId()).thenReturn(enrollment));
  }

  public Mono<LearningPathResponse> readablePath(Long pathId) {
    return CurrentUser.get().flatMap(caller -> getLearningPathByIdUseCase.execute(pathId).flatMap(path ->
      caller.belongsTo(path.getInstitutionId())
        ? Mono.just(path)
        : Mono.error(new ForbiddenException("You cannot see this learning path"))));
  }

  public Mono<LearningPathResponse> editablePath(Long pathId) {
    return CurrentUser.get().flatMap(caller -> getLearningPathByIdUseCase.execute(pathId).flatMap(path ->
      caller.isStaff() && caller.belongsTo(path.getInstitutionId())
        ? Mono.just(path)
        : Mono.error(new ForbiddenException("You cannot manage this learning path"))));
  }
}
