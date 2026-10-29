package com.gems.education.application;

import com.gems.education.application.exceptions.TapsCuentaRequeridaException;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TapsCuentasTest {
  private static final AuthenticatedUser ALUMNO = new AuthenticatedUser(5L, "STUDENT", "inst-1");
  private final UserDirectory users = mock(UserDirectory.class);
  private final Instant[] ahora = {Instant.parse("2026-10-29T15:00:00Z")};
  private final TapsCuentas cuentas = new TapsCuentas(users, new Clock() {
    public ZoneId getZone() { return ZoneOffset.UTC; }
    public Clock withZone(ZoneId z) { return this; }
    public Instant instant() { return ahora[0]; }
  });

  private static UserDirectory.UserProfile perfil(String email) {
    return new UserDirectory.UserProfile(5L, "Ana Ruiz", "inst-1", "Ana", "Ruiz", email);
  }

  @Test
  void reconoceElDominioDeInvitadoSinMayusculas() {
    assertTrue(TapsCuentas.esInvitado("invitado-abc@invitado.gems.lat"));
    assertTrue(TapsCuentas.esInvitado(" Invitado-ABC@INVITADO.GEMS.LAT "));
    assertFalse(TapsCuentas.esInvitado("ana@correo.com"));
    assertFalse(TapsCuentas.esInvitado("ana@invitado.gems.lat.otro.com"));
    assertFalse(TapsCuentas.esInvitado(null));
  }

  @Test
  void elInvitadoRecibe403YElRegistradoPasa() {
    when(users.find(5L)).thenReturn(Mono.just(perfil("invitado-abc@invitado.gems.lat")));
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectError(TapsCuentaRequeridaException.class).verify();
    AuthenticatedUser otro = new AuthenticatedUser(6L, "STUDENT", "inst-1");
    when(users.find(6L)).thenReturn(Mono.just(perfil("luis@correo.com")));
    StepVerifier.create(cuentas.registrada(otro)).expectNext(otro).verifyComplete();
  }

  @Test
  void otrosRolesNoSeConsultan() {
    AuthenticatedUser creador = new AuthenticatedUser(8L, "CREATOR", null);
    StepVerifier.create(cuentas.registrada(creador)).expectNext(creador).verifyComplete();
    verifyNoInteractions(users);
  }

  @Test
  void laRespuestaSeGuardaUnMinutoYAlReclamarLaCuentaPasa() {
    when(users.find(5L)).thenReturn(Mono.just(perfil("invitado-abc@invitado.gems.lat")));
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectError(TapsCuentaRequeridaException.class).verify();
    when(users.find(5L)).thenReturn(Mono.just(perfil("ana@correo.com")));   // reclamó la cuenta
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectError(TapsCuentaRequeridaException.class).verify();
    verify(users, times(1)).find(5L);
    ahora[0] = ahora[0].plusSeconds(61);
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectNext(ALUMNO).verifyComplete();
  }

  @Test
  void siMsAuthFallaNoDejaPasar() {
    when(users.find(5L)).thenReturn(Mono.error(new IllegalStateException("503")));
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectError(IllegalStateException.class).verify();
    when(users.find(5L)).thenReturn(Mono.empty());
    StepVerifier.create(cuentas.registrada(ALUMNO)).expectError(IllegalStateException.class).verify();
  }
}
