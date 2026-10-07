package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.CourseFeedbackGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import com.gems.education.domain.entities.CourseFeedback.SurveyResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.gems.education.application.exceptions.CourseActivityException.NOT_ENROLLED;

/**
 * Course surveys (one per course), their responses (one per student) and course reviews
 * (one per student, rating 1–5). Students must be enrolled to answer or review.
 */
public class CourseFeedbackUseCase {
  public static final String SURVEY_NOT_FOUND = "SURVEY_NOT_FOUND";
  public static final String SURVEY_CLOSED = "SURVEY_CLOSED";

  private final CourseFeedbackGateway gateway;
  private final EnrollmentGateway enrollmentGateway;

  public CourseFeedbackUseCase(CourseFeedbackGateway gateway, EnrollmentGateway enrollmentGateway) {
    this.gateway = gateway;
    this.enrollmentGateway = enrollmentGateway;
  }

  public Mono<Survey> survey(Long courseId) {
    return gateway.findSurvey(courseId);
  }

  /** Creates or replaces the course survey. */
  public Mono<Survey> saveSurvey(Long courseId, String title, String description, String sections, boolean published) {
    return gateway.findSurvey(courseId).map(Survey::id).defaultIfEmpty(-1L)
      .flatMap(id -> gateway.saveSurvey(new Survey(id > 0 ? id : null, courseId,
        title == null || title.isBlank() ? "Encuesta del curso" : title.trim(), description,
        sections == null ? "[]" : sections, published, LocalDateTime.now())));
  }

  /** Records the student's answers; answering again replaces the previous answers. */
  public Mono<SurveyResponse> respond(Long courseId, Long studentId, String answers) {
    return enrolled(courseId, studentId)
      .then(Mono.defer(() -> gateway.findSurvey(courseId)))
      .switchIfEmpty(Mono.error(new CourseActivityException(SURVEY_NOT_FOUND, "This course has no survey")))
      .flatMap(survey -> {
        if (!survey.published()) {
          return Mono.error(new CourseActivityException(SURVEY_CLOSED, "The survey is not open"));
        }
        return gateway.findResponse(survey.id(), studentId).map(SurveyResponse::id).defaultIfEmpty(-1L)
          .flatMap(id -> gateway.saveResponse(new SurveyResponse(id > 0 ? id : null, survey.id(), courseId,
            studentId, answers == null ? "[]" : answers, LocalDateTime.now())));
      });
  }

  public Flux<SurveyResponse> responses(Long courseId) {
    return gateway.findResponses(courseId);
  }

  public Mono<SurveyResponse> responseOf(Long courseId, Long studentId) {
    return gateway.findSurvey(courseId).flatMap(s -> gateway.findResponse(s.id(), studentId));
  }

  /** Creates or replaces the student's review of the course. */
  public Mono<Review> review(Long courseId, Long studentId, int rating, String comment) {
    if (rating < 1 || rating > 5) {
      return Mono.error(new IllegalArgumentException("Rating must be between 1 and 5"));
    }
    String text = comment == null ? "" : comment.trim();
    return enrolled(courseId, studentId)
      .then(Mono.defer(() -> gateway.findReview(courseId, studentId)))
      .map(existing -> new Review(existing.id(), courseId, studentId, rating, text, existing.createdAt(), LocalDateTime.now()))
      .switchIfEmpty(Mono.fromSupplier(() ->
        new Review(null, courseId, studentId, rating, text, LocalDateTime.now(), LocalDateTime.now())))
      .flatMap(gateway::saveReview);
  }

  public Flux<Review> reviews(Long courseId) {
    return gateway.findReviews(courseId);
  }

  public Mono<Review> reviewOf(Long courseId, Long studentId) {
    return gateway.findReview(courseId, studentId);
  }

  private Mono<Void> enrolled(Long courseId, Long studentId) {
    return enrollmentGateway.findByStudentIdAndCourseId(studentId, courseId)
      .switchIfEmpty(Mono.error(new CourseActivityException(NOT_ENROLLED, "You are not enrolled in this course")))
      .then();
  }
}
