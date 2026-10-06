package com.gems.education.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementsTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 8); // jueves

  private static QuizAttempt attempt(long block, int score, boolean passed) {
    return new QuizAttempt(block, 1L, 7L, 1L, block, 1L, 1, "[]", score, passed, null, TODAY.atStartOfDay());
  }

  private static Achievements.Badge badge(Achievements.Profile p, String id) {
    return p.badges().stream().filter(b -> b.id().equals(id)).findFirst().orElseThrow();
  }

  @Test
  void theStreakSurvivesUntilTheEndOfTodayAndBreaksOnAGap() {
    Set<LocalDate> days = Set.of(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3), TODAY.minusDays(6));
    Achievements.Profile p = Achievements.compute(days, List.of(), List.of(), List.of(), TODAY);
    assertEquals(3, p.streak(), "yesterday's streak still counts before studying today");
    assertEquals(3, p.bestStreak());
    assertFalse(p.activeToday());
    assertEquals(3, p.weekDays(), "Monday to today");

    Achievements.Profile broken = Achievements.compute(Set.of(TODAY.minusDays(2)), List.of(), List.of(), List.of(), TODAY);
    assertEquals(0, broken.streak());
  }

  @Test
  void retryingAQuizDoesNotFarmPointsAndPerfectScoresEarnABonus() {
    List<QuizAttempt> attempts = List.of(attempt(1, 40, false), attempt(1, 100, true), attempt(1, 100, true), attempt(2, 70, true));
    Achievements.Profile p = Achievements.compute(Set.of(), attempts, List.of(), List.of(), TODAY);
    assertEquals(2 * Achievements.XP_QUIZ_PASSED + Achievements.XP_PERFECT_BONUS, p.xp());
    assertTrue(badge(p, "first-quiz").earned());
    assertTrue(badge(p, "perfect").earned());
    assertEquals(2, badge(p, "quiz-master").progress());
    assertFalse(badge(p, "quiz-master").earned());
  }

  @Test
  void levelsGetProgressivelyLonger() {
    assertEquals(0, Achievements.xpForLevel(1));
    assertEquals(100, Achievements.xpForLevel(2));
    assertEquals(250, Achievements.xpForLevel(3));
    Enrollment done = new Enrollment(1L, 7L, 1L, "completed", LocalDateTime.now(), 100, LocalDateTime.now());
    Achievements.Profile p = Achievements.compute(Set.of(TODAY), List.of(), List.of(), List.of(done), TODAY);
    assertEquals(Achievements.XP_COURSE_COMPLETED + Achievements.XP_ACTIVE_DAY, p.xp());
    assertEquals(2, p.level());
    assertTrue(badge(p, "finisher").earned());
    assertTrue(badge(p, "first-step").earned());
  }
}
