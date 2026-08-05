package com.gems.admin.application;

import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionMetadataResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Institution;
import reactor.core.publisher.Flux;

public class GetAllInstitutionsUseCase {
  private final InstitutionGateway institutionGateway;

  public GetAllInstitutionsUseCase(InstitutionGateway institutionGateway) {
    this.institutionGateway = institutionGateway;
  }

  public Flux<InstitutionResponse> execute() {
    return institutionGateway.findAll()
      .map(this::mapToResponse);
  }

  private InstitutionResponse mapToResponse(Institution institution) {
    InstitutionMetadataResponse metadataResponse = null;
    if (institution.getMetadata() != null) {
      metadataResponse = new InstitutionMetadataResponse(
        institution.getMetadata().getInstitutionId(),
        institution.getMetadata().getDescription(),
        institution.getMetadata().getWebsite(),
        institution.getMetadata().getContactEmail(),
        institution.getMetadata().getPhoneNumber(),
        institution.getMetadata().getAddress(),
        institution.getMetadata().getSubscriptionType(),
        institution.getMetadata().getMaxUsers(),
        institution.getMetadata().getLastActivity()
      );
    }
    return new InstitutionResponse(
      institution.getId(),
      institution.getName(),
      institution.getType(),
      institution.getStatus(),
      institution.getUsersCount(),
      institution.getCreatedAt(),
      institution.getUpdatedAt(),
      metadataResponse
    );
  }
}
