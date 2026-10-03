package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.LearningDataGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Deletes a student's learning data in one transaction. */
@Repository
public class LearningDataRepositoryAdapter implements LearningDataGateway {
  /** Courses in scope: the institution's, or all of them for a null institution. */
  private static final String COURSES = "(SELECT id FROM courses WHERE :institutionId IS NULL OR institution_id = :institutionId)";
  private static final String PATHS = "(SELECT id FROM learning_paths WHERE :institutionId IS NULL OR institution_id = :institutionId)";

  private static final List<String> DELETES = List.of(
    "DELETE FROM quiz_attempts WHERE student_id = :studentId AND course_id IN " + COURSES,
    "DELETE FROM assignment_submissions WHERE student_id = :studentId AND course_id IN " + COURSES,
    "DELETE FROM survey_responses WHERE student_id = :studentId AND course_id IN " + COURSES,
    "DELETE FROM course_reviews WHERE student_id = :studentId AND course_id IN " + COURSES,
    "DELETE FROM enrollments WHERE student_id = :studentId AND course_id IN " + COURSES,
    "DELETE FROM path_enrollments WHERE student_id = :studentId AND learning_path_id IN " + PATHS,
    "UPDATE student_groups SET student_ids = array_remove(student_ids, :studentId) "
      + "WHERE :institutionId IS NULL OR institution_id = :institutionId",
    "DELETE FROM notification_reads WHERE user_id = :studentId",
    "DELETE FROM notifications WHERE recipient_user_id = :studentId",
    // Counters and ratings of the courses in scope.
    "UPDATE courses SET "
      + "enrolled_count = (SELECT COUNT(*) FROM enrollments e WHERE e.course_id = courses.id), "
      + "completion_rate = COALESCE((SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE e.status = 'completed') / NULLIF(COUNT(*), 0)) "
      + "FROM enrollments e WHERE e.course_id = courses.id), 0), "
      + "average_rating = (SELECT ROUND(AVG(rating)::numeric, 1) FROM course_reviews r WHERE r.course_id = courses.id), "
      + "rating_count = (SELECT COUNT(*) FROM course_reviews r WHERE r.course_id = courses.id) "
      + "WHERE :institutionId IS NULL OR institution_id = :institutionId"
  );

  private final DatabaseClient db;
  private final TransactionalOperator tx;

  public LearningDataRepositoryAdapter(DatabaseClient db, TransactionalOperator tx) {
    this.db = db;
    this.tx = tx;
  }

  @Override
  public Mono<Void> purge(Long studentId, String institutionId) {
    return Flux.fromIterable(DELETES)
      .concatMap(sql -> {
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql);
        if (sql.contains(":studentId")) spec = spec.bind("studentId", studentId);
        if (sql.contains(":institutionId")) {
          spec = institutionId == null ? spec.bindNull("institutionId", String.class) : spec.bind("institutionId", institutionId);
        }
        return spec.then();
      })
      .then()
      .as(tx::transactional);
  }
}
