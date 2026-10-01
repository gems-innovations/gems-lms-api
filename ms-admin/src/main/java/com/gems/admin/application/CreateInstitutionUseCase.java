package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.exceptions.InstitutionAlreadyExistsException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Branding;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class CreateInstitutionUseCase {
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public CreateInstitutionUseCase(InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  public Mono<InstitutionResponse> execute(InstitutionCommand command) {
    return resolveId(command.id(), command.name())
      .flatMap(id -> institutionGateway.findById(id)
        .flatMap(existing -> Mono.<String>error(
          new InstitutionAlreadyExistsException("Institution with ID " + id + " already exists")
        ))
        .switchIfEmpty(Mono.just(id))
      )
      .flatMap(id -> {
        Institution institution = new Institution();
        institution.setId(id);
        institution.setName(command.name());
        institution.setType(command.type());
        institution.setStatus(command.status() != null && !command.status().isBlank() ? command.status() : "pending");
        institution.setUsersCount(0);
        institution.setCreatedAt(LocalDateTime.now());
        institution.setUpdatedAt(LocalDateTime.now());

        if (command.metadata() != null) {
          InstitutionMetadata metadata = new InstitutionMetadata();
          metadata.setInstitutionId(id);
          metadata.setDescription(command.metadata().description());
          metadata.setWebsite(command.metadata().website());
          metadata.setContactEmail(command.metadata().contactEmail());
          metadata.setPhoneNumber(command.metadata().phoneNumber());
          metadata.setAddress(command.metadata().address());
          metadata.setSubscriptionType(
            command.metadata().subscriptionType() != null && !command.metadata().subscriptionType().isBlank()
              ? command.metadata().subscriptionType() : "basic");
          metadata.setMaxUsers(command.metadata().maxUsers() != null ? command.metadata().maxUsers() : 100);
          metadata.setLastActivity(LocalDateTime.now());
          institution.setMetadata(metadata);
        }

        Branding branding = new Branding();
        branding.setCompanyId(id);
        if (command.branding() != null) {
          branding.setType(command.branding().type());
          branding.setLogoUrl(command.branding().logoUrl());
          branding.setIconUrl(command.branding().iconUrl());
          branding.setPrimaryColor(command.branding().colorPrimary());
          branding.setSecondaryColor(command.branding().colorSecondary());
          branding.setBackgroundColor(command.branding().backgroundColor());
          branding.setDarkMode(command.branding().darkMode() != null ? command.branding().darkMode() : false);
        } else {
          branding.setType("color-badge");
          branding.setPrimaryColor("#6C63FF");
          branding.setSecondaryColor("#1E1B4B");
          branding.setDarkMode(false);
        }
        branding.setUpdatedAt(LocalDateTime.now());

        return institutionGateway.save(institution)
          .flatMap(savedInstitution -> brandingGateway.save(branding)
            .map(savedBranding -> InstitutionResponseMapper.toResponse(savedInstitution, savedBranding)));
      });
  }

  private Mono<String> resolveId(String requestedId, String name) {
    if (requestedId != null && !requestedId.isBlank()) {
      return Mono.just(requestedId.trim());
    }
    String base = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    return findAvailableId(base, 0);
  }

  private Mono<String> findAvailableId(String base, int attempt) {
    String candidate = attempt == 0 ? base : base + "-" + attempt;
    return institutionGateway.findById(candidate)
      .flatMap(existing -> findAvailableId(base, attempt + 1))
      .switchIfEmpty(Mono.just(candidate));
  }
}
