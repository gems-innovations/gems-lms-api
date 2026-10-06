package com.gems.education.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskAssessmentTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

  private static RiskAssessment.Signals signals(LocalDate lastActive, int progress, Double grade, int graded, int missing) {
    return new RiskAssessment.Signals(7L, 70L, TODAY.minusDays(40), lastActive, progress, grade, graded, missing, 60);
  }

  @Test
  void aStudentWhoStoppedAndFailsIsHighRiskWithReadableReasons() {
    RiskAssessment.Risk r = RiskAssessment.assess(signals(TODAY.minusDays(16), 20, 35.0, 2, 2), TODAY);
    assertEquals("high", r.level());
    assertEquals(100, r.score());
    assertTrue(r.reasons().contains("16 días sin actividad"));
    assertTrue(r.reasons().stream().anyMatch(x -> x.startsWith("Nota actual de 35")));
    assertTrue(r.reasons().stream().anyMatch(x -> x.contains("2 entregas pendientes")));
    assertTrue(r.reasons().stream().anyMatch(x -> x.contains("frente al 60%")));
  }

  @Test
  void aStudentWhoNeverStartedIsFlagged() {
    RiskAssessment.Risk r = RiskAssessment.assess(signals(null, 0, null, 0, 0), TODAY);
    assertEquals("medium", r.level());
    assertTrue(r.reasons().get(0).startsWith("No ha empezado el curso"));
  }

  @Test
  void anActiveStudentDoingWellIsLowRisk() {
    RiskAssessment.Risk r = RiskAssessment.assess(signals(TODAY.minusDays(1), 70, 85.0, 3, 0), TODAY);
    assertEquals("low", r.level());
    assertTrue(r.reasons().isEmpty());
  }

  @Test
  void aFailingGradeAloneIsWorthALook() {
    RiskAssessment.Risk r = RiskAssessment.assess(signals(TODAY, 60, 45.0, 2, 0), TODAY);
    assertEquals("medium", r.level());
  }
}
