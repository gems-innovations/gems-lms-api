package com.gems.admin.application.command;

public record BrandingCommand(
  String companyId,
  String domain,
  String logoUrl,
  String faviconUrl,
  String primaryColor,
  String secondaryColor,
  String accentColor,
  String textColor,
  String theme,
  String loginBackgroundUrl,
  String customCss
) {
}
