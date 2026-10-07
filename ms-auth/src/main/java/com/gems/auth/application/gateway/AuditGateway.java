package com.gems.auth.application.gateway;

import com.gems.auth.domain.entities.AuditEvent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditGateway {
  Mono<AuditEvent> save(AuditEvent event);
  Mono<Page> search(String institutionId, String search, String action, LocalDateTime from,
                    LocalDateTime to, int limit, long offset);

  record Page(List<AuditEvent> events, long total) {}
}
