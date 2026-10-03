package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Resolves the course owning a lesson before authorizing legacy quiz operations. */
@Component
public class LessonAccess {
  private final CourseGateway courses;
  private final EnrollmentGateway enrollments;
  private final EducationAccess access;

  public LessonAccess(@org.springframework.beans.factory.annotation.Qualifier("courseGateway") CourseGateway courses,
                     @org.springframework.beans.factory.annotation.Qualifier("enrollmentGateway") EnrollmentGateway enrollments,
                     EducationAccess access) {
    this.courses = courses;
    this.enrollments = enrollments;
    this.access = access;
  }

  private Mono<Long> courseId(Long lessonId) {
    return courses.findCourseIdByLessonId(lessonId)
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found")));
  }

  public Mono<CourseResponse> readable(Long lessonId) {
    return courseId(lessonId).flatMap(access::readableCourse);
  }

  public Mono<CourseResponse> editable(Long lessonId) {
    return access.staff().then(courseId(lessonId)).flatMap(access::editableCourse);
  }

  public Mono<Void> enrolled(Long lessonId) {
    return readable(lessonId).flatMap(course -> CurrentUser.get().flatMap(caller ->
      enrollments.existsByStudentIdAndCourseId(caller.userId(), course.getId())
        .flatMap(enrolled -> enrolled ? Mono.<Void>empty()
          : Mono.error(new ForbiddenException("You must be enrolled to submit this quiz")))));
  }
}
