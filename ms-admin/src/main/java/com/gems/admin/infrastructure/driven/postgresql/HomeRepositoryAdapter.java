package com.gems.admin.infrastructure.driven.postgresql;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.gems.admin.application.gateway.HomeGateway;
import com.gems.admin.application.home.HomeModels.HomeRecord;
import com.gems.admin.application.home.HomeModels.HomeVersion;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class HomeRepositoryAdapter implements HomeGateway {
  private final R2dbcEntityTemplate template;
  private final IHomeVersionRepository versionRepository;

  public HomeRepositoryAdapter(R2dbcEntityTemplate template, IHomeVersionRepository versionRepository) {
    this.template = template;
    this.versionRepository = versionRepository;
  }

  @Override
  public Mono<HomeRecord> find(String institutionId) {
    return template.selectOne(query(where("institution_id").is(institutionId)), HomeEntity.class)
      .map(HomeRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<HomeRecord> findBySlug(String slug) {
    return template.selectOne(query(where("slug").is(slug)), HomeEntity.class).map(HomeRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<HomeRecord> save(HomeRecord record) {
    HomeEntity entity = new HomeEntity(record.institutionId(), record.slug(), record.draftMode(), record.draftHtml(), record.draftCss(),
      record.draftUpdatedAt(), record.draftUpdatedBy(), record.publishedVersion());
    return template.exists(query(where("institution_id").is(record.institutionId())), HomeEntity.class)
      .flatMap(exists -> exists ? template.update(entity) : template.insert(entity))
      .map(HomeRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Boolean> slugTaken(String slug, String exceptInstitutionId) {
    return template.exists(query(where("slug").is(slug).and("institution_id").not(exceptInstitutionId)),
      HomeEntity.class);
  }

  @Override
  public Flux<HomeVersion> versions(String institutionId) {
    return versionRepository.findByInstitutionId(institutionId).map(HomeRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<HomeVersion> version(String institutionId, int version) {
    return versionRepository.findByInstitutionIdAndVersion(institutionId, version).map(HomeRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<HomeVersion> saveVersion(HomeVersion version) {
    return versionRepository.save(new HomeVersionEntity(null, version.institutionId(), version.version(),
      version.mode(), version.html(), version.css(), version.note(), version.publishedBy(), version.publishedAt()))
      .map(HomeRepositoryAdapter::toDomain);
  }

  private static HomeRecord toDomain(HomeEntity e) {
    return new HomeRecord(e.institutionId(), e.slug(), e.draftMode(), e.draftHtml(), e.draftCss(), e.draftUpdatedAt(),
      e.draftUpdatedBy(), e.publishedVersion());
  }

  private static HomeVersion toDomain(HomeVersionEntity e) {
    return new HomeVersion(e.institutionId(), e.version(), e.mode(), e.html(), e.css(), e.note(), e.publishedBy(),
      e.publishedAt());
  }
}
