package com.gems.admin.application;

import com.gems.admin.application.exceptions.BrandingNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Mono;

public class GetBrandingByCompanyIdUseCase {
  private final BrandingGateway brandingGateway;

  public GetBrandingByCompanyIdUseCase(BrandingGateway brandingGateway) {
    this.brandingGateway = brandingGateway;
  }

  public Mono<BrandingResponse> execute(String companyId) {
    return brandingGateway.findByCompanyId(companyId)
      .switchIfEmpty(Mono.error(new BrandingNotFoundException("Branding not found for company: " + companyId)))
      .map(this::mapToResponse);
  }

  private BrandingResponse mapToResponse(Branding branding) {
    return new BrandingResponse(
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
