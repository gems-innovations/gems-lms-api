package com.gems.admin.infrastructure.driven.postgresql;

import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InstitutionRepositoryAdapter implements InstitutionGateway {
  private final IInstitutionRepository institutionRepository;
  private final IInstitutionMetadataRepository metadataRepository;

  public InstitutionRepositoryAdapter(IInstitutionRepository institutionRepository,
                                      IInstitutionMetadataRepository metadataRepository) {
    this.institutionRepository = institutionRepository;
    this.metadataRepository = metadataRepository;
  }

  @Override
  public Mono<Institution> save(Institution institution) {
    InstitutionEntity entity = mapToEntity(institution);
    entity.setNew(true);

    return institutionRepository.save(entity)
      .flatMap(savedEntity -> {
        if (institution.getMetadata() != null) {
          InstitutionMetadataEntity metadataEntity = mapToMetadataEntity(institution.getMetadata());
          metadataEntity.setInstitutionId(savedEntity.getId());
          metadataEntity.setNew(true);
          return metadataRepository.save(metadataEntity)
            .map(savedMeta -> mapToDomain(savedEntity, savedMeta));
        }
        return Mono.just(mapToDomain(savedEntity, null));
      });
  }

  @Override
  public Mono<Institution> findById(String id) {
    return institutionRepository.findById(id)
      .flatMap(entity -> {
        entity.setNew(false);
        return metadataRepository.findById(id)
          .map(meta -> {
            meta.setNew(false);
            return mapToDomain(entity, meta);
          })
          .defaultIfEmpty(mapToDomain(entity, null));
      });
  }

  @Override
  public Mono<Institution> update(Institution institution) {
    InstitutionEntity entity = mapToEntity(institution);
    entity.setNew(false);

    return institutionRepository.save(entity)
      .flatMap(savedEntity -> {
        if (institution.getMetadata() != null) {
          InstitutionMetadataEntity metadataEntity = mapToMetadataEntity(institution.getMetadata());
          metadataEntity.setInstitutionId(savedEntity.getId());
          
          // Check if metadata exists to determine if we should update or insert
          return metadataRepository.findById(savedEntity.getId())
            .flatMap(existingMeta -> {
              metadataEntity.setNew(false);
              return metadataRepository.save(metadataEntity);
            })
            .switchIfEmpty(Mono.defer(() -> {
              metadataEntity.setNew(true);
              return metadataRepository.save(metadataEntity);
            }))
            .map(savedMeta -> mapToDomain(savedEntity, savedMeta));
        }
        return Mono.just(mapToDomain(savedEntity, null));
      });
  }

  @Override
  public Mono<Void> deleteById(String id) {
    return metadataRepository.deleteById(id)
      .then(institutionRepository.deleteById(id));
  }

  @Override
  public Flux<Institution> findAll() {
    return institutionRepository.findAll()
      .flatMap(entity -> {
        entity.setNew(false);
        return metadataRepository.findById(entity.getId())
          .map(meta -> {
            meta.setNew(false);
            return mapToDomain(entity, meta);
          })
          .defaultIfEmpty(mapToDomain(entity, null));
      });
  }

  private Institution mapToDomain(InstitutionEntity entity, InstitutionMetadataEntity metadataEntity) {
    InstitutionMetadata metadata = null;
    if (metadataEntity != null) {
      metadata = new InstitutionMetadata(
        metadataEntity.getInstitutionId(),
        metadataEntity.getDescription(),
        metadataEntity.getWebsite(),
        metadataEntity.getContactEmail(),
        metadataEntity.getPhoneNumber(),
        metadataEntity.getAddress(),
        metadataEntity.getSubscriptionType(),
        metadataEntity.getMaxUsers(),
        metadataEntity.getLastActivity()
      );
    }
    return new Institution(
      entity.getId(),
      entity.getName(),
      entity.getType(),
      entity.getStatus(),
      entity.getUsersCount(),
      entity.getCreatedAt(),
      entity.getUpdatedAt(),
      metadata
    );
  }

  private InstitutionEntity mapToEntity(Institution institution) {
    return new InstitutionEntity(
      institution.getId(),
      institution.getName(),
      institution.getType(),
      institution.getStatus(),
      institution.getUsersCount(),
      institution.getCreatedAt(),
      institution.getUpdatedAt()
    );
  }

  private InstitutionMetadataEntity mapToMetadataEntity(InstitutionMetadata metadata) {
    return new InstitutionMetadataEntity(
      metadata.getInstitutionId(),
      metadata.getDescription(),
      metadata.getWebsite(),
      metadata.getContactEmail(),
      metadata.getPhoneNumber(),
      metadata.getAddress(),
      metadata.getSubscriptionType(),
      metadata.getMaxUsers(),
      metadata.getLastActivity()
    );
  }
}
