package com.gems.education.application.exceptions;

/** Un dispositivo envía partidas de Taps más rápido de lo que se puede jugar (se responde 429). */
public class TapsLimiteException extends RuntimeException {
  public static final String CODE = "TAPS_TOO_MANY_GAMES";

  public TapsLimiteException(String message) {
    super(message);
  }
}
