package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Announcement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AnnouncementGateway {
  /** Pinned first, then newest first. */
  Flux<Announcement> findByCourse(Long courseId);

  Mono<Announcement> findById(Long id);

  Mono<Announcement> save(Announcement announcement);

  Mono<Void> delete(Long id);
}
