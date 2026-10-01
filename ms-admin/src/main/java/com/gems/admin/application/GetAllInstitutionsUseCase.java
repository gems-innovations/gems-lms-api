package com.gems.admin.application;

import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionListResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GetAllInstitutionsUseCase {
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public GetAllInstitutionsUseCase(InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  /** Legacy unpaginated listing, kept for callers that need every institution at once. */
  public Flux<InstitutionResponse> execute() {
    return institutionGateway.findAll()
      .flatMap(institution -> brandingGateway.findByCompanyId(institution.getId())
        .map(branding -> InstitutionResponseMapper.toResponse(institution, branding))
        .defaultIfEmpty(InstitutionResponseMapper.toResponse(institution, (Branding) null)));
  }

  public Mono<InstitutionListResponse> execute(String search, String status, int page, int limit) {
    int safePage = Math.max(page, 1);
    int safeLimit = Math.max(limit, 1);
    int offset = (safePage - 1) * safeLimit;

    Flux<InstitutionResponse> institutions = institutionGateway.findPage(search, status, offset, safeLimit)
      .flatMap(institution -> brandingGateway.findByCompanyId(institution.getId())
        .map(branding -> InstitutionResponseMapper.toResponse(institution, branding))
        .defaultIfEmpty(InstitutionResponseMapper.toResponse(institution, (Branding) null)));

    return institutions.collectList()
      .zipWith(institutionGateway.count(search, status))
      .map(tuple -> {
        var list = tuple.getT1();
        long total = tuple.getT2();
        int totalPages = (int) Math.ceil((double) total / safeLimit);
        return new InstitutionListResponse(
          list, total, safePage, safeLimit, totalPages,
          safePage < totalPages, safePage > 1
        );
      });
  }
}
