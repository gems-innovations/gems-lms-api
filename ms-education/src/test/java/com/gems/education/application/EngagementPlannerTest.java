package com.gems.education.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementPlannerTest {
  // 2026-10-05 is a Monday.
  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
  private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);

  private static EngagementPlanner.Course course(int progress, Integer percentile, int size) {
    return new EngagementPlanner.Course(9L, "Matemáticas", progress, percentile, size);
  }

  private static Set<LocalDate> daysEndingAgo(LocalDate today, int ago, int length) {
    return IntStream.range(0, length).mapToObj(i -> today.minusDays(ago + i)).collect(Collectors.toSet());
  }

  private static EngagementPlanner.Student student(Set<LocalDate> days, EngagementPlanner.Course... courses) {
    return new EngagementPlanner.Student(1L, List.of(courses), days, false, Set.of());
  }

  @Test
  void streakAtRiskWhenStudiedUntilYesterday() {
    var email = EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, 1, 5), course(40, null, 1)), WEDNESDAY);
    assertThat(email).get().satisfies(e -> {
      assertThat(e.kind()).isEqualTo(EngagementPlanner.STREAK);
      assertThat(e.subject()).contains("5 días");
    });
  }

  @Test
  void shortStreakIsNotWorthAnEmail() {
    assertThat(EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, 1, 2), course(40, null, 1)), WEDNESDAY)).isEmpty();
  }

  @Test
  void missYouByStageOfAbsence() {
    for (int[] c : new int[][]{{3, 0}, {5, 0}, {7, 1}, {10, 1}, {14, 2}, {30, 2}}) {
      var kind = List.of(EngagementPlanner.MISS_3, EngagementPlanner.MISS_7, EngagementPlanner.MISS_14).get(c[1]);
      var email = EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, c[0], 1), course(40, null, 1)), WEDNESDAY);
      assertThat(email).get().extracting(EngagementPlanner.Email::kind).isEqualTo(kind);
    }
    assertThat(EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, 2, 1), course(40, null, 1)), WEDNESDAY)).isEmpty();
    assertThat(EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, 31, 1), course(40, null, 1)), WEDNESDAY)).isEmpty();
  }

  @Test
  void aStageSkippedByTheWeeklyCapStillGoesOutLater() {
    var days = daysEndingAgo(WEDNESDAY, 10, 1);
    var afterMiss3 = new EngagementPlanner.Student(1L, List.of(course(40, null, 1)), days, false, Set.of(EngagementPlanner.MISS_3));
    assertThat(EngagementPlanner.plan(afterMiss3, WEDNESDAY)).get()
      .extracting(EngagementPlanner.Email::kind).isEqualTo(EngagementPlanner.MISS_7);
  }

  @Test
  void stopsWritingAfterTheLastReminder() {
    var days = daysEndingAgo(WEDNESDAY, 20, 1);
    var done = new EngagementPlanner.Student(1L, List.of(course(40, null, 1)), days, false, Set.of(EngagementPlanner.MISS_14));
    assertThat(EngagementPlanner.plan(done, WEDNESDAY)).isEmpty();
  }

  @Test
  void neverRepeatsAKindOrSendsTwiceAWeek() {
    var days = daysEndingAgo(WEDNESDAY, 3, 1);
    var repeated = new EngagementPlanner.Student(1L, List.of(course(40, null, 1)), days, false, Set.of(EngagementPlanner.MISS_3));
    var sentThisWeek = new EngagementPlanner.Student(1L, List.of(course(40, null, 1)), days, true, Set.of());
    assertThat(EngagementPlanner.plan(repeated, WEDNESDAY)).isEmpty();
    assertThat(EngagementPlanner.plan(sentThisWeek, WEDNESDAY)).isEmpty();
  }

  @Test
  void topOnlyOnMondaysForActiveStudentsInBigEnoughCourses() {
    var activeToday = daysEndingAgo(MONDAY, 0, 1);
    assertThat(EngagementPlanner.plan(student(activeToday, course(90, 8, 25)), MONDAY)).get()
      .satisfies(e -> assertThat(e.subject()).contains("top 10 %"));
    assertThat(EngagementPlanner.plan(student(activeToday, course(90, 8, 3)), MONDAY)).isEmpty();
    assertThat(EngagementPlanner.plan(student(activeToday, course(30, 60, 25)), MONDAY)).isEmpty();
    assertThat(EngagementPlanner.plan(student(daysEndingAgo(WEDNESDAY, 0, 1), course(90, 8, 25)), WEDNESDAY)).isEmpty();
  }

  @Test
  void percentileRanksTheBestFirst() {
    var progress = List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
    assertThat(EngagementPlanner.percentile(100, progress)).isEqualTo(10);
    assertThat(EngagementPlanner.percentile(10, progress)).isEqualTo(100);
  }
}
