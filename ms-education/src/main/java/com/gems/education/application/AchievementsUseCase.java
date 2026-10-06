package com.gems.education.application;

import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.Achievements;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;

/** The student's points, level, streak, weekly goal and badges (see {@link Achievements}). */
public class AchievementsUseCase {

  /** Days on which each student did something in a course. */
  public interface ActivityDays {
    Mono<Void> record(Long studentId, LocalDate day);

    Flux<LocalDate> since(Long studentId, LocalDate from);
  }

  private final ActivityDays activityDays;
  private final CourseActivityGateway activity;
  private final EnrollmentGateway enrollments;
  private final Clock clock;

  public AchievementsUseCase(ActivityDays activityDays, CourseActivityGateway activity, EnrollmentGateway enrollments,
                             Clock clock) {
    this.activityDays = activityDays;
    this.activity = activity;
    this.enrollments = enrollments;
    this.clock = clock;
  }

  /** Marks today as a learning day. Never fails the request that triggered it. */
  public Mono<Void> recordActivity(Long studentId) {
    return activityDays.record(studentId, LocalDate.now(clock)).onErrorResume(e -> Mono.empty());
  }

  public Mono<Achievements.Profile> of(Long studentId) {
    LocalDate today = LocalDate.now(clock);
    return Mono.zip(
        activityDays.since(studentId, today.minusYears(1)).collectList(),
        activity.findAttemptsByStudent(studentId).collectList(),
        activity.findSubmissionsByStudent(studentId).collectList(),
        enrollments.findByStudentId(studentId).collectList())
      .map(t -> Achievements.compute(t.getT1(), t.getT2(), t.getT3(), t.getT4(), today));
  }
}
