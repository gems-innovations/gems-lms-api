package com.gems.admin.application;

import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.exceptions.BrandingNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateBrandingUseCase {
  private final BrandingGateway brandingGateway;

  public UpdateBrandingUseCase(BrandingGateway brandingGateway) {
    this.brandingGateway = brandingGateway;
  }

  public Mono<BrandingResponse> execute(String companyId, BrandingCommand command) {
    return brandingGateway.findByCompanyId(companyId)
      .switchIfEmpty(Mono.error(new BrandingNotFoundException("Branding not found for company: " + companyId)))
      .flatMap(existingBranding -> {
        existingBranding.setDomain(command.domain());
        existingBranding.setLogoUrl(command.logoUrl());
        existingBranding.setFaviconUrl(command.faviconUrl());
        existingBranding.setPrimaryColor(command.primaryColor());
        existingBranding.setSecondaryColor(command.secondaryColor());
        existingBranding.setAccentColor(command.accentColor());
        existingBranding.setTextColor(command.textColor());
        existingBranding.setTheme(command.theme());
        existingBranding.setLoginBackgroundUrl(command.loginBackgroundUrl());
        existingBranding.setCustomCss(command.customCss());
        existingBranding.setUpdatedAt(LocalDateTime.now());

        return brandingGateway.update(existingBranding);
      })
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
