package com.gems.education.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TapsSocialUseCaseTest {

  private static String motivo(String texto) {
    return assertThrows(IllegalArgumentException.class, () -> TapsSocialUseCase.validarTexto(texto)).getMessage();
  }

  @Test
  void aceptaTextoNormalYLoLimpia() {
    assertEquals("¡Me encantó la 3! El 2.5 fue difícil", TapsSocialUseCase.validarTexto("  ¡Me encantó   la 3!\nEl 2.5 fue difícil "));
    assertEquals("a".repeat(200), TapsSocialUseCase.validarTexto("a".repeat(200)));
  }

  @Test
  void rechazaVacioYLargo() {
    assertEquals("El comentario está vacío", motivo("   "));
    assertEquals("El comentario está vacío", motivo(null));
    assertEquals("El comentario pasa de 200 caracteres", motivo("a".repeat(201)));
  }

  @Test
  void rechazaEnlacesYDatosPersonales() {
    for (String t : new String[] {"mira https://x.io", "entra a www.algo", "ve a misitio.com ya", "sígueme en youtube.com/abc"}) {
      assertEquals("Los comentarios no pueden llevar enlaces", motivo(t), t);
    }
    for (String t : new String[] {"escríbeme a ana@gmail", "sígueme @ana_23", "llámame 300 123 4567", "3001234567"}) {
      assertEquals("Por tu seguridad, no compartas correos, teléfonos ni usuarios", motivo(t), t);
    }
  }

  @Test
  void rechazaInsultosConTildesMayusculasYLeet() {
    for (String t : new String[] {"eres un IDIOTA", "qué estúpido", "imb3cil", "malparido!"}) {
      assertEquals("Ese comentario tiene palabras que no se permiten", motivo(t), t);
    }
    // Palabras que contienen un insulto sin serlo no se bloquean.
    assertEquals("computadora y caperucita", TapsSocialUseCase.validarTexto("computadora y caperucita"));
  }

  @Test
  void quienDelLike() {
    assertEquals("u9", TapsSocialUseCase.quien(9L, "3f1c2b7a-9d4e-4c1a-8b2f-0a1b2c3d4e5f"));
    assertEquals("j3f1c2b7a-9d4e-4c1a-8b2f-0a1b2c3d4e5f", TapsSocialUseCase.quien(null, "3F1C2B7A-9D4E-4C1A-8B2F-0A1B2C3D4E5F"));
    assertNull(TapsSocialUseCase.quien(null, "no-es-uuid"));
    assertNull(TapsSocialUseCase.quien(null, null));
  }
}
