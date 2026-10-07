package com.gems.education.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

/**
 * Festivos de Colombia (Ley 51 de 1983, «Ley Emiliani»). Los correos promocionales no se envían esos días
 * (Ley 2300 de 2023). Se calculan para cualquier año: no hay que mantener una lista.
 */
public final class ColombianHolidays {
  private ColombianHolidays() {
  }

  public static boolean isHoliday(LocalDate date) {
    return of(date.getYear()).contains(date);
  }

  public static Set<LocalDate> of(int year) {
    Set<LocalDate> days = new HashSet<>();
    // Fijos: no se mueven.
    days.add(LocalDate.of(year, 1, 1));
    days.add(LocalDate.of(year, 5, 1));
    days.add(LocalDate.of(year, 7, 20));
    days.add(LocalDate.of(year, 8, 7));
    days.add(LocalDate.of(year, 12, 8));
    days.add(LocalDate.of(year, 12, 25));
    // Se trasladan al lunes siguiente si no caen en lunes.
    for (int[] md : new int[][]{{1, 6}, {3, 19}, {6, 29}, {8, 15}, {10, 12}, {11, 1}, {11, 11}}) {
      days.add(nextMonday(LocalDate.of(year, md[0], md[1])));
    }
    // Dependen de la Pascua.
    LocalDate easter = easter(year);
    days.add(easter.minusDays(3));             // Jueves Santo
    days.add(easter.minusDays(2));             // Viernes Santo
    days.add(nextMonday(easter.plusDays(39))); // Ascensión del Señor
    days.add(nextMonday(easter.plusDays(60))); // Corpus Christi
    days.add(nextMonday(easter.plusDays(68))); // Sagrado Corazón
    return days;
  }

  private static LocalDate nextMonday(LocalDate date) {
    return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
  }

  /** Domingo de Pascua (algoritmo de Meeus/Jones/Butcher, calendario gregoriano). */
  static LocalDate easter(int year) {
    int a = year % 19, b = year / 100, c = year % 100, d = b / 4, e = b % 4;
    int f = (b + 8) / 25, g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
    int month = (h + l - 7 * m + 114) / 31, day = ((h + l - 7 * m + 114) % 31) + 1;
    return LocalDate.of(year, month, day);
  }
}
