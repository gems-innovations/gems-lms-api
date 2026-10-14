package com.gems.admin.application.gateway;

import com.gems.admin.application.home.HomeModels.HomeRecord;
import com.gems.admin.application.home.HomeModels.HomeVersion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HomeGateway {
  Mono<HomeRecord> find(String institutionId);

  Mono<HomeRecord> findBySlug(String slug);

  /** Inserta o actualiza el registro de la institución. */
  Mono<HomeRecord> save(HomeRecord record);

  Mono<Boolean> slugTaken(String slug, String exceptInstitutionId);

  Flux<HomeVersion> versions(String institutionId);

  Mono<HomeVersion> version(String institutionId, int version);

  Mono<HomeVersion> saveVersion(HomeVersion version);
}
