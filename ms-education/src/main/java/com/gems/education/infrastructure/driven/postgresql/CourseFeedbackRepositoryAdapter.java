package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseFeedbackGateway;
import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import com.gems.education.domain.entities.CourseFeedback.SurveyResponse;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** Surveys, survey responses and reviews, with plain SQL (each is read and written whole). */
@Repository
public class CourseFeedbackRepositoryAdapter implements CourseFeedbackGateway {
  private final DatabaseClient db;

  public CourseFeedbackRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  // ── Surveys ────────────────────────────────────────────────────────────────

  @Override
  public Mono<Survey> findSurvey(Long courseId) {
    return db.sql("SELECT * FROM course_surveys WHERE course_id = :courseId")
      .bind("courseId", courseId)
      .map(CourseFeedbackRepositoryAdapter::toSurvey).one();
  }

  @Override
  public Mono<Survey> saveSurvey(Survey s) {
    String sql = s.id() == null
      ? "INSERT INTO course_surveys (course_id, title, description, sections, published, updated_at) "
        + "VALUES (:courseId, :title, :description, :sections, :published, :updatedAt) RETURNING *"
      : "UPDATE course_surveys SET title = :title, description = :description, sections = :sections, "
        + "published = :published, updated_at = :updatedAt WHERE id = :id AND course_id = :courseId RETURNING *";
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
      .bind("courseId", s.courseId())
      .bind("title", s.title())
      .bind("sections", s.sections())
      .bind("published", s.published())
      .bind("updatedAt", s.updatedAt());
    spec = s.description() == null ? spec.bindNull("description", String.class) : spec.bind("description", s.description());
    if (s.id() != null) spec = spec.bind("id", s.id());
    return spec.map(CourseFeedbackRepositoryAdapter::toSurvey).one();
  }

  // ── Survey responses ──────────────────────────────────────────────────────

  @Override
  public Mono<SurveyResponse> findResponse(Long surveyId, Long studentId) {
    return db.sql("SELECT * FROM survey_responses WHERE survey_id = :surveyId AND student_id = :studentId")
      .bind("surveyId", surveyId).bind("studentId", studentId)
      .map(CourseFeedbackRepositoryAdapter::toResponse).one();
  }

  @Override
  public Mono<SurveyResponse> saveResponse(SurveyResponse r) {
    return db.sql("INSERT INTO survey_responses (survey_id, course_id, student_id, answers, submitted_at) "
        + "VALUES (:surveyId, :courseId, :studentId, :answers, :submittedAt) "
        + "ON CONFLICT (survey_id, student_id) DO UPDATE SET answers = EXCLUDED.answers, "
        + "submitted_at = EXCLUDED.submitted_at RETURNING *")
      .bind("surveyId", r.surveyId()).bind("courseId", r.courseId()).bind("studentId", r.studentId())
      .bind("answers", r.answers()).bind("submittedAt", r.submittedAt())
      .map(CourseFeedbackRepositoryAdapter::toResponse).one();
  }

  @Override
  public Flux<SurveyResponse> findResponses(Long courseId) {
    return db.sql("SELECT * FROM survey_responses WHERE course_id = :courseId ORDER BY submitted_at")
      .bind("courseId", courseId)
      .map(CourseFeedbackRepositoryAdapter::toResponse).all();
  }

  // ── Reviews ───────────────────────────────────────────────────────────────

  @Override
  public Mono<Review> findReview(Long courseId, Long studentId) {
    return db.sql("SELECT * FROM course_reviews WHERE course_id = :courseId AND student_id = :studentId")
      .bind("courseId", courseId).bind("studentId", studentId)
      .map(CourseFeedbackRepositoryAdapter::toReview).one();
  }

  @Override
  public Mono<Review> saveReview(Review r) {
    return db.sql("INSERT INTO course_reviews (course_id, student_id, rating, comment, created_at, updated_at) "
        + "VALUES (:courseId, :studentId, :rating, :comment, :createdAt, :updatedAt) "
        + "ON CONFLICT (course_id, student_id) DO UPDATE SET rating = EXCLUDED.rating, "
        + "comment = EXCLUDED.comment, updated_at = EXCLUDED.updated_at RETURNING *")
      .bind("courseId", r.courseId()).bind("studentId", r.studentId()).bind("rating", r.rating())
      .bind("comment", r.comment()).bind("createdAt", r.createdAt()).bind("updatedAt", r.updatedAt())
      .map(CourseFeedbackRepositoryAdapter::toReview).one()
      .flatMap(saved -> refreshRating(saved.courseId()).thenReturn(saved));
  }

  @Override
  public Flux<Review> findReviews(Long courseId) {
    return db.sql("SELECT * FROM course_reviews WHERE course_id = :courseId ORDER BY updated_at DESC")
      .bind("courseId", courseId)
      .map(CourseFeedbackRepositoryAdapter::toReview).all();
  }

  private Mono<Void> refreshRating(Long courseId) {
    return db.sql("UPDATE courses SET "
        + "average_rating = (SELECT ROUND(AVG(rating)::numeric, 1) FROM course_reviews WHERE course_id = :courseId), "
        + "rating_count = (SELECT COUNT(*) FROM course_reviews WHERE course_id = :courseId) WHERE id = :courseId")
      .bind("courseId", courseId).then();
  }

  // ── Mapping ───────────────────────────────────────────────────────────────

  private static Survey toSurvey(Readable row) {
    return new Survey(row.get("id", Long.class), row.get("course_id", Long.class), row.get("title", String.class),
      row.get("description", String.class), row.get("sections", String.class),
      Boolean.TRUE.equals(row.get("published", Boolean.class)), row.get("updated_at", LocalDateTime.class));
  }

  private static SurveyResponse toResponse(Readable row) {
    return new SurveyResponse(row.get("id", Long.class), row.get("survey_id", Long.class),
      row.get("course_id", Long.class), row.get("student_id", Long.class), row.get("answers", String.class),
      row.get("submitted_at", LocalDateTime.class));
  }

  private static Review toReview(Readable row) {
    Integer rating = row.get("rating", Integer.class);
    return new Review(row.get("id", Long.class), row.get("course_id", Long.class), row.get("student_id", Long.class),
      rating == null ? 0 : rating, row.get("comment", String.class), row.get("created_at", LocalDateTime.class),
      row.get("updated_at", LocalDateTime.class));
  }
}
