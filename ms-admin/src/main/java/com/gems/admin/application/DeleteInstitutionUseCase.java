package com.gems.admin.application;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import reactor.core.publisher.Mono;

public class DeleteInstitutionUseCase {
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public DeleteInstitutionUseCase(InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  public Mono<Void> execute(String id) {
    // Branding rows are keyed by company_id (unique), so they must go with the institution;
    // otherwise re-creating an institution with the same id fails on the unique index.
    return institutionGateway.findById(id)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found with ID " + id)))
      .flatMap(existing -> institutionGateway.deleteById(id)
        .then(brandingGateway.deleteByCompanyId(id)));
  }
}
