package com.gems.education.application.exceptions;

/** El borrador cambió desde la revisión que trae If-Match (autoguardado desde dos pestañas). Se responde 409. */
public class TapsCreadorConflictoException extends RuntimeException {
  public static final String CODE = "TAP_REVISION_CONFLICT";

  public TapsCreadorConflictoException() {
    super("El Tap cambió desde la última vez que lo cargaste");
  }
}
