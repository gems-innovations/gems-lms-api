package com.gems.admin.application;

import com.gems.admin.application.response.InstitutionBrandingResponse;
import com.gems.admin.application.response.InstitutionMetadataResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.domain.entities.Branding;
import com.gems.admin.domain.entities.Institution;

final class InstitutionResponseMapper {

  private InstitutionResponseMapper() {
  }

  static InstitutionResponse toResponse(Institution institution, Branding branding) {
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

    InstitutionBrandingResponse brandingResponse = branding == null
      ? defaultBranding()
      : new InstitutionBrandingResponse(
          branding.getType(),
          branding.getLogoUrl(),
          branding.getIconUrl(),
          branding.getPrimaryColor(),
          branding.getSecondaryColor(),
          branding.getBackgroundColor(),
          branding.getDarkMode()
        );

    return new InstitutionResponse(
      institution.getId(),
      institution.getName(),
      institution.getType(),
      institution.getStatus(),
      institution.getUsersCount(),
      institution.getCreatedAt(),
      institution.getUpdatedAt(),
      metadataResponse,
      brandingResponse
    );
  }

  private static InstitutionBrandingResponse defaultBranding() {
    return new InstitutionBrandingResponse("color-badge", null, null, "#6C63FF", "#1E1B4B", null, false);
  }
}
