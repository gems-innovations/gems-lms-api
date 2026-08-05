package com.gems.admin.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface IInstitutionMetadataRepository extends ReactiveCrudRepository<InstitutionMetadataEntity, String> {
}
