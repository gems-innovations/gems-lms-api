package com.gems.education.domain.entities;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Light gamification computed from what a student already did: points and level, the streak of
 * consecutive days with learning activity, a weekly goal and badges. Nothing is stored apart from
 * the days with activity, so it can never disagree with the grades and progress it comes from.
 */
public final class Achievements {
  public static final int XP_QUIZ_PASSED = 20;
  public static final int XP_PERFECT_BONUS = 10;
  public static final int XP_ASSIGNMENT = 15;
  public static final int XP_COURSE_COMPLETED = 100;
  public static final int XP_ACTIVE_DAY = 5;
  /** Days with activity per week the student aims for. */
  public static final int WEEKLY_GOAL_DAYS = 4;

  private Achievements() {
  }

  public record Badge(String id, String title, String description, boolean earned, LocalDateTime earnedAt,
                      int progress, int target) {
  }

  public record Profile(int xp, int level, int levelXp, int nextLevelXp, int streak, int bestStreak,
                        boolean activeToday, int weekDays, int weeklyGoal, List<LocalDate> recentDays,
                        List<Badge> badges) {
  }

  /** XP needed to reach a level: 0, 100, 250, 450, 700… (each level asks 50 more than the last). */
  public static int xpForLevel(int level) {
    return 25 * (level - 1) * (level + 2);
  }

  public static Profile compute(Collection<LocalDate> activityDays, List<QuizAttempt> attempts,
                                List<AssignmentSubmission> submissions, List<Enrollment> enrollments, LocalDate today) {
    SortedSet<LocalDate> days = new TreeSet<>(activityDays);

    // Best attempt per quiz: retrying does not farm points.
    List<QuizAttempt> bestPerQuiz = attempts.stream()
      .collect(java.util.stream.Collectors.toMap(a -> a.courseId() + ":" + a.blockId(), Function.identity(),
        (a, b) -> a.score() >= b.score() ? a : b))
      .values().stream().toList();
    List<QuizAttempt> passed = bestPerQuiz.stream().filter(QuizAttempt::passed).toList();
    long perfect = bestPerQuiz.stream().filter(a -> a.score() >= 100).count();
    List<AssignmentSubmission> delivered = submissions.stream().filter(s -> s.submittedAt() != null).toList();
    List<Enrollment> completed = enrollments.stream()
      .filter(e -> e.getCompletedAt() != null || (e.getProgress() != null && e.getProgress() >= 100)).toList();

    int xp = passed.size() * XP_QUIZ_PASSED + (int) perfect * XP_PERFECT_BONUS + delivered.size() * XP_ASSIGNMENT
      + completed.size() * XP_COURSE_COMPLETED + days.size() * XP_ACTIVE_DAY;
    int level = 1;
    while (xp >= xpForLevel(level + 1)) level++;

    int streak = currentStreak(days, today);
    int best = bestStreak(days);
    LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    int weekDays = (int) days.stream().filter(d -> !d.isBefore(monday) && !d.isAfter(today)).count();
    List<LocalDate> recent = days.stream().filter(d -> !d.isBefore(today.minusDays(27))).toList();

    List<Badge> badges = new ArrayList<>();
    badges.add(badge("first-step", "Primer paso", "Inscríbete en tu primer curso", enrollments.size(), 1,
      first(enrollments, Enrollment::getEnrolledAt)));
    badges.add(badge("first-quiz", "Primera evaluación aprobada", "Aprueba un quiz", passed.size(), 1,
      first(passed, QuizAttempt::completedAt)));
    badges.add(badge("perfect", "Puntaje perfecto", "Saca 100 en un quiz", (int) perfect, 1,
      first(bestPerQuiz.stream().filter(a -> a.score() >= 100).toList(), QuizAttempt::completedAt)));
    badges.add(badge("quiz-master", "Maestría en evaluaciones", "Aprueba 10 quizzes", passed.size(), 10,
      nth(passed, QuizAttempt::completedAt, 10)));
    badges.add(badge("on-time", "Entrega cumplida", "Entrega tu primera tarea", delivered.size(), 1,
      first(delivered, AssignmentSubmission::submittedAt)));
    badges.add(badge("finisher", "Curso completado", "Termina un curso", completed.size(), 1,
      first(completed, Enrollment::getCompletedAt)));
    badges.add(badge("collector", "Coleccionista", "Termina 3 cursos", completed.size(), 3,
      nth(completed, Enrollment::getCompletedAt, 3)));
    badges.add(badge("streak-3", "Racha de 3 días", "Aprende 3 días seguidos", best, 3, null));
    badges.add(badge("streak-7", "Semana imparable", "Aprende 7 días seguidos", best, 7, null));
    badges.add(badge("streak-30", "Hábito de acero", "Aprende 30 días seguidos", best, 30, null));
    badges.add(badge("weekly-goal", "Meta semanal", "Aprende " + WEEKLY_GOAL_DAYS + " días en una semana",
      bestWeek(days), WEEKLY_GOAL_DAYS, null));

    return new Profile(xp, level, xpForLevel(level), xpForLevel(level + 1), streak, best,
      days.contains(today), weekDays, WEEKLY_GOAL_DAYS, recent, badges);
  }

  /** Consecutive days ending today, or yesterday when the student has not studied yet today. */
  static int currentStreak(SortedSet<LocalDate> days, LocalDate today) {
    LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
    int streak = 0;
    while (days.contains(cursor)) {
      streak++;
      cursor = cursor.minusDays(1);
    }
    return streak;
  }

  static int bestStreak(SortedSet<LocalDate> days) {
    int best = 0;
    int run = 0;
    LocalDate previous = null;
    for (LocalDate d : days) {
      run = previous != null && previous.plusDays(1).equals(d) ? run + 1 : 1;
      best = Math.max(best, run);
      previous = d;
    }
    return best;
  }

  static int bestWeek(SortedSet<LocalDate> days) {
    return days.stream()
      .collect(java.util.stream.Collectors.groupingBy(d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        java.util.stream.Collectors.counting()))
      .values().stream().mapToInt(Long::intValue).max().orElse(0);
  }

  private static Badge badge(String id, String title, String description, int progress, int target, LocalDateTime earnedAt) {
    boolean earned = progress >= target;
    return new Badge(id, title, description, earned, earned ? earnedAt : null, Math.min(progress, target), target);
  }

  private static <T> LocalDateTime first(List<T> items, Function<T, LocalDateTime> when) {
    return nth(items, when, 1);
  }

  private static <T> LocalDateTime nth(List<T> items, Function<T, LocalDateTime> when, int n) {
    List<LocalDateTime> sorted = items.stream().map(when).filter(Objects::nonNull).sorted(Comparator.naturalOrder()).toList();
    return Optional.of(sorted).filter(l -> l.size() >= n).map(l -> l.get(n - 1)).orElse(null);
  }
}
