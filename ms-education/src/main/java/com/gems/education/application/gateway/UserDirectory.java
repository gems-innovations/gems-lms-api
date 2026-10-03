package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

public interface UserDirectory {
  Mono<UserProfile> find(Long userId);

  record UserProfile(Long id, String fullName, String institutionId) {}
}
