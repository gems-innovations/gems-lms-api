package com.gems.admin.application;

import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class CreateBrandingUseCase {
  private final BrandingGateway brandingGateway;

  public CreateBrandingUseCase(BrandingGateway brandingGateway) {
    this.brandingGateway = brandingGateway;
  }

  public Mono<BrandingResponse> execute(BrandingCommand command) {
    Branding branding = new Branding();
    branding.setCompanyId(command.companyId());
    branding.setDomain(command.domain());
    branding.setLogoUrl(command.logoUrl());
    branding.setFaviconUrl(command.faviconUrl());
    branding.setPrimaryColor(command.primaryColor());
    branding.setSecondaryColor(command.secondaryColor());
    branding.setAccentColor(command.accentColor());
    branding.setTextColor(command.textColor());
    branding.setTheme(command.theme());
    branding.setLoginBackgroundUrl(command.loginBackgroundUrl());
    branding.setCustomCss(command.customCss());
    branding.setUpdatedAt(LocalDateTime.now());

    return brandingGateway.save(branding)
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
