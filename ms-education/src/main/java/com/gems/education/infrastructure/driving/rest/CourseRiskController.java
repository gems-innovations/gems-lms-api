package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.CourseRiskUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Early warning: students of a course at risk, for the staff who can edit it. */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/at-risk")
public class CourseRiskController {
  private final CourseRiskUseCase risk;
  private final EducationAccess access;

  public CourseRiskController(CourseRiskUseCase risk, EducationAccess access) {
    this.risk = risk;
    this.access = access;
  }

  @GetMapping
  public Mono<ResponseEntity<CourseRiskUseCase.CourseRisk>> course(@PathVariable Long courseId) {
    return access.editableCourse(courseId).then(Mono.defer(() -> risk.course(courseId))).map(ResponseEntity::ok);
  }

  public record ReminderRequest(String message) {
  }

  /** Sends the student an in-app reminder; only students enrolled in the course. */
  @PostMapping("/{studentId}/remind")
  public Mono<ResponseEntity<Void>> remind(@PathVariable Long courseId, @PathVariable Long studentId,
                                           @RequestBody(required = false) ReminderRequest body) {
    return access.editableCourse(courseId)
      .flatMap(course -> risk.course(courseId)
        .filter(r -> r.atRisk().stream().anyMatch(x -> x.studentId().equals(studentId)))
        .switchIfEmpty(Mono.error(new IllegalArgumentException("The student is not at risk in this course")))
        .then(risk.remind(course.getInstitutionId(), courseId, course.getTitle(), studentId, body == null ? null : body.message())))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }
}
