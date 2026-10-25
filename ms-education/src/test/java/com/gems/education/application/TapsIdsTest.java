package com.gems.education.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TapsIdsTest {
  @Test
  void generaIdsDeUsuarioConLaFormaEsperada() {
    for (int i = 0; i < 200; i++) assertTrue(TapsIds.esDeUsuario(TapsIds.nuevo()));
    assertEquals(12, TapsIds.nuevo().length());
  }

  @Test
  void sonDistintosYNoFormanPalabras() {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 5000; i++) ids.add(TapsIds.nuevo(new Random(i)));
    assertEquals(5000, ids.size());
    assertTrue(ids.stream().noneMatch(id -> id.substring(2).matches(".*[aeiou01lo].*")));
  }

  @Test
  void losIdsDelBancoNoSonDeUsuario() {
    assertFalse(TapsIds.esDeUsuario("mc-gancho"));
    assertFalse(TapsIds.esDeUsuario("napoleon"));
    assertFalse(TapsIds.esDeUsuario("u-corto"));
    assertFalse(TapsIds.esDeUsuario("u-AAAAAAAAAA"));
    assertFalse(TapsIds.esDeUsuario(null));
  }
}