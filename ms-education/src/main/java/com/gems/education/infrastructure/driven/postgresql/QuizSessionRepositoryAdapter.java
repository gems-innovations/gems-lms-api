package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.QuizSessionGateway;
import com.gems.education.domain.entities.QuizSession;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public class QuizSessionRepositoryAdapter implements QuizSessionGateway {
  private static final String COLUMNS = "id, enrollment_id, student_id, course_id, block_id, questions, "
    + "student_questions, passing_score, started_at, expires_at, submitted_at";

  private final DatabaseClient db;

  public QuizSessionRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<QuizSession> save(QuizSession s) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO quiz_sessions(enrollment_id, student_id, course_id, "
        + "block_id, questions, student_questions, passing_score, started_at, expires_at) VALUES (:enrollment, :student, "
        + ":course, :block, :questions, :studentQuestions, :passing, :started, :expires) RETURNING " + COLUMNS)
      .bind("enrollment", s.enrollmentId()).bind("student", s.studentId()).bind("course", s.courseId())
      .bind("block", s.blockId()).bind("questions", s.questions()).bind("studentQuestions", s.studentQuestions())
      .bind("passing", s.passingScore()).bind("started", s.startedAt());
    spec = s.expiresAt() == null ? spec.bindNull("expires", LocalDateTime.class) : spec.bind("expires", s.expiresAt());
    return spec.map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<QuizSession> findById(Long id) {
    return db.sql("SELECT " + COLUMNS + " FROM quiz_sessions WHERE id = :id").bind("id", id)
      .map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<QuizSession> findOpen(Long enrollmentId, Long blockId) {
    return db.sql("SELECT " + COLUMNS + " FROM quiz_sessions WHERE enrollment_id = :enrollment AND block_id = :block "
        + "AND submitted_at IS NULL")
      .bind("enrollment", enrollmentId).bind("block", blockId)
      .map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<QuizSession> close(Long id) {
    return db.sql("UPDATE quiz_sessions SET submitted_at = :now WHERE id = :id AND submitted_at IS NULL RETURNING " + COLUMNS)
      .bind("id", id).bind("now", LocalDateTime.now())
      .map((row, meta) -> map(row)).one();
  }

  private static QuizSession map(Row row) {
    return new QuizSession(row.get("id", Long.class), row.get("enrollment_id", Long.class),
      row.get("student_id", Long.class), row.get("course_id", Long.class), row.get("block_id", Long.class),
      row.get("questions", String.class), row.get("student_questions", String.class),
      row.get("passing_score", Integer.class), row.get("started_at", LocalDateTime.class),
      row.get("expires_at", LocalDateTime.class), row.get("submitted_at", LocalDateTime.class));
  }
}
