package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
  Mono<Notification> save(Notification notification);

  /** Notifications addressed to the user, plus those to their institution's staff when {@code staff}. */
  Flux<Notification> findFor(Long userId, String institutionId, boolean staff, int limit);

  Mono<Notification> findById(Long id);

  Mono<Void> markRead(Long notificationId, Long userId);

  Mono<Void> markAllRead(Long userId, String institutionId, boolean staff);
}
