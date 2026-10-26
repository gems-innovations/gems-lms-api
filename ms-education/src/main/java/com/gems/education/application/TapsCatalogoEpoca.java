package com.gems.education.application;

import java.util.Set;

/**
 * Catálogo cerrado del tema «época» del motor de Taps (public/taps-engine/themes/epoca: theme.json characters y shots,
 * scenery.js WORLD_NAMES, rig.js ITEMS). Un Tap de creador solo puede señalar escenas de esta lista: el motor ignora un
 * anfitrión desconocido, pero la API no guarda nada que no esté aquí.
 */
public final class TapsCatalogoEpoca {
  public static final String TEMA = "epoca";

  /** scenery.js WORLDS. */
  public static final Set<String> MUNDOS = Set.of("taller-londres", "excavacion-egipto", "foro-roma", "sala-medieval",
    "laboratorio-moderno");
  /** theme.json characters (los 7 puntos de partida). */
  public static final Set<String> ANFITRIONES = Set.of("grabador", "arqueologa", "senador", "escriba", "artesano",
    "investigadora", "periodista");
  /** rig.js ITEMS. */
  public static final Set<String> OBJETOS = Set.of("pluma", "cepillo", "pergamino", "libro", "lupa", "matraz", "antorcha", "mapa");
  /** theme.json shots. */
  public static final Set<String> ENCUADRES = Set.of("centro", "izquierda", "derecha", "cerca-izquierda", "cerca-derecha", "lejos");

  private TapsCatalogoEpoca() {
  }
}
