package com.gems.admin.infrastructure.driven.postgresql;

import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.domain.entities.Branding;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class BrandingRepositoryAdapter implements BrandingGateway {
  private final IBrandingRepository brandingRepository;

  public BrandingRepositoryAdapter(IBrandingRepository brandingRepository) {
    this.brandingRepository = brandingRepository;
  }

  @Override
  public Mono<Branding> save(Branding branding) {
    BrandingEntity entity = mapToEntity(branding);
    return brandingRepository.save(entity)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Branding> findByCompanyId(String companyId) {
    return brandingRepository.findByCompanyId(companyId)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Branding> update(Branding branding) {
    BrandingEntity entity = mapToEntity(branding);
    return brandingRepository.save(entity)
      .map(this::mapToDomain);
  }

  private Branding mapToDomain(BrandingEntity entity) {
    return new Branding(
      entity.getBrandingId(),
      entity.getCompanyId(),
      entity.getDomain(),
      entity.getLogoUrl(),
      entity.getFaviconUrl(),
      entity.getPrimaryColor(),
      entity.getSecondaryColor(),
      entity.getAccentColor(),
      entity.getTextColor(),
      entity.getTheme(),
      entity.getLoginBackgroundUrl(),
      entity.getCustomCss(),
      entity.getUpdatedAt()
    );
  }

  private BrandingEntity mapToEntity(Branding branding) {
    return new BrandingEntity(
      branding.getBrandingId(),
      branding.getCompanyId(),
      branding.getDomain(),
      branding.getLogoUrl(),
      branding.getFaviconUrl(),
      branding.getPrimaryColor(),
      branding.getSecondaryColor(),
      branding.getAccentColor(),
      branding.getTextColor(),
      branding.getTheme(),
      branding.getLoginBackgroundUrl(),
      branding.getCustomCss(),
      branding.getUpdatedAt()
    );
  }
}
