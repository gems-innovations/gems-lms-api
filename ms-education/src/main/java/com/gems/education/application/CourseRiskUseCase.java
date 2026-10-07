package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.NotificationGateway;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.Notification;
import com.gems.education.domain.entities.RiskAssessment;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Students of a course at medium or high risk of falling behind, and reminders to them. */
public class CourseRiskUseCase {
  private final GradebookUseCase gradebook;
  private final EnrollmentGateway enrollments;
  private final AchievementsUseCase.ActivityDays activityDays;
  private final NotificationGateway notifications;
  private final Clock clock;

  public CourseRiskUseCase(GradebookUseCase gradebook, EnrollmentGateway enrollments,
                           AchievementsUseCase.ActivityDays activityDays, NotificationGateway notifications, Clock clock) {
    this.gradebook = gradebook;
    this.enrollments = enrollments;
    this.activityDays = activityDays;
    this.notifications = notifications;
    this.clock = clock;
  }

  public record CourseRisk(Long courseId, int students, int high, int medium, List<RiskAssessment.Risk> atRisk) {
  }

  public Mono<CourseRisk> course(Long courseId) {
    LocalDate today = LocalDate.now(clock);
    return Mono.zip(gradebook.course(courseId),
        enrollments.findByCourseId(courseId).filter(e -> !"cancelled".equalsIgnoreCase(e.getStatus())).collectList())
      .flatMap(t -> {
        GradebookUseCase.Gradebook book = t.getT1();
        List<Enrollment> active = t.getT2().stream().filter(e -> e.getCompletedAt() == null).toList();
        if (active.isEmpty()) return Mono.just(new CourseRisk(courseId, 0, 0, 0, List.of()));
        List<Long> ids = active.stream().map(Enrollment::getStudentId).toList();
        return activityDays.lastActive(ids).map(last -> build(courseId, book, t.getT2(), active, last, today));
      });
  }

  private CourseRisk build(Long courseId, GradebookUseCase.Gradebook book, List<Enrollment> all, List<Enrollment> active,
                           Map<Long, LocalDate> last, LocalDate today) {
    Map<Long, GradebookUseCase.Row> rows = book.rows().stream()
      .collect(Collectors.toMap(GradebookUseCase.Row::studentId, Function.identity(), (a, b) -> a));
    // A block counts as "due" for a student once at least half of the class delivered it.
    Map<Long, Long> delivered = new HashMap<>();
    for (GradebookUseCase.Row row : book.rows()) {
      for (GradebookUseCase.Cell c : row.cells()) {
        if (!GradebookUseCase.Cell.MISSING.equals(c.state())) delivered.merge(c.blockId(), 1L, Long::sum);
      }
    }
    int classSize = Math.max(1, book.rows().size());
    double avgProgress = all.stream().map(Enrollment::getProgress).filter(Objects::nonNull).mapToInt(Integer::intValue)
      .average().orElse(0);

    List<RiskAssessment.Risk> risks = active.stream().map(e -> {
      GradebookUseCase.Row row = rows.get(e.getStudentId());
      int missing = 0;
      int graded = 0;
      if (row != null) {
        for (GradebookUseCase.Cell c : row.cells()) {
          if (GradebookUseCase.Cell.GRADED.equals(c.state())) graded++;
          if (GradebookUseCase.Cell.MISSING.equals(c.state()) && delivered.getOrDefault(c.blockId(), 0L) * 2 >= classSize) missing++;
        }
      }
      return RiskAssessment.assess(new RiskAssessment.Signals(e.getStudentId(), e.getId(),
        e.getEnrolledAt() == null ? null : e.getEnrolledAt().toLocalDate(), last.get(e.getStudentId()),
        e.getProgress() == null ? 0 : e.getProgress(), row == null ? null : row.currentGrade(), graded, missing,
        avgProgress), today);
    }).filter(r -> !"low".equals(r.level()))
      .sorted(Comparator.comparingInt(RiskAssessment.Risk::score).reversed())
      .toList();

    return new CourseRisk(courseId, active.size(), (int) risks.stream().filter(r -> "high".equals(r.level())).count(),
      (int) risks.stream().filter(r -> "medium".equals(r.level())).count(), risks);
  }

  /** In-app reminder from the teacher to a student of the course. */
  public Mono<Notification> remind(String institutionId, Long courseId, String courseTitle, Long studentId, String message) {
    String body = message == null || message.isBlank()
      ? "Tu docente quiere saber cómo vas en " + courseTitle + ". Retoma el curso cuando puedas; si algo te frena, escríbele."
      : message.trim();
    return notifications.save(new Notification(null, institutionId, studentId, Notification.REMINDER,
      "Recordatorio de " + courseTitle, body.length() > 500 ? body.substring(0, 500) : body, courseId, null,
      LocalDateTime.now(clock), false));
  }
}
