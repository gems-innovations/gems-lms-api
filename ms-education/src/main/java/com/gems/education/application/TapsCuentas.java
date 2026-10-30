package com.gems.education.application;

import com.gems.education.application.exceptions.TapsCuentaRequeridaException;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.shared.security.AuthenticatedUser;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Me gusta y comentarios de Taps son para cuentas registradas (decisión del dueño, 29-oct): un invitado recibe 403
 * {@value TapsCuentaRequeridaException#CODE}.
 *
 * <p>El token no dice si la cuenta es de invitado, así que se mira el correo en ms-auth: los invitados son STUDENT con
 * correo {@value #DOMINIO_INVITADO} (GuestAccessUseCase). Otros roles no se consultan. La respuesta se guarda
 * {@value #VIDA_MS} ms por usuario: quien reclama su cuenta puede esperar hasta un minuto. Si ms-auth falla, se responde
 * su error (503), nunca se deja pasar.
 */
public class TapsCuentas {
  static final String DOMINIO_INVITADO = "@invitado.gems.lat";
  static final long VIDA_MS = 60_000;
  static final int MAX_CACHE = 50_000;

  private record Entrada(long vence, boolean invitado) {}

  private final UserDirectory users;
  private final Clock clock;
  private final ConcurrentHashMap<Long, Entrada> cache = new ConcurrentHashMap<>();

  public TapsCuentas(UserDirectory users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  static boolean esInvitado(String email) {
    return email != null && email.strip().toLowerCase(Locale.ROOT).endsWith(DOMINIO_INVITADO);
  }

  /** El usuario si su cuenta está registrada; 403 {@link TapsCuentaRequeridaException} si es de invitado. */
  public Mono<AuthenticatedUser> registrada(AuthenticatedUser u) {
    if (!u.isStudent()) return Mono.just(u);
    long ahora = clock.millis();
    Entrada e = cache.get(u.userId());
    Mono<Boolean> invitado = e != null && e.vence() > ahora ? Mono.just(e.invitado())
      : users.find(u.userId()).map(p -> esInvitado(p.email()))
        .switchIfEmpty(Mono.error(() -> new IllegalStateException("Sin perfil para el usuario")))
        .doOnNext(inv -> {
          if (cache.size() >= MAX_CACHE) cache.values().removeIf(x -> x.vence() <= ahora);
          if (cache.size() >= MAX_CACHE) cache.clear();
          cache.put(u.userId(), new Entrada(ahora + VIDA_MS, inv));
        });
    return invitado.flatMap(inv -> inv ? Mono.error(new TapsCuentaRequeridaException()) : Mono.just(u));
  }
}
