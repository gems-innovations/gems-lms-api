package com.gems.education.application;

import com.gems.education.application.gateway.TapsEstadisticasGateway;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Estadísticas por Tap, anónimas y agregadas: vistas, primera respuesta, completados, compartidos, y por pregunta cuántas
 * se respondieron, cuántas bien y cuántas personas se fueron después de ella (abandono).
 *
 * <ul>
 *   <li>Solo cuentan los Taps del banco ({@link TapsBanco}); un evento de un Tap desconocido responde 400.</li>
 *   <li>Eventos: {@code vista}, {@code respuesta} (con su número de pregunta, desde 0, y si fue acierto),
 *       {@code completado} y {@code compartido}.</li>
 *   <li>No se guarda la IP ni ninguna identidad: solo contadores por día (hora de Bogotá).</li>
 *   <li>Son cifras orientativas, no antifraude: cualquiera puede enviar eventos, así que sirven para decidir el orden
 *       de los Taps, no para premios.</li>
 * </ul>
 */
public class TapsEstadisticasUseCase {
  public static final int DIAS_POR_DEFECTO = 30;
  public static final int DIAS_MAXIMOS = 90;
  /** Preguntas máximas de un Tap (una historia cuenta sus pasos además de la pregunta final; en el banco figura con 1). */
  static final int MAX_PREGUNTAS = 10;
  static final Set<String> EVENTOS = Set.of("vista", "respuesta", "completado", "compartido");
  /** Evento interno: se suma junto a {@code respuesta} cuando la respuesta fue correcta. */
  static final String ACIERTO = "acierto";

  /** Un evento tal como llega de la web. {@code pregunta} y {@code acierto} solo cuentan en {@code respuesta}. */
  public record Evento(String tapId, String evento, Integer pregunta, Boolean acierto) {}

  public record Pregunta(int n, long respuestas, long aciertos, long abandonos) {}

  public record TapEstadistica(String tapId, String tema, long vistas, long primeraRespuesta, long completados,
                               long compartidos, long respuestas, long aciertos, List<Pregunta> preguntas) {}

  public record Resumen(String desde, String hasta, int dias, String masVisto, List<TapEstadistica> taps) {}

  private final TapsEstadisticasGateway gateway;
  private final TapsBanco banco;
  private final Clock clock;

  public TapsEstadisticasUseCase(TapsEstadisticasGateway gateway, TapsBanco banco) {
    this(gateway, banco, Clock.system(TapsRankingUseCase.ZONA));
  }

  public TapsEstadisticasUseCase(TapsEstadisticasGateway gateway, TapsBanco banco, Clock clock) {
    this.gateway = gateway;
    this.banco = banco;
    this.clock = clock;
  }

  public Mono<Void> registrar(Evento e) {
    if (e == null || e.tapId() == null || e.evento() == null) throw new IllegalArgumentException("falta tapId o evento");
    TapsBanco.Tap tap = banco.buscar(e.tapId()).orElseThrow(() -> new IllegalArgumentException("Tap desconocido"));
    if (!EVENTOS.contains(e.evento())) throw new IllegalArgumentException("evento no válido");
    LocalDate dia = LocalDate.now(clock);
    if (!"respuesta".equals(e.evento())) return gateway.sumar(tap.id(), dia, e.evento(), 0);
    int n = e.pregunta() == null ? -1 : e.pregunta();
    if (n < 0 || n >= MAX_PREGUNTAS) throw new IllegalArgumentException("pregunta fuera del Tap");
    Mono<Void> respuesta = gateway.sumar(tap.id(), dia, "respuesta", n);
    return Boolean.TRUE.equals(e.acierto()) ? respuesta.then(gateway.sumar(tap.id(), dia, ACIERTO, n)) : respuesta;
  }

  public Mono<Resumen> leer(Integer dias) {
    int d = dias == null ? DIAS_POR_DEFECTO : Math.max(1, Math.min(DIAS_MAXIMOS, dias));
    LocalDate hasta = LocalDate.now(clock), desde = hasta.minusDays(d - 1L);
    return gateway.leer(desde, hasta).collectList().map(filas -> resumir(filas, desde, hasta, d));
  }

  private Resumen resumir(List<TapsEstadisticasGateway.Fila> filas, LocalDate desde, LocalDate hasta, int dias) {
    Map<String, Map<String, Long>> sueltos = new HashMap<>();   // tapId -> evento -> cantidad (pregunta 0 para los no-respuesta)
    Map<String, Map<Integer, long[]>> porPregunta = new HashMap<>();   // tapId -> n -> [respuestas, aciertos]
    for (TapsEstadisticasGateway.Fila f : filas) {
      if (banco.buscar(f.tapId()).isEmpty()) continue;
      if ("respuesta".equals(f.evento()) || ACIERTO.equals(f.evento())) {
        long[] par = porPregunta.computeIfAbsent(f.tapId(), k -> new HashMap<>()).computeIfAbsent(f.pregunta(), k -> new long[2]);
        par["respuesta".equals(f.evento()) ? 0 : 1] += f.cantidad();
      } else {
        sueltos.computeIfAbsent(f.tapId(), k -> new HashMap<>()).merge(f.evento(), f.cantidad(), Long::sum);
      }
    }
    List<TapEstadistica> taps = new ArrayList<>();
    for (String id : unir(sueltos.keySet(), porPregunta.keySet())) {
      TapsBanco.Tap tap = banco.buscar(id).orElseThrow();
      Map<String, Long> s = sueltos.getOrDefault(id, Map.of());
      Map<Integer, long[]> pp = porPregunta.getOrDefault(id, Map.of());
      long completados = s.getOrDefault("completado", 0L), respuestas = 0, aciertos = 0;
      List<Pregunta> preguntas = new ArrayList<>();
      int largo = Math.max(tap.total(), pp.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1);   // una historia: pasos + final
      for (int n = 0; n < largo; n++) {
        long[] a = pp.getOrDefault(n, new long[2]);
        long siguientes = n + 1 < largo ? pp.getOrDefault(n + 1, new long[2])[0] : completados;
        preguntas.add(new Pregunta(n, a[0], a[1], Math.max(0, a[0] - siguientes)));
        respuestas += a[0];
        aciertos += a[1];
      }
      taps.add(new TapEstadistica(id, tap.tema(), s.getOrDefault("vista", 0L), preguntas.isEmpty() ? 0 : preguntas.get(0).respuestas(),
        completados, s.getOrDefault("compartido", 0L), respuestas, aciertos, preguntas));
    }
    taps.sort(Comparator.comparingLong(TapEstadistica::vistas).reversed().thenComparing(TapEstadistica::tapId));
    String masVisto = taps.isEmpty() || taps.get(0).vistas() == 0 ? null : taps.get(0).tapId();
    return new Resumen(desde.toString(), hasta.toString(), dias, masVisto, taps);
  }

  private static Set<String> unir(Set<String> a, Set<String> b) {
    java.util.TreeSet<String> todos = new java.util.TreeSet<>(a);
    todos.addAll(b);
    return todos;
  }
}