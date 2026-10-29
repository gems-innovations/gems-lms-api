package com.gems.education.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialesGateway;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cifras sociales de los Taps para las tarjetas de la web: «me gusta» anónimos y vistas.
 *
 * <ul>
 *   <li>Identidad: el mismo jugadorId (UUID del dispositivo) que usa el ranking, con su misma validación. No se guarda
 *       IP ni usuario. Un me gusta por jugador y Tap.</li>
 *   <li>Poner o quitar es idempotente: se envía el estado deseado, no se alterna.</li>
 *   <li>Solo Taps registrados ({@link TapsRegistrados}: banco ∪ publicados); un id desconocido responde 400 al dar
 *       me gusta y se ignora al leer.</li>
 *   <li>Máximo {@value #CAMBIOS_POR_MINUTO} cambios por jugador y minuto (429), contados en memoria.</li>
 *   <li>Lectura: hasta {@value #MAX_IDS} ids con forma válida (más responde 400); de ellos, solo los primeros
 *       {@value #MAX_DE_USUARIO} de creador (u-) se consultan.</li>
 *   <li>Vistas: suma de los últimos {@value #DIAS_VISTAS} días (hoy incluido, hora de Bogotá) de taps_estadistica.</li>
 *   <li>Son cifras orientativas: quien genere UUIDs nuevos puede sumar me gusta (el gateway limita por IP).</li>
 * </ul>
 */
public class TapsSocialesUseCase {
  static final int MAX_IDS = 60;
  static final int DIAS_VISTAS = 30;
  static final int CAMBIOS_POR_MINUTO = 60;
  static final long VENTANA_MS = 60_000;
  /** Jugadores vigilados a la vez por el límite (memoria acotada); si se llena, los nuevos pasan sin vigilar. */
  static final int MAX_VIGILADOS = 50_000;
  /** Ids de Taps de creador (u-) que se buscan por lectura; el resto se ignora (cada uno puede ir a la base). */
  static final int MAX_DE_USUARIO = 20;

  public record Cambio(String jugador, Boolean meGusta) {}

  public record Estado(String tapId, boolean meGusta, long total) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Social(String tapId, long vistas, long meGusta, Boolean meGustaYo) {}

  public record Sociales(List<Social> taps) {}

  /** Ventana de un minuto: cuándo empezó y cuántos cambios lleva (inmutable para leerla sin bloqueo). */
  private record Ventana(long inicio, int cambios) {}

  private final TapsSocialesGateway gateway;
  private final TapsRegistrados registrados;
  private final Clock clock;
  private final ConcurrentHashMap<UUID, Ventana> ventanas = new ConcurrentHashMap<>();
  private volatile long ultimaLimpieza;

  public TapsSocialesUseCase(TapsSocialesGateway gateway, TapsRegistrados registrados, Clock clock) {
    this.gateway = gateway;
    this.registrados = registrados;
    this.clock = clock;
  }

  /** Deja el me gusta del jugador en {@code c.meGusta()} y responde el total del Tap. */
  public Mono<Estado> cambiar(String tapId, Cambio c) {
    if (c == null || c.meGusta() == null) return Mono.error(new IllegalArgumentException("Falta meGusta"));
    UUID jugador;
    try {
      jugador = jugador(c.jugador());
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    if (jugador == null) return Mono.error(new IllegalArgumentException("jugador debe ser un UUID"));
    if (tapId == null || !TapsRankingUseCase.TAP_ID.matcher(tapId).matches()) {
      return Mono.error(new IllegalArgumentException("tapId no válido"));
    }
    if (!turno(jugador)) return Mono.error(new TapsLimiteException("Demasiados me gusta seguidos: espera un momento"));
    boolean poner = c.meGusta();
    return registrados.buscar(tapId)
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Tap desconocido")))
      .flatMap(tap -> (poner ? gateway.ponerMeGusta(tap.id(), jugador) : gateway.quitarMeGusta(tap.id(), jugador))
        .then(gateway.meGusta(List.of(tap.id())))
        .map(totales -> new Estado(tap.id(), poner, totales.getOrDefault(tap.id(), 0L))));
  }

  /** Vistas y me gusta de los Taps de {@code ids} (separados por coma), en el mismo orden; {@code jugador} es opcional. */
  public Mono<Sociales> leer(String ids, String jugador) {
    UUID yo;
    try {
      yo = jugador(jugador);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    List<String> pedidos = ids == null ? List.of() : Arrays.stream(ids.split(",", -1)).map(String::trim)
      .filter(id -> TapsRankingUseCase.TAP_ID.matcher(id).matches()).distinct().toList();
    if (pedidos.size() > MAX_IDS) return Mono.error(new IllegalArgumentException("Máximo " + MAX_IDS + " ids"));
    LocalDate hasta = LocalDate.now(clock), desde = hasta.minusDays(DIAS_VISTAS - 1L);
    // Los ids u- pueden ir a la base (si no están en la caché de TapsRegistrados): se miran como mucho MAX_DE_USUARIO.
    Set<String> deUsuario = pedidos.stream().filter(TapsIds::esDeUsuario).limit(MAX_DE_USUARIO)
      .collect(java.util.stream.Collectors.toSet());
    return Flux.fromIterable(pedidos)
      .filter(id -> !TapsIds.esDeUsuario(id) || deUsuario.contains(id))
      .concatMap(registrados::buscar)
      .map(TapsBanco.Tap::id)
      .collectList()
      .flatMap(validos -> Mono.zip(gateway.vistas(validos, desde, hasta), gateway.meGusta(validos),
          yo == null ? Mono.just(Set.<String>of()) : gateway.meGustaDe(yo, validos))
        .map(z -> new Sociales(validos.stream().map(id -> new Social(id, z.getT1().getOrDefault(id, 0L),
          z.getT2().getOrDefault(id, 0L), yo == null ? null : z.getT3().contains(id))).toList())));
  }

  /** El UUID del jugador con la validación del ranking; null si no viene. */
  private static UUID jugador(String texto) {
    if (texto == null || texto.isBlank()) return null;
    if (!TapsRankingUseCase.UUID_TEXTO.matcher(texto.trim()).matches()) throw new IllegalArgumentException("jugador debe ser un UUID");
    return UUID.fromString(texto.trim());
  }

  /** Cuenta un cambio del jugador; falso si ya hizo {@value #CAMBIOS_POR_MINUTO} en su minuto. */
  boolean turno(UUID jugador) {
    long ahora = clock.millis();
    if (ventanas.size() >= MAX_VIGILADOS && !ventanas.containsKey(jugador)) {
      if (ahora - ultimaLimpieza >= 1000) {
        ultimaLimpieza = ahora;
        ventanas.values().removeIf(v -> v.inicio() <= ahora - VENTANA_MS);
      }
      // Lleno de ventanas vivas (UUIDs inventados): se deja pasar sin vigilar; cerrar bloquearía a los jugadores nuevos.
      if (ventanas.size() >= MAX_VIGILADOS) return true;
    }
    Ventana v = ventanas.compute(jugador, (k, a) -> a == null || a.inicio() <= ahora - VENTANA_MS
      ? new Ventana(ahora, 1) : new Ventana(a.inicio(), Math.min(a.cambios() + 1, CAMBIOS_POR_MINUTO + 1)));
    return v.cambios() <= CAMBIOS_POR_MINUTO;
  }
}
