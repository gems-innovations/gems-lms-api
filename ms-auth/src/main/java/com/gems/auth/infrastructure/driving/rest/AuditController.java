package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.AuditUseCase;
import com.gems.auth.domain.entities.AuditEvent;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.InternalApiKey;
import com.gems.shared.web.Paging;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
public class AuditController {
  private final AuditUseCase audit;
  private final InternalApiKey internalKey;

  public AuditController(AuditUseCase audit, InternalApiKey internalKey) {
    this.audit = audit;
    this.internalKey = internalKey;
  }

  @PostMapping("/internal/audit/events")
  public Mono<ResponseEntity<Void>> record(@RequestHeader("X-Audit-Key") String key,
                                           @RequestBody AuditRequest request) {
    internalKey.require(key);
    return CurrentUser.forPasswordChange().flatMap(caller -> audit.record(caller.userId(), caller.role(),
      caller.institutionId(), request.action(), request.method(), request.path(), request.status(),
      request.clientIp(), request.userAgent())).thenReturn(ResponseEntity.noContent().build());
  }

  @GetMapping("/api/v1/audit/events")
  public Mono<ResponseEntity<List<AuditEvent>>> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer limit) {
    return CurrentUser.require(c -> c.isSuperAdmin() || c.isAdmin(), "Only administrators see the audit trail")
      .flatMap(caller -> {
        int size = Paging.limit(limit);
        String scope = caller.isSuperAdmin() ? null : caller.institutionId();
        return audit.search(scope, search, action, from == null ? null : from.atStartOfDay(),
          to == null ? null : to.plusDays(1).atStartOfDay(), size, Paging.offset(page, size));
      }).flatMap(result -> Paging.ok(result.events(), result.total()));
  }

  public record AuditRequest(String action, String method, String path, int status,
                             String clientIp, String userAgent) {}
}
