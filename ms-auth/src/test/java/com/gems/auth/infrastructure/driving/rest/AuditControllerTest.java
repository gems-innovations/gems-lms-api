package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.AuditUseCase;
import com.gems.auth.application.gateway.AuditGateway;
import com.gems.auth.domain.entities.AuditEvent;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.InternalApiKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditControllerTest {
  private final AuditUseCase audit = mock(AuditUseCase.class);

  private WebTestClient client(AuthenticatedUser caller) {
    return WebTestClient.bindToController(new AuditController(audit, new InternalApiKey("internal-secret", "jwt-secret")))
      .webFilter(TestSecurity.authenticatedAs(caller))
      .controllerAdvice(new GlobalExceptionHandler(), new com.gems.shared.security.SecurityExceptionAdvice())
      .build();
  }

  @Test
  void anAdminOnlyReadsTheirInstitutionAndGetsTheTotal() {
    var admin = new AuthenticatedUser(7L, "ADMIN", "inst-1");
    var event = new AuditEvent(1L, 7L, "ADMIN", "inst-1", "UPDATE", "PUT", "/api/v1/courses/3",
      200, "127.0.0.1", null, LocalDateTime.now());
    when(audit.search(eq("inst-1"), eq("courses"), eq("UPDATE"), any(), isNull(), eq(20), eq(20L)))
      .thenReturn(Mono.just(new AuditGateway.Page(List.of(event), 25)));

    client(admin).get().uri("/api/v1/audit/events?page=2&search=courses&action=UPDATE&from=2026-10-01")
      .exchange().expectStatus().isOk().expectHeader().valueEquals("X-Total-Count", "25")
      .expectBody().jsonPath("$[0].institutionId").isEqualTo("inst-1");
  }

  @Test
  void studentsCannotReadTheTrail() {
    client(new AuthenticatedUser(5L, "STUDENT", "inst-1")).get().uri("/api/v1/audit/events")
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(audit);
  }

  @Test
  void gatewaySignatureIsRequiredAndTheActorComesFromTheToken() {
    var admin = new AuthenticatedUser(7L, "ADMIN", "inst-1");
    when(audit.record(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(),
      anyInt(), any(), any())).thenReturn(Mono.just(mock(AuditEvent.class)));
    var body = new AuditController.AuditRequest("DELETE", "DELETE", "/api/v1/users/8", 204,
      "127.0.0.1", "browser");

    client(admin).post().uri("/internal/audit/events").header("X-Audit-Key", "wrong")
      .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange().expectStatus().isForbidden();
    client(admin).post().uri("/internal/audit/events").header("X-Audit-Key", "internal-secret")
      .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange().expectStatus().isNoContent();
    verify(audit).record(eq(7L), eq("ADMIN"), eq("inst-1"), eq("DELETE"), eq("DELETE"),
      eq("/api/v1/users/8"), eq(204), eq("127.0.0.1"), eq("browser"));
  }
}
