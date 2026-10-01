package com.gems.admin.application.command;

public record InstitutionBrandingCommand(
  String type,
  String logoUrl,
  String iconUrl,
  String colorPrimary,
  String colorSecondary,
  String backgroundColor,
  Boolean darkMode
) {
}
