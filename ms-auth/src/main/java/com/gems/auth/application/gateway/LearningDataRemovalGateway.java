package com.gems.auth.application.gateway;

import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

public interface LearningDataRemovalGateway {
  Mono<Void> remove(UserId userId);
}
