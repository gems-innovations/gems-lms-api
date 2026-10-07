package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Group;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GroupGateway {
  Mono<Group> save(Group group);
  Mono<Group> findById(Long id);
  Flux<Group> findAll();
  Flux<Group> findByInstitution(String institutionId);
  Mono<Void> deleteById(Long id);
}
