package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.gems.education.application.CourseFeedbackUseCase;
import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import com.gems.education.domain.entities.CourseFeedback.SurveyResponse;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Course survey and reviews. Anyone who can see the course reads its published survey and its
 * reviews; enrolled students answer and review; staff of the course edit the survey and read
 * every response.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}")
public class CourseFeedbackController {
  private final CourseFeedbackUseCase feedback;
  private final EducationAccess access;

  public CourseFeedbackController(CourseFeedbackUseCase feedback, EducationAccess access) {
    this.feedback = feedback;
    this.access = access;
  }

  // ── Survey ─────────────────────────────────────────────────────────────────

  /** 404 when the course has no survey, or (for students) when it is not published. */
  @GetMapping("/survey")
  public Mono<ResponseEntity<SurveyBody>> survey(@PathVariable Long courseId) {
    return access.readableCourse(courseId)
      .then(CurrentUser.get())
      .flatMap(caller -> feedback.survey(courseId).filter(s -> s.published() || caller.isStaff()))
      .map(s -> ResponseEntity.ok(SurveyBody.from(s)))
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PutMapping("/survey")
  public Mono<ResponseEntity<SurveyBody>> saveSurvey(@PathVariable Long courseId, @RequestBody SurveyRequest request) {
    return access.editableCourse(courseId)
      .then(Mono.defer(() -> feedback.saveSurvey(courseId, request.title(), request.description(),
        request.sections() == null ? "[]" : request.sections().toString(), Boolean.TRUE.equals(request.isPublished()))))
      .map(s -> ResponseEntity.ok(SurveyBody.from(s)));
  }

  @PostMapping("/survey/responses")
  public Mono<ResponseEntity<ResponseBody>> respond(@PathVariable Long courseId, @RequestBody AnswersRequest request) {
    return access.readableCourse(courseId)
      .then(CurrentUser.get())
      .flatMap(caller -> feedback.respond(courseId, caller.userId(),
        request.answers() == null ? "[]" : request.answers().toString()))
      .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ResponseBody.from(r)));
  }

  @GetMapping("/survey/responses")
  public Mono<ResponseEntity<List<ResponseBody>>> responses(@PathVariable Long courseId) {
    return access.editableCourse(courseId)
      .then(Mono.defer(() -> feedback.responses(courseId).map(ResponseBody::from).collectList()))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/survey/responses/me")
  public Mono<ResponseEntity<ResponseBody>> myResponse(@PathVariable Long courseId) {
    return CurrentUser.get()
      .flatMap(caller -> feedback.responseOf(courseId, caller.userId()))
      .map(r -> ResponseEntity.ok(ResponseBody.from(r)))
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  // ── Reviews ────────────────────────────────────────────────────────────────

  @PutMapping("/review")
  public Mono<ResponseEntity<Review>> review(@PathVariable Long courseId, @Valid @RequestBody ReviewRequest request) {
    return access.readableCourse(courseId)
      .then(CurrentUser.get())
      .flatMap(caller -> caller.isStudent()
        ? feedback.review(courseId, caller.userId(), request.rating(), request.comment())
        : Mono.error(new ForbiddenException("Only students review courses")))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/reviews")
  public Mono<ResponseEntity<List<Review>>> reviews(@PathVariable Long courseId) {
    return access.readableCourse(courseId)
      .then(Mono.defer(() -> feedback.reviews(courseId).collectList()))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/reviews/me")
  public Mono<ResponseEntity<Review>> myReview(@PathVariable Long courseId) {
    return CurrentUser.get()
      .flatMap(caller -> feedback.reviewOf(courseId, caller.userId()))
      .map(ResponseEntity::ok)
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  // ── Bodies ─────────────────────────────────────────────────────────────────

  /** sections: [{id, title, description, questions: [{id, type: scale|text, label}]}] */
  public record SurveyRequest(String title, String description, JsonNode sections, Boolean isPublished) {
  }

  /** answers: [{questionId, value}] */
  public record AnswersRequest(JsonNode answers) {
  }

  public record ReviewRequest(
    @NotNull(message = "Rating is required") @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5") Integer rating,
    String comment) {
  }

  public record SurveyBody(Long id, Long courseId, String title, String description, @JsonRawValue String sections,
                           boolean isPublished, LocalDateTime updatedAt) {
    static SurveyBody from(Survey s) {
      return new SurveyBody(s.id(), s.courseId(), s.title(), s.description(), s.sections(), s.published(), s.updatedAt());
    }
  }

  public record ResponseBody(Long id, Long surveyId, Long courseId, Long studentId, @JsonRawValue String answers,
                             LocalDateTime submittedAt) {
    static ResponseBody from(SurveyResponse r) {
      return new ResponseBody(r.id(), r.surveyId(), r.courseId(), r.studentId(), r.answers(), r.submittedAt());
    }
  }
}
