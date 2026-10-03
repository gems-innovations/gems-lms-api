package com.gems.auth.domain.entities;

import java.time.LocalDateTime;

public record AuditEvent(Long id, Long actorUserId, String actorRole, String institutionId,
                         String action, String httpMethod, String resourcePath, int responseStatus,
                         String clientIp, String userAgent, LocalDateTime occurredAt) {
}
