package com.gems.education.domain.entities;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Early warning for students who are falling behind in a course. Each signal adds points and a
 * reason the teacher can read; the level comes from the total. The signals are deliberately
 * simple so a teacher can check them by looking at the gradebook.
 */
public final class RiskAssessment {
  public static final int HIGH = 60;
  public static final int MEDIUM = 30;

  private RiskAssessment() {
  }

  /** What the course knows about one student. lastActive is null when there is no recorded activity. */
  public record Signals(Long studentId, Long enrollmentId, LocalDate enrolledOn, LocalDate lastActive, int progress,
                        Double currentGrade, int gradedItems, int missingItems, double classAverageProgress) {
  }

  public record Risk(Long studentId, Long enrollmentId, int score, String level, List<String> reasons,
                     Integer daysInactive, int progress, Double currentGrade, int missingItems) {
  }

  public static Risk assess(Signals s, LocalDate today) {
    int score = 0;
    List<String> reasons = new ArrayList<>();

    Integer inactive = s.lastActive() == null ? null : (int) ChronoUnit.DAYS.between(s.lastActive(), today);
    long sinceEnrolled = s.enrolledOn() == null ? 0 : ChronoUnit.DAYS.between(s.enrolledOn(), today);
    if (inactive == null && s.progress() == 0 && sinceEnrolled >= 7) {
      score += 35;
      reasons.add("No ha empezado el curso (inscrito hace " + sinceEnrolled + " días)");
    } else if (inactive != null && inactive >= 14) {
      score += 40;
      reasons.add(inactive + " días sin actividad");
    } else if (inactive != null && inactive >= 7) {
      score += 25;
      reasons.add(inactive + " días sin actividad");
    }

    if (s.currentGrade() != null && s.gradedItems() > 0) {
      if (s.currentGrade() < 40) {
        score += 45;
        reasons.add("Nota actual de " + Math.round(s.currentGrade()) + "/100");
      } else if (s.currentGrade() < 60) {
        // A failing grade alone is worth a look: it reaches medium risk.
        score += 30;
        reasons.add("Nota actual de " + Math.round(s.currentGrade()) + "/100, por debajo de 60");
      }
    }

    if (s.missingItems() > 0) {
      score += Math.min(30, s.missingItems() * 12);
      reasons.add(s.missingItems() == 1 ? "1 entrega pendiente que la mayoría ya hizo"
        : s.missingItems() + " entregas pendientes que la mayoría ya hizo");
    }

    if (s.classAverageProgress() - s.progress() >= 30) {
      score += 15;
      reasons.add("Avance de " + s.progress() + "% frente al " + Math.round(s.classAverageProgress()) + "% del grupo");
    }

    score = Math.min(100, score);
    String level = score >= HIGH ? "high" : score >= MEDIUM ? "medium" : "low";
    return new Risk(s.studentId(), s.enrollmentId(), score, level, reasons, inactive, s.progress(), s.currentGrade(),
      s.missingItems());
  }
}
