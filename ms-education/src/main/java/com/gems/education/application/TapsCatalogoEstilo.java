package com.gems.education.application;

import java.util.Set;

/**
 * Catálogo cerrado de la personalización de un Tap ({@code estilo}). Copia los ids de la web
 * (gems-web-pagina projects/main/src/app/taps/taps.estilo.ts: MARCOS, PALETAS, LETRAS, PATRONES, MUSICAS, STICKERS): si allí
 * se añade uno, se añade aquí, o la API lo rechaza al guardar.
 */
public final class TapsCatalogoEstilo {
  public static final Set<String> MARCOS = Set.of("brillo", "neon", "arcoiris", "latido", "destellos", "pixel");
  public static final Set<String> PALETAS = Set.of("oceano", "atardecer", "bosque", "dulce", "noche", "grafito");
  public static final Set<String> LETRAS = Set.of("redonda", "pixel");
  public static final Set<String> PATRONES = Set.of("puntos", "rayas", "cuadricula", "ondas");
  public static final Set<String> MUSICAS = Set.of("pixel-quest-loop", "cave-quiz-crawl", "magma-quest-loop",
    "village-morning-loop", "island-quiz-loop", "cozy-study-loop", "puzzle-tick-loop", "wonder-loop", "orbit-quiz-drift");
  public static final Set<String> STICKERS = Set.of("estrella", "corazon", "rayo", "llama", "corona", "trofeo", "cohete",
    "bombillo", "nota", "diana", "medalla", "pregunta", "exclamacion", "check", "nube", "planeta");

  public static final int MAX_CAPAS = 6;
  public static final int MAX_TEXTO_CAPA = 40;

  private TapsCatalogoEstilo() {
  }
}
