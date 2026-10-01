package com.gems.admin.application;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Mono;

public class GetInstitutionByIdUseCase {
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public GetInstitutionByIdUseCase(InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  public Mono<InstitutionResponse> execute(String id) {
    return institutionGateway.findById(id)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found with ID " + id)))
      .flatMap(institution -> brandingGateway.findByCompanyId(id)
        .map(branding -> InstitutionResponseMapper.toResponse(institution, branding))
        .defaultIfEmpty(InstitutionResponseMapper.toResponse(institution, (Branding) null)));
  }
}
