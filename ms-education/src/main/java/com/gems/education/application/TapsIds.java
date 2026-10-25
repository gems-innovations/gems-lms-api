package com.gems.education.application;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Ids de los Taps creados por personas (docentes, comunidad). Los Taps del banco de GEMS conservan sus ids de siempre
 * (p. ej. {@code mc-gancho}, {@code napoleon}); los creados desde el Estudio reciben un id generado por el servidor:
 * {@code u-} más 10 caracteres de un alfabeto sin vocales ni símbolos confusos (no forma palabras ni se adivina).
 *
 * <p>Así, el prefijo {@code u-} distingue de un vistazo un Tap de usuario de uno del banco, y todas las estadísticas
 * ({@code taps_estadistica}) pueden agruparse por origen sin una columna aparte. Un id de usuario solo cuenta para
 * estadísticas cuando el Tap está registrado (para que nadie llene la tabla con ids inventados).
 */
public final class TapsIds {
  private static final String ALFABETO = "23456789bcdfghjkmnpqrstvwxz";
  private static final int LARGO = 10;
  public static final String PREFIJO = "u-";
  private static final Pattern DE_USUARIO = Pattern.compile("^u-[23456789bcdfghjkmnpqrstvwxz]{10}$");
  private static final SecureRandom RANDOM = new SecureRandom();

  private TapsIds() {}

  /** Un id nuevo para un Tap de usuario. */
  public static String nuevo() {
    return nuevo(RANDOM);
  }

  static String nuevo(java.util.Random random) {
    StringBuilder sb = new StringBuilder(PREFIJO);
    for (int i = 0; i < LARGO; i++) sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
    return sb.toString();
  }

  /** Verdadero si el id tiene la forma de un Tap de usuario (no dice si existe). */
  public static boolean esDeUsuario(String id) {
    return id != null && DE_USUARIO.matcher(id).matches();
  }
}