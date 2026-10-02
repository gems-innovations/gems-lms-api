package com.gems.admin;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.command.InstitutionMetadataCommand;
import com.gems.admin.domain.entities.Branding;
import com.gems.admin.infrastructure.driven.postgresql.BrandingEntity;

import java.time.LocalDateTime;

/** Short factories for test fixtures; fields a test does not care about get neutral defaults. */
public final class TestData {

  private TestData() {
  }

  public static Branding branding(Long id, String companyId, String domain, String logoUrl, String faviconUrl,
                                  String primaryColor, String secondaryColor, String accentColor, String textColor,
                                  String theme, String loginBackgroundUrl, String customCss, LocalDateTime updatedAt) {
    return new Branding(id, companyId, domain, "color-badge", logoUrl, null, faviconUrl, primaryColor,
      secondaryColor, accentColor, textColor, null, false, theme, loginBackgroundUrl, customCss, updatedAt);
  }

  public static BrandingEntity brandingEntity(Long id, String companyId, String domain, String logoUrl,
                                              String faviconUrl, String primaryColor, String secondaryColor,
                                              String accentColor, String textColor, String theme,
                                              String loginBackgroundUrl, String customCss, LocalDateTime updatedAt) {
    return new BrandingEntity(id, companyId, domain, "color-badge", logoUrl, null, faviconUrl, primaryColor,
      secondaryColor, accentColor, textColor, null, false, theme, loginBackgroundUrl, customCss, updatedAt);
  }

  public static InstitutionCommand institutionCommand(String id, String name, String type, String status,
                                                      InstitutionMetadataCommand metadata) {
    return new InstitutionCommand(id, name, type, status, metadata, null);
  }
}
