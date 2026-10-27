package com.gems.education.application.exceptions;

/**
 * No hay un Tap vivo con ese id para quien pregunta. Se responde 404 también cuando el Tap existe y es de otra persona:
 * nunca se revela que existe.
 */
public class TapsCreadorNotFoundException extends RuntimeException {
  public static final String CODE = "TAP_NOT_FOUND";

  public TapsCreadorNotFoundException() {
    super("Este Tap no existe");
  }
}
