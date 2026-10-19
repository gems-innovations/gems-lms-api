package com.gems.education.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lista blanca de Taps que cuentan para el ranking: id, tema y número de preguntas de cada uno. Sale del recurso
 * {@code taps/banco.txt}, generado desde el banco de la web con {@code ms-education/scripts/generar-taps-banco.mjs}.
 */
public final class TapsBanco {
  public static final String RECURSO = "taps/banco.txt";

  public record Tap(String id, String tema, int total) {}

  private final Map<String, Tap> taps;

  public TapsBanco(Map<String, Tap> taps) {
    this.taps = Map.copyOf(taps);
  }

  public Optional<Tap> buscar(String tapId) {
    return Optional.ofNullable(tapId == null ? null : taps.get(tapId));
  }

  public int cantidad() {
    return taps.size();
  }

  /** Lee el recurso del classpath (líneas «id tema total»; las que empiezan por # son comentarios). */
  public static TapsBanco delClasspath() {
    InputStream in = TapsBanco.class.getClassLoader().getResourceAsStream(RECURSO);
    if (in == null) throw new IllegalStateException("Falta el recurso " + RECURSO);
    return leer(in);
  }

  static TapsBanco leer(InputStream in) {
    Map<String, Tap> taps = new LinkedHashMap<>();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String linea;
      while ((linea = r.readLine()) != null) {
        linea = linea.trim();
        if (linea.isEmpty() || linea.startsWith("#")) continue;
        String[] partes = linea.split("\\s+");
        if (partes.length != 3) throw new IllegalStateException("Línea no válida en " + RECURSO + ": " + linea);
        taps.put(partes[0], new Tap(partes[0], partes[1], Integer.parseInt(partes[2])));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new TapsBanco(taps);
  }
}
