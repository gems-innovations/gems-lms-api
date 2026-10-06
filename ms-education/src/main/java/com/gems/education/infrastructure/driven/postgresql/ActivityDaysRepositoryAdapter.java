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
  public Flux<LocalDate> since(Long studentId, LocalDate from) {
    return db.sql("SELECT day FROM student_activity_days WHERE student_id = :student AND day >= :from ORDER BY day")
      .bind("student", studentId).bind("from", from)
      .map((row, meta) -> row.get("day", LocalDate.class)).all();
  }
}
