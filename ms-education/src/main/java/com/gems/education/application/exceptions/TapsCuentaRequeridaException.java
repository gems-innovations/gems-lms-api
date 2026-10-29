package com.gems.education.application.exceptions;

/** Una cuenta de invitado intenta usar me gusta o comentarios de Taps (403): la web le ofrece crear su cuenta. */
public class TapsCuentaRequeridaException extends RuntimeException {
  public static final String CODE = "TAPS_SOCIAL_REQUIERE_CUENTA";

  public TapsCuentaRequeridaException() {
    super("Crea tu cuenta para dar me gusta y comentar");
  }
}
