package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gems.education.application.GetCourseActivityUseCase;
import com.gems.education.application.GradeSubmissionUseCase;
import com.gems.education.application.NotificationUseCase;
import com.gems.education.application.SubmitAssignmentUseCase;
import com.gems.education.application.SubmitQuizAttemptUseCase;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.QuizAttempt;
import com.gems.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quiz attempts (graded here, never on the client) and assignment submissions.
 * Students act on their own enrollments; staff of the course's institution list and grade
 * submissions.
 */
@RestController
@RequestMapping("/api/v1")
public class CourseActivityController {
  private final SubmitQuizAttemptUseCase submitQuizAttemptUseCase;
  private final SubmitAssignmentUseCase submitAssignmentUseCase;
  private final GradeSubmissionUseCase gradeSubmissionUseCase;
  private final GetCourseActivityUseCase getCourseActivityUseCase;
  private final EducationAccess access;
  private final ObjectMapper mapper;
  private final NotificationUseCase notifications;

  public CourseActivityController(SubmitQuizAttemptUseCase submitQuizAttemptUseCase,
                                  SubmitAssignmentUseCase submitAssignmentUseCase,
                                  GradeSubmissionUseCase gradeSubmissionUseCase,
                                  GetCourseActivityUseCase getCourseActivityUseCase,
                                  EducationAccess access,
                                  ObjectMapper mapper,
                                  NotificationUseCase notifications) {
    this.submitQuizAttemptUseCase = submitQuizAttemptUseCase;
    this.submitAssignmentUseCase = submitAssignmentUseCase;
    this.gradeSubmissionUseCase = gradeSubmissionUseCase;
    this.getCourseActivityUseCase = getCourseActivityUseCase;
    this.access = access;
    this.mapper = mapper;
    this.notifications = notifications;
  }

  @PostMapping("/courses/{courseId}/blocks/{blockId}/attempts")
  public Mono<ResponseEntity<AttemptResponse>> submitAttempt(@PathVariable Long courseId, @PathVariable Long blockId,
                                                             @Valid @RequestBody AttemptRequest request) {
    return access.readableCourse(courseId)
      .then(CurrentUser.get())
      .flatMap(caller -> submitQuizAttemptUseCase.execute(caller.userId(), courseId, blockId, request.answers().toString()))
      .map(attempt -> ResponseEntity.status(HttpStatus.CREATED).body(AttemptResponse.from(attempt)));
  }

  @PutMapping("/courses/{courseId}/blocks/{blockId}/submission")
  public Mono<ResponseEntity<SubmissionResponse>> submitAssignment(@PathVariable Long courseId, @PathVariable Long blockId,
                                                                   @RequestBody SubmissionRequest request) {
    return access.readableCourse(courseId)
      .flatMap(course -> CurrentUser.get()
        .flatMap(caller -> submitAssignmentUseCase.execute(caller.userId(), courseId, blockId,
          request.textContent(), toJson(request.fileUrls())))
        .flatMap(submission -> notifications.submissionReceived(course.getInstitutionId(), courseId,
            course.getTitle(), submission.id())
          .onErrorResume(e -> Mono.empty())
          .thenReturn(submission)))
      .map(submission -> ResponseEntity.ok(SubmissionResponse.from(submission)));
  }

  /** The caller's quiz attempts and assignment submissions across all their courses. */
  @GetMapping("/activity/me")
  public Mono<ResponseEntity<ActivityResponse>> myActivity() {
    return CurrentUser.get().flatMap(caller -> Mono.zip(
        getCourseActivityUseCase.attemptsOfStudent(caller.userId()).map(AttemptResponse::from).collectList(),
        getCourseActivityUseCase.submissionsOfStudent(caller.userId()).map(SubmissionResponse::from).collectList()))
      .map(t -> ResponseEntity.ok(new ActivityResponse(t.getT1(), t.getT2())));
  }

  @GetMapping("/courses/{courseId}/submissions")
  public Mono<ResponseEntity<List<SubmissionResponse>>> courseSubmissions(@PathVariable Long courseId) {
    return access.editableCourse(courseId)
      .flatMap(course -> getCourseActivityUseCase.submissionsOfCourse(courseId).map(SubmissionResponse::from).collectList())
      .map(ResponseEntity::ok);
  }

  /** Every submission in the institution's courses, newest first (staff dashboards). */
  @GetMapping("/submissions/institution/{institutionId}")
  public Mono<ResponseEntity<List<SubmissionResponse>>> institutionSubmissions(@PathVariable String institutionId) {
    return access.staffOf(institutionId)
      .flatMap(caller -> getCourseActivityUseCase.submissionsOfInstitution(institutionId).map(SubmissionResponse::from).collectList())
      .map(ResponseEntity::ok);
  }

  @PutMapping("/submissions/{id}/grade")
  public Mono<ResponseEntity<SubmissionResponse>> grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
    return gradeSubmissionUseCase.findSubmission(id)
      .flatMap(submission -> access.editableCourse(submission.courseId()))
      .flatMap(course -> gradeSubmissionUseCase.execute(id, request.grade(), request.feedback())
        .flatMap(graded -> notifications.submissionGraded(course.getInstitutionId(), graded.studentId(),
            course.getId(), course.getTitle(), graded.id(), request.grade())
          .onErrorResume(e -> Mono.empty())
          .thenReturn(graded)))
      .map(submission -> ResponseEntity.ok(SubmissionResponse.from(submission)));
  }

  private String toJson(List<String> values) {
    try {
      return values == null ? null : mapper.writeValueAsString(values);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid file URLs");
    }
  }

  // ── Request / response bodies ──────────────────────────────────────────────

  /** answers: [{questionId, answer}] where answer is a string, a list of option ids or a boolean. */
  public record AttemptRequest(@NotNull(message = "Answers are required") JsonNode answers) {
  }

  public record SubmissionRequest(String textContent, List<String> fileUrls) {
  }

  public record GradeRequest(
    @NotNull(message = "Grade is required") @Min(value = 0, message = "Grade cannot be negative") Integer grade,
    String feedback) {
  }

  public record AttemptResponse(Long id, Long enrollmentId, Long studentId, Long courseId, Long blockId,
                                Long lessonId, int attemptNumber, @JsonRawValue String answers, int score,
                                boolean passed, @JsonRawValue String feedback, LocalDateTime completedAt) {
    static AttemptResponse from(QuizAttempt a) {
      return new AttemptResponse(a.id(), a.enrollmentId(), a.studentId(), a.courseId(), a.blockId(), a.lessonId(),
        a.attemptNumber(), a.answers(), a.score(), a.passed(), a.feedback() != null ? a.feedback() : "[]",
        a.completedAt());
    }
  }

  public record SubmissionResponse(Long id, Long enrollmentId, Long studentId, Long courseId, Long blockId,
                                   Long lessonId, String textContent, @JsonRawValue String fileUrls,
                                   LocalDateTime submittedAt, Integer grade, String feedback, String status) {
    static SubmissionResponse from(AssignmentSubmission s) {
      return new SubmissionResponse(s.id(), s.enrollmentId(), s.studentId(), s.courseId(), s.blockId(),
        s.lessonId(), s.textContent(), s.fileUrls() != null ? s.fileUrls() : "[]", s.submittedAt(), s.grade(),
        s.feedback(), s.status());
    }
  }

  public record ActivityResponse(List<AttemptResponse> quizAttempts, List<SubmissionResponse> submissions) {
  }
}
