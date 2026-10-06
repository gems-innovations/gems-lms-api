package com.gems.education.domain.entities;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSimilarityTest {
  private static final String ORIGINAL = "La fotosíntesis es el proceso mediante el cual las plantas verdes transforman la energía de la luz "
    + "solar en energía química. Ocurre en los cloroplastos gracias a la clorofila, que absorbe la luz. Como resultado se "
    + "produce glucosa y se libera oxígeno a la atmósfera, lo que permite la vida de muchos otros organismos del planeta.";

  @Test
  void aCopyWithSmallChangesIsFlaggedAndAnIndependentAnswerIsNot() {
    String copy = "**La fotosintesis** es el proceso mediante el cual las plantas verdes transforman la energia de la luz "
      + "solar en energia quimica. Ocurre en los cloroplastos gracias a la clorofila, que absorbe la luz del sol. Como resultado se "
      + "produce glucosa y se libera oxigeno a la atmosfera, lo que permite la vida de otros organismos.";
    String own = "Las plantas capturan luz con un pigmento verde llamado clorofila y con ella fabrican azúcares a partir de agua "
      + "y dióxido de carbono. En el camino sueltan oxígeno. Sin este mecanismo casi ninguna cadena alimenticia funcionaría, "
      + "porque las plantas son la base de la que dependen herbívoros y carnívoros en todos los ecosistemas.";
    Map<Long, String> texts = new LinkedHashMap<>();
    texts.put(1L, ORIGINAL);
    texts.put(2L, copy);
    texts.put(3L, own);

    List<TextSimilarity.Match> matches = TextSimilarity.matches(texts, 0.35);
    assertEquals(1, matches.size());
    assertEquals(1L, matches.get(0).submissionId());
    assertEquals(2L, matches.get(0).otherSubmissionId());
    assertTrue(matches.get(0).score() > 0.5, "score " + matches.get(0).score());
    assertTrue(matches.get(0).sharedExcerpt().contains("proceso mediante el cual"));
  }

  @Test
  void shortAnswersAreNotCompared() {
    Map<Long, String> texts = Map.of(1L, "Sí, es correcto.", 2L, "Sí, es correcto.");
    assertTrue(TextSimilarity.matches(texts, 0.1).isEmpty());
  }
}
