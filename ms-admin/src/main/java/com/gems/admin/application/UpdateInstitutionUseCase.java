package com.gems.admin.application;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Branding;
import com.gems.admin.domain.entities.Institution;
import com.gems.admin.domain.entities.InstitutionMetadata;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateInstitutionUseCase {
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public UpdateInstitutionUseCase(InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  public Mono<InstitutionResponse> execute(String id, InstitutionCommand command) {
    return institutionGateway.findById(id)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found with ID " + id)))
      .flatMap(existing -> {
        if (command.name() != null && !command.name().isBlank()) existing.setName(command.name());
        if (command.type() != null && !command.type().isBlank()) existing.setType(command.type());
        if (command.status() != null && !command.status().isBlank()) existing.setStatus(command.status());
        existing.setUpdatedAt(LocalDateTime.now());

        if (command.metadata() != null) {
          InstitutionMetadata metadata = existing.getMetadata();
          if (metadata == null) {
            metadata = new InstitutionMetadata();
            metadata.setInstitutionId(id);
          }
          if (command.metadata().description() != null) metadata.setDescription(command.metadata().description());
          if (command.metadata().website() != null) metadata.setWebsite(command.metadata().website());
          if (command.metadata().contactEmail() != null) metadata.setContactEmail(command.metadata().contactEmail());
          if (command.metadata().phoneNumber() != null) metadata.setPhoneNumber(command.metadata().phoneNumber());
          if (command.metadata().address() != null) metadata.setAddress(command.metadata().address());
          if (command.metadata().subscriptionType() != null) metadata.setSubscriptionType(command.metadata().subscriptionType());
          if (command.metadata().maxUsers() != null) metadata.setMaxUsers(command.metadata().maxUsers());
          metadata.setLastActivity(LocalDateTime.now());
          existing.setMetadata(metadata);
        }

        return institutionGateway.update(existing);
      })
      .flatMap(updatedInstitution -> updateBranding(id, command)
        .map(branding -> InstitutionResponseMapper.toResponse(updatedInstitution, branding)));
  }

  private Mono<Branding> updateBranding(String institutionId, InstitutionCommand command) {
    return brandingGateway.findByCompanyId(institutionId)
      .defaultIfEmpty(newBranding(institutionId))
      .flatMap(branding -> {
        if (command.branding() != null) {
          if (command.branding().type() != null) branding.setType(command.branding().type());
          if (command.branding().logoUrl() != null) branding.setLogoUrl(command.branding().logoUrl());
          if (command.branding().iconUrl() != null) branding.setIconUrl(command.branding().iconUrl());
          if (command.branding().colorPrimary() != null) branding.setPrimaryColor(command.branding().colorPrimary());
          if (command.branding().colorSecondary() != null) branding.setSecondaryColor(command.branding().colorSecondary());
          if (command.branding().backgroundColor() != null) branding.setBackgroundColor(command.branding().backgroundColor());
          if (command.branding().darkMode() != null) branding.setDarkMode(command.branding().darkMode());
        }
        branding.setUpdatedAt(LocalDateTime.now());
        return branding.getBrandingId() == null
          ? brandingGateway.save(branding)
          : brandingGateway.update(branding);
      });
  }

  private Branding newBranding(String institutionId) {
    Branding branding = new Branding();
    branding.setCompanyId(institutionId);
    branding.setType("color-badge");
    branding.setPrimaryColor("#6C63FF");
    branding.setSecondaryColor("#1E1B4B");
    branding.setDarkMode(false);
    return branding;
  }
}
