package com.gems.education.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which motivation e-mail (if any) a student gets today, Duolingo style but never more than one a
 * week: streak at risk,
 * "we miss you" after 3, 7 and 14 days away (up to 30), and a weekly "you're in the top X%" with real course data.
 * Pure logic: no I/O, so every rule is easy to test.
 */
public final class EngagementPlanner {
  public static final String STREAK = "streak";
  public static final String MISS_3 = "miss3";
  public static final String MISS_7 = "miss7";
  public static final String MISS_14 = "miss14";
  public static final String TOP = "top";

  /** Courses with fewer students than this never get a ranking (it would not mean much). */
  static final int MIN_CLASS_FOR_RANKING = 5;
  static final int TOP_PERCENT = 20;

  /**
   * One active (not completed) enrollment of the student.
   * @param percentile 1..100, lower is better (1 = the most progress in the course); null when not ranked
   */
  public record Course(Long courseId, String title, int progress, Integer percentile, int classSize) {
  }

  /**
   * @param activeDays    days with activity, any order (the last ~60 are enough)
   * @param sentThisWeek whether any motivation e-mail went out in the last 7 days (at most one a week)
   * @param sentKinds     kinds already sent during the current absence (so "miss3" is not repeated)
   */
  public record Student(Long studentId, List<Course> courses, Set<LocalDate> activeDays, boolean sentThisWeek,
                        Set<String> sentKinds) {
  }

  public record Email(String kind, Long courseId, String subject, String message, String linkPath, String linkLabel) {
  }

  private EngagementPlanner() {
  }

  public static Optional<Email> plan(Student s, LocalDate today) {
    if (s.sentThisWeek() || s.courses().isEmpty() || s.activeDays().isEmpty()) return Optional.empty();
    LocalDate last = s.activeDays().stream().max(Comparator.naturalOrder()).orElseThrow();
    if (!last.isBefore(today)) {
      return today.getDayOfWeek() == DayOfWeek.MONDAY ? top(s) : Optional.empty();
    }
    long away = ChronoUnit.DAYS.between(last, today);
    Course course = mostAdvanced(s.courses());
    String link = "/learn/courses/" + course.courseId();

    if (away == 1) {
      int streak = streakEndingOn(s.activeDays(), last);
      if (streak >= 3 && !s.sentKinds().contains(STREAK)) {
        return Optional.of(new Email(STREAK, course.courseId(), "Tu racha de " + streak + " días se pierde hoy",
          "Llevas " + streak + " días seguidos estudiando. Una lección corta de «" + course.title()
            + "» es suficiente para no perderla.", link, "Salvar mi racha"));
      }
      return Optional.empty();
    }
    // Windows instead of exact days: with one e-mail a week, a stage skipped by the cap still goes out later.
    // A later stage replaces the earlier ones, and after 30 days away we stop writing.
    if (away > 30 || s.sentKinds().contains(MISS_14)) return Optional.empty();
    if (away >= 3 && away < 7 && !s.sentKinds().contains(MISS_3)) {
      return Optional.of(new Email(MISS_3, course.courseId(), "Te extrañamos en " + course.title(),
        "Hace unos días no pasas por «" + course.title() + "». Tu avance sigue ahí, justo donde lo dejaste: "
          + "retómalo con una lección de 10 minutos.", link, "Seguir donde iba"));
    }
    if (away >= 7 && away < 14 && !s.sentKinds().contains(MISS_7)) {
      String progress = course.progress() > 0 ? "Vas en " + course.progress() + " %: te falta menos de lo que crees. "
        : "Empezar es lo más difícil y ya lo hiciste. ";
      return Optional.of(new Email(MISS_7, course.courseId(), "Tu curso te espera: " + course.title(),
        progress + "Unos días sin estudiar se recuperan con una sola lección.", link, "Volver al curso"));
    }
    if (away >= 14) {
      return Optional.of(new Email(MISS_14, course.courseId(), "¿Seguimos con " + course.title() + "?",
        "Han pasado dos semanas. Si este no es el momento, está bien: tu avance queda guardado. Y si quieres "
          + "retomarlo, una lección hoy es el mejor comienzo. No te volveremos a escribir por esto.",
        link, "Retomar el curso"));
    }
    return Optional.empty();
  }

  /** Weekly, only for students active today and really near the top of a course big enough to compare. */
  private static Optional<Email> top(Student s) {
    if (s.sentKinds().contains(TOP)) return Optional.empty();
    return s.courses().stream()
      .filter(c -> c.percentile() != null && c.classSize() >= MIN_CLASS_FOR_RANKING && c.percentile() <= TOP_PERCENT)
      .min(Comparator.comparingInt(Course::percentile))
      .map(c -> {
        int shown = c.percentile() <= 5 ? 5 : c.percentile() <= 10 ? 10 : TOP_PERCENT;
        return new Email(TOP, c.courseId(), "Estás en el top " + shown + " % de " + c.title(),
          "De las " + c.classSize() + " personas que estudian «" + c.title() + "», vas entre el " + shown
            + " % más avanzado. Lo estás haciendo muy bien: sigue así esta semana.",
          "/learn/courses/" + c.courseId(), "Seguir avanzando");
      });
  }

  /** Consecutive active days ending on {@code end}. */
  static int streakEndingOn(Set<LocalDate> days, LocalDate end) {
    int streak = 0;
    for (LocalDate d = end; days.contains(d); d = d.minusDays(1)) streak++;
    return streak;
  }

  private static Course mostAdvanced(List<Course> courses) {
    return courses.stream().max(Comparator.comparingInt(Course::progress)).orElseThrow();
  }

  /** Percentile of {@code progress} in the course (1 = best). Ties share the better rank. */
  public static int percentile(int progress, List<Integer> classProgress) {
    long ahead = classProgress.stream().filter(p -> p > progress).count();
    return (int) Math.max(1, Math.ceil((ahead + 1) * 100.0 / classProgress.size()));
  }
}
