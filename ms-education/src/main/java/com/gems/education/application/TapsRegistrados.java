package com.gems.education.application;

import com.gems.education.application.gateway.TapsCreadorGateway;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista blanca de Taps que cuentan para las estadísticas: el banco de GEMS ({@link TapsBanco}) más los Taps de creador
 * publicados con enlace ({@code taps_creador}). El ranking sigue mirando solo el banco (decisión del 26-oct: los Taps
 * {@code u-} no suman XP en el MVP).
 */
public interface TapsRegistrados {
  /** Tema con el que se agrupan las estadísticas de los Taps de creador. */
  String TEMA_CREADOR = "creador";

  /** El Tap si está registrado (banco o publicado); vacío si no. */
  Mono<TapsBanco.Tap> buscar(String tapId);

  /** Solo el banco (para pruebas y para quien no tiene base). */
  static TapsRegistrados deBanco(TapsBanco banco) {
    return id -> Mono.justOrEmpty(banco.buscar(id));
  }

  /** Banco ∪ publicados, con caché en memoria de 60 s por id (también de los negativos: ids inventados no golpean la base). */
  static TapsRegistrados conPublicados(TapsBanco banco, TapsCreadorGateway creador) {
    return new Compuesto(banco, creador, Clock.systemUTC(), Duration.ofSeconds(60));
  }

  final class Compuesto implements TapsRegistrados {
    private static final int MAX_CACHE = 10_000;
    private final TapsBanco banco;
    private final TapsCreadorGateway creador;
    private final Clock reloj;
    private final Duration vida;
    private final ConcurrentHashMap<String, Entrada> cache = new ConcurrentHashMap<>();

    private record Entrada(long vence, Optional<TapsBanco.Tap> tap) {}

    Compuesto(TapsBanco banco, TapsCreadorGateway creador, Clock reloj, Duration vida) {
      this.banco = banco;
      this.creador = creador;
      this.reloj = reloj;
      this.vida = vida;
    }

    @Override
    public Mono<TapsBanco.Tap> buscar(String tapId) {
      Optional<TapsBanco.Tap> delBanco = banco.buscar(tapId);
      if (delBanco.isPresent()) return Mono.just(delBanco.get());
      if (!TapsIds.esDeUsuario(tapId)) return Mono.empty();
      long ahora = reloj.millis();
      Entrada e = cache.get(tapId);
      if (e != null && e.vence() > ahora) return Mono.justOrEmpty(e.tap());
      return creador.buscarPublicado(tapId)
        .map(p -> Optional.of(new TapsBanco.Tap(p.id(), TEMA_CREADOR, TapsPayloadValidador.PREGUNTAS)))
        .defaultIfEmpty(Optional.empty())
        .doOnNext(tap -> {
          if (cache.size() >= MAX_CACHE) cache.entrySet().removeIf(x -> x.getValue().vence() <= ahora);
          if (cache.size() < MAX_CACHE) cache.put(tapId, new Entrada(ahora + vida.toMillis(), tap));
        })
        .flatMap(Mono::justOrEmpty);
    }
  }
}
