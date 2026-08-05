package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.exceptions.InstitutionAlreadyExistsException;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionMetadataResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class CreateInstitutionUseCase {
  private final InstitutionGateway institutionGateway;

  public CreateInstitutionUseCase(InstitutionGateway institutionGateway) {
    this.institutionGateway = institutionGateway;
  }

  public Mono<InstitutionResponse> execute(InstitutionCommand command) {
    return institutionGateway.findById(command.id())
      .flatMap(existing -> Mono.<InstitutionResponse>error(
        new InstitutionAlreadyExistsException("Institution with ID " + command.id() + " already exists")
      ))
      .switchIfEmpty(Mono.defer(() -> {
        Institution institution = new Institution();
        institution.setId(command.id());
        institution.setName(command.name());
        institution.setType(command.type());
        institution.setStatus(command.status());
        institution.setUsersCount(0);
        institution.setCreatedAt(LocalDateTime.now());
        institution.setUpdatedAt(LocalDateTime.now());

        if (command.metadata() != null) {
          InstitutionMetadata metadata = new InstitutionMetadata();
          metadata.setInstitutionId(command.id());
          metadata.setDescription(command.metadata().description());
          metadata.setWebsite(command.metadata().website());
          metadata.setContactEmail(command.metadata().contactEmail());
          metadata.setPhoneNumber(command.metadata().phoneNumber());
          metadata.setAddress(command.metadata().address());
          metadata.setSubscriptionType(command.metadata().subscriptionType());
          metadata.setMaxUsers(command.metadata().maxUsers() != null ? command.metadata().maxUsers() : 100);
          metadata.setLastActivity(LocalDateTime.now());
          institution.setMetadata(metadata);
        }

        return institutionGateway.save(institution)
          .map(this::mapToResponse);
      }));
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
