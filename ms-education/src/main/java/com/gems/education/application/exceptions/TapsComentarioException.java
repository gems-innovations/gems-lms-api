package com.gems.education.application.exceptions;

/** Un comentario de Tap que no se puede aceptar (422) o que no existe (404). El mensaje es para mostrarlo tal cual. */
public class TapsComentarioException extends RuntimeException {
  public static final String INVALIDO = "TAPS_COMMENT_INVALID";
  public static final String NO_EXISTE = "TAPS_COMMENT_NOT_FOUND";
  private final String code;

  public TapsComentarioException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static TapsComentarioException invalido(String message) {
    return new TapsComentarioException(INVALIDO, message);
  }

  public static TapsComentarioException noExiste() {
    return new TapsComentarioException(NO_EXISTE, "Este comentario no existe");
  }
}
