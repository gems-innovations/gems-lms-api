package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.AchievementsUseCase;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/** One row per student and day with learning activity (streaks and weekly goal). */
@Repository
public class ActivityDaysRepositoryAdapter implements AchievementsUseCase.ActivityDays {
  private final DatabaseClient db;

  public ActivityDaysRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> record(Long studentId, LocalDate day) {
    return db.sql("INSERT INTO student_activity_days(student_id, day, actions) VALUES (:student, :day, 1) "
        + "ON CONFLICT (student_id, day) DO UPDATE SET actions = student_activity_days.actions + 1")
      .bind("student", studentId).bind("day", day).then();
  }

  @Override
  public Mono<java.util.Map<Long, LocalDate>> lastActive(java.util.Collection<Long> studentIds) {
    if (studentIds.isEmpty()) return Mono.just(java.util.Map.of());
    return db.sql("SELECT student_id, MAX(day) AS last_day FROM student_activity_days WHERE student_id = ANY(:ids) GROUP BY student_id")
      .bind("ids", studentIds.toArray(Long[]::new))
      .map((row, meta) -> java.util.Map.entry(row.get("student_id", Long.class), row.get("last_day", LocalDate.class)))
      .all().collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue);
  }

  @Override
  public Flux<LocalDate> since(Long studentId, LocalDate from) {
    return db.sql("SELECT day FROM student_activity_days WHERE student_id = :student AND day >= :from ORDER BY day")
      .bind("student", studentId).bind("from", from)
      .map((row, meta) -> row.get("day", LocalDate.class)).all();
  }
}
