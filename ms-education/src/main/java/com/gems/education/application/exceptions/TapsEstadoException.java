package com.gems.education.application.exceptions;

/** El Tap existe y es de quien pregunta, pero su estado no admite la acción (p. ej. publicar uno en revisión). 409. */
public class TapsEstadoException extends RuntimeException {
  public static final String CODE = "TAP_ESTADO_INVALIDO";

  public TapsEstadoException(String message) {
    super(message);
  }
}
