package com.gems.auth.application;

import com.gems.auth.application.gateway.AuditGateway;
import com.gems.auth.domain.entities.AuditEvent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class AuditUseCase {
  private final AuditGateway gateway;

  public AuditUseCase(AuditGateway gateway) {
    this.gateway = gateway;
  }

  public Mono<AuditEvent> record(Long actorUserId, String actorRole, String institutionId,
                                 String action, String method, String path, int status,
                                 String clientIp, String userAgent) {
    return gateway.save(new AuditEvent(null, actorUserId, actorRole, institutionId,
      action, method, path, status, trim(clientIp, 100), trim(userAgent, 500), LocalDateTime.now()));
  }

  public Mono<AuditGateway.Page> search(String institutionId, String search, String action,
                                        LocalDateTime from, LocalDateTime to, int limit, long offset) {
    return gateway.search(institutionId, search, action, from, to, limit, offset);
  }

  private static String trim(String value, int max) {
    if (value == null || value.isBlank()) return null;
    return value.length() <= max ? value : value.substring(0, max);
  }
}
