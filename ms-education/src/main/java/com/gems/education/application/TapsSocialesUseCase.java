package com.gems.education.application;

import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialesGateway;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cifras sociales de las tarjetas de Taps y el «me gusta» (decisión del dueño, 29-oct: me gusta y comentarios solo con
 * sesión; las cifras son públicas).
 *
 * <ul>
 *   <li>Público: vistas (últimos {@value #DIAS_VISTAS} días, hoy incluido, hora de Bogotá), total de me gusta y de
 *       comentarios visibles. Sin datos de nadie.</li>
 *   <li>Con sesión: el me gusta es del userId del token, uno por usuario y Tap. Poner o quitar es idempotente (se envía
 *       el estado deseado). Máximo {@value #CAMBIOS_POR_MINUTO} cambios por usuario y minuto (429), en memoria.</li>
 *   <li>Solo Taps registrados ({@link TapsRegistrados}: banco ∪ publicados): un id desconocido responde 400 al dar me
 *       gusta y se ignora al leer.</li>
 *   <li>Lectura: hasta {@value #MAX_IDS} ids con forma válida (más responde 400); de ellos, solo los primeros
 *       {@value #MAX_DE_USUARIO} de creador (u-) se consultan.</li>
 * </ul>
 */
public class TapsSocialesUseCase {
  static final int MAX_IDS = 60;
  static final int DIAS_VISTAS = 30;
  static final int CAMBIOS_POR_MINUTO = 60;
  /** Ids de Taps de creador (u-) que se buscan por lectura; el resto se ignora (cada uno puede ir a la base). */
  static final int MAX_DE_USUARIO = 20;

  public record Estado(String tapId, boolean meGusta, long total) {}

  public record Social(String tapId, long vistas, long meGusta, long comentarios) {}

  public record Sociales(List<Social> taps) {}

  public record MeGustaYo(String tapId, boolean meGustaYo) {}

  public record Mios(List<MeGustaYo> taps) {}

  private final TapsSocialesGateway gateway;
  private final TapsRegistrados registrados;
  private final Clock clock;
  private final LimitePorMinuto limite;

  public TapsSocialesUseCase(TapsSocialesGateway gateway, TapsRegistrados registrados, Clock clock) {
    this.gateway = gateway;
    this.registrados = registrados;
    this.clock = clock;
    this.limite = new LimitePorMinuto(CAMBIOS_POR_MINUTO, clock);
  }

  /** Vistas, me gusta y comentarios de los Taps de {@code ids} (separados por coma), en el mismo orden. */
  public Mono<Sociales> leer(String ids) {
    LocalDate hasta = LocalDate.now(clock), desde = hasta.minusDays(DIAS_VISTAS - 1L);
    return validos(ids).flatMap(v -> Mono.zip(gateway.vistas(v, desde, hasta), gateway.meGusta(v), gateway.comentarios(v))
      .map(z -> new Sociales(v.stream().map(id -> new Social(id, z.getT1().getOrDefault(id, 0L),
        z.getT2().getOrDefault(id, 0L), z.getT3().getOrDefault(id, 0L))).toList())));
  }

  /** De los Taps de {@code ids}, a cuáles les dio me gusta el usuario. */
  public Mono<Mios> mios(Long userId, String ids) {
    return validos(ids).flatMap(v -> gateway.meGustaDe(userId, v)
      .map(mios -> new Mios(v.stream().map(id -> new MeGustaYo(id, mios.contains(id))).toList())));
  }

  /** Deja el me gusta del usuario en {@code meGusta} y responde el total del Tap. */
  public Mono<Estado> cambiar(Long userId, String tapId, Boolean meGusta) {
    if (userId == null) return Mono.error(new IllegalArgumentException("Falta el usuario"));
    if (meGusta == null) return Mono.error(new IllegalArgumentException("Falta meGusta"));
    if (tapId == null || !TapsRankingUseCase.TAP_ID.matcher(tapId).matches()) {
      return Mono.error(new IllegalArgumentException("tapId no válido"));
    }
    if (!limite.turno(userId)) return Mono.error(new TapsLimiteException("Demasiados me gusta seguidos: espera un momento"));
    return registrados.buscar(tapId)
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Tap desconocido")))
      .flatMap(tap -> (meGusta ? gateway.ponerMeGusta(tap.id(), userId) : gateway.quitarMeGusta(tap.id(), userId))
        .then(gateway.meGusta(List.of(tap.id())))
        .map(totales -> new Estado(tap.id(), meGusta, totales.getOrDefault(tap.id(), 0L))));
  }

  /** Los ids registrados de la lista, sin repetir y en orden; 400 si hay más de {@value #MAX_IDS} con forma válida. */
  private Mono<List<String>> validos(String ids) {
    List<String> pedidos = ids == null ? List.of() : Arrays.stream(ids.split(",", -1)).map(String::trim)
      .filter(id -> TapsRankingUseCase.TAP_ID.matcher(id).matches()).distinct().toList();
    if (pedidos.size() > MAX_IDS) return Mono.error(new IllegalArgumentException("Máximo " + MAX_IDS + " ids"));
    Set<String> deUsuario = pedidos.stream().filter(TapsIds::esDeUsuario).limit(MAX_DE_USUARIO).collect(Collectors.toSet());
    return Flux.fromIterable(pedidos)
      .filter(id -> !TapsIds.esDeUsuario(id) || deUsuario.contains(id))
      .concatMap(registrados::buscar)
      .map(TapsBanco.Tap::id)
      .collectList();
  }
}
