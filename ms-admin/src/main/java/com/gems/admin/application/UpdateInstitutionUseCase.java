package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionMetadataResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateInstitutionUseCase {
  private final InstitutionGateway institutionGateway;

  public UpdateInstitutionUseCase(InstitutionGateway institutionGateway) {
    this.institutionGateway = institutionGateway;
  }

  public Mono<InstitutionResponse> execute(String id, InstitutionCommand command) {
    return institutionGateway.findById(id)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found with ID " + id)))
      .flatMap(existing -> {
        existing.setName(command.name());
        existing.setType(command.type());
        existing.setStatus(command.status());
        existing.setUpdatedAt(LocalDateTime.now());

        if (command.metadata() != null) {
          InstitutionMetadata metadata = existing.getMetadata();
          if (metadata == null) {
            metadata = new InstitutionMetadata();
            metadata.setInstitutionId(id);
          }
          metadata.setDescription(command.metadata().description());
          metadata.setWebsite(command.metadata().website());
          metadata.setContactEmail(command.metadata().contactEmail());
          metadata.setPhoneNumber(command.metadata().phoneNumber());
          metadata.setAddress(command.metadata().address());
          metadata.setSubscriptionType(command.metadata().subscriptionType());
          metadata.setMaxUsers(command.metadata().maxUsers() != null ? command.metadata().maxUsers() : 100);
          metadata.setLastActivity(LocalDateTime.now());
          existing.setMetadata(metadata);
        }

        return institutionGateway.update(existing)
          .map(this::mapToResponse);
      });
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
