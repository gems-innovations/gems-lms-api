package com.gems.admin.application.gateway;

import com.gems.admin.domain.entities.Institution;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InstitutionGateway {
  Mono<Institution> save(Institution institution);

  Mono<Institution> findById(String id);

  Mono<Institution> update(Institution institution);

  Mono<Void> deleteById(String id);

  Flux<Institution> findAll();
}
