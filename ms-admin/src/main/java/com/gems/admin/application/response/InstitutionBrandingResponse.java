package com.gems.admin.application.response;

public record InstitutionBrandingResponse(
  String type,
  String logoUrl,
  String iconUrl,
  String colorPrimary,
  String colorSecondary,
  String backgroundColor,
  Boolean darkMode
) {
}
