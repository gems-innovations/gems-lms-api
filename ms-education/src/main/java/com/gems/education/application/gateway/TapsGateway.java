package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistencia de Taps: dispositivos (jugadorId), usuarios con sesión y partidas. Los rankings se calculan por
 * agregación en la base de datos (suma de XP por identidad en un rango de fechas).
 *
 * <p>Cada partida guarda con qué identidad se jugó: el usuario si llegó con sesión válida, o el dispositivo si
 * llegó anónima. Así un dispositivo compartido no mezcla las partidas de varias personas: las anónimas suman al
 * dispositivo y las de cada usuario suman a ese usuario.
 */
public interface TapsGateway {

  /** Un dispositivo. {@code usuarioId} es el primer usuario que jugó con sesión en él (o null). */
  record Jugador(UUID jugadorId, Long usuarioId, String alias) {}

  /**
   * Con quién compite una partida: un usuario ({@code usuarioId} no nulo) o un dispositivo anónimo.
   * {@code jugadorId} es el dispositivo desde el que se juega.
   */
  record Identidad(UUID jugadorId, Long usuarioId, String alias) {
    public String clave() {
      return usuarioId != null ? "u" + usuarioId : "j" + jugadorId;
    }
  }

  /** Una fila del ranking: puesto, alias y XP de una identidad; {@code jugadores} es el total del ranking. */
  record Fila(int puesto, String alias, int xp, String clave, int jugadores) {}

  /** Dónde está una identidad: puesto, XP, cuántos compiten y cuántos tienen estrictamente menos XP. */
  record Posicion(int puesto, int xp, int jugadores, int menores, String alias) {}

  Mono<Jugador> buscarJugador(UUID jugadorId);

  /** Crea el dispositivo; si ya existía (envío simultáneo), devuelve el existente. */
  Mono<Jugador> crearJugador(Jugador jugador, Instant creado);

  /** Anota en el dispositivo su primer usuario, si aún no tiene. */
  Mono<Void> vincularDispositivo(UUID jugadorId, Long usuarioId);

  /** Alias del usuario, si ya jugó con sesión alguna vez. */
  Mono<String> aliasDeUsuario(Long usuarioId);

  /** Registra al usuario con ese alias; si ya existía (envío simultáneo), devuelve su alias. */
  Mono<String> crearUsuario(Long usuarioId, String alias, Instant creado);

  /**
   * Reserva, de forma atómica, el turno de una partida del dispositivo: falso si jugó hace menos de
   * {@code espera} o si ya agotó {@code maximoPorHora} partidas en la ventana de una hora en curso.
   */
  Mono<Boolean> reservarTurno(UUID jugadorId, Instant ahora, java.time.Duration espera, int maximoPorHora);

  /** Veces que esa identidad jugó ese Tap en el rango [desde, hasta). */
  Mono<Long> repeticiones(Identidad identidad, String tapId, Instant desde, Instant hasta);

  Mono<Void> guardarPartida(Identidad identidad, String tapId, String tema, int aciertos, int total, int xp,
                            long duracionMs, Instant creada);

  /** Las primeras {@code limite} identidades del rango, opcionalmente de un tema ({@code tema} null = todos). */
  Flux<Fila> top(Instant desde, Instant hasta, String tema, int limite);

  /** Posición de esa identidad en el rango; vacío si no jugó en él. */
  Mono<Posicion> posicion(Identidad identidad, Instant desde, Instant hasta, String tema);
}
