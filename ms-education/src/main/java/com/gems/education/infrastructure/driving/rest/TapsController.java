package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsRankingUseCase;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.JwtReactiveAuthenticationManager;
import com.gems.shared.security.JwtServerAuthenticationConverter;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Rankings de Taps, públicos (sin sesión obligatoria): /api/v1/public/** ya es público en el gateway y en este
 * servicio. Si la petición trae un Bearer válido la partida se asocia también al usuario; si no trae o no es
 * válido, se juega como anónimo (nunca responde 401). En el ranking, con sesión «yo» es el usuario; sin sesión,
 * las partidas anónimas del jugadorId. No se guarda la IP.
 */
@RestController
public class TapsController {
  private final TapsRankingUseCase taps;
  private final JwtReactiveAuthenticationManager jwt;
  private static final JwtServerAuthenticationConverter BEARER = new JwtServerAuthenticationConverter();

  public TapsController(TapsRankingUseCase taps, JwtReactiveAuthenticationManager jwt) {
    this.taps = taps;
    this.jwt = jwt;
  }

  @PostMapping("/api/v1/public/taps/partidas")
  public Mono<ResponseEntity<TapsRankingUseCase.Resultado>> registrar(@RequestBody TapsRankingUseCase.Partida partida,
                                                                      ServerWebExchange exchange) {
    return usuario(exchange, jwt)
      .flatMap(u -> taps.registrar(partida, u.orElse(null)))
      .map(r -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r));
  }

  @GetMapping("/api/v1/public/taps/ranking")
  public Mono<ResponseEntity<TapsRankingUseCase.Ranking>> ranking(@RequestParam(required = false) String periodo,
                                                                  @RequestParam(required = false) String tema,
                                                                  @RequestParam(required = false) String jugadorId,
                                                                  @RequestParam(required = false) Integer limite,
                                                                  ServerWebExchange exchange) {
    return usuario(exchange, jwt)
      .flatMap(u -> taps.ranking(periodo, tema, jugadorId, limite, u.orElse(null)))
      .map(r -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r));
  }

  /** El usuario del Bearer, si llega uno válido; vacío si no hay sesión o el token no sirve. */
  static Mono<Optional<Long>> usuario(ServerWebExchange exchange, JwtReactiveAuthenticationManager jwt) {
    return BEARER.convert(exchange)
      .flatMap(jwt::authenticate)
      .map(auth -> auth.getPrincipal())
      .filter(AuthenticatedUser.class::isInstance)
      .map(p -> Optional.ofNullable(((AuthenticatedUser) p).userId()))
      .onErrorResume(e -> Mono.empty())
      .defaultIfEmpty(Optional.empty());
  }
}
