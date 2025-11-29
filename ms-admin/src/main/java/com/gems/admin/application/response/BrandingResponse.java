package com.gems.admin.application.response;

import java.time.LocalDateTime;

public record BrandingResponse(
  Long brandingId,
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
  String customCss,
  LocalDateTime updatedAt
) {
}
