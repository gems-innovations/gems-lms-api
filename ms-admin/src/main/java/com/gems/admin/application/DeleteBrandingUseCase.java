package com.gems.admin.application;

import com.gems.admin.application.exceptions.BrandingNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import reactor.core.publisher.Mono;

public class DeleteBrandingUseCase {
  private final BrandingGateway brandingGateway;

  public DeleteBrandingUseCase(BrandingGateway brandingGateway) {
    this.brandingGateway = brandingGateway;
  }

  public Mono<Void> execute(String companyId) {
    return brandingGateway.findByCompanyId(companyId)
      .switchIfEmpty(Mono.error(new BrandingNotFoundException("Branding not found for company ID: " + companyId)))
      .flatMap(branding -> brandingGateway.deleteByCompanyId(companyId));
  }
}
