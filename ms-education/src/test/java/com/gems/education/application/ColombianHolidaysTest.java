package com.gems.education.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ColombianHolidaysTest {
  @Test
  void easterDates() {
    assertThat(ColombianHolidays.easter(2025)).isEqualTo(LocalDate.of(2025, 4, 20));
    assertThat(ColombianHolidays.easter(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
    assertThat(ColombianHolidays.easter(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
  }

  @Test
  void the2026CalendarHasEighteenHolidays() {
    // Calendario oficial 2026.
    assertThat(ColombianHolidays.of(2026)).containsExactlyInAnyOrder(
      LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23), LocalDate.of(2026, 4, 2),
      LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 8),
      LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 7),
      LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16),
      LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25));
  }

  @Test
  void ordinaryDaysAreNotHolidays() {
    assertThat(ColombianHolidays.isHoliday(LocalDate.of(2026, 10, 7))).isFalse();
    assertThat(ColombianHolidays.isHoliday(LocalDate.of(2026, 10, 12))).isTrue();
  }
}
