package com.gems.admin.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BrandingTest {

  @Test
  void shouldCreateBrandingWithAllFields() {
    // Given
    Long brandingId = 1L;
    String companyId = "company-123";
    String domain = "example.com";
    String logoUrl = "https://cdn.example.com/logo.png";
    String faviconUrl = "https://cdn.example.com/favicon.ico";
    String primaryColor = "#3B82F6";
    String secondaryColor = "#8B5CF6";
    String accentColor = "#10B981";
    String textColor = "#1F2937";
    String theme = "light";
    String loginBackgroundUrl = "https://cdn.example.com/bg.jpg";
    String customCss = ".custom { color: red; }";
    LocalDateTime updatedAt = LocalDateTime.now();

    // When
    Branding branding = new Branding(
      brandingId, companyId, domain, logoUrl, faviconUrl,
      primaryColor, secondaryColor, accentColor, textColor,
      theme, loginBackgroundUrl, customCss, updatedAt
    );

    // Then
    assertEquals(brandingId, branding.getBrandingId());
    assertEquals(companyId, branding.getCompanyId());
    assertEquals(domain, branding.getDomain());
    assertEquals(logoUrl, branding.getLogoUrl());
    assertEquals(faviconUrl, branding.getFaviconUrl());
    assertEquals(primaryColor, branding.getPrimaryColor());
    assertEquals(secondaryColor, branding.getSecondaryColor());
    assertEquals(accentColor, branding.getAccentColor());
    assertEquals(textColor, branding.getTextColor());
    assertEquals(theme, branding.getTheme());
    assertEquals(loginBackgroundUrl, branding.getLoginBackgroundUrl());
    assertEquals(customCss, branding.getCustomCss());
    assertEquals(updatedAt, branding.getUpdatedAt());
  }

  @Test
  void shouldCreateEmptyBrandingAndSetFields() {
    // When
    Branding branding = new Branding();
    branding.setCompanyId("test-company");
    branding.setDomain("test.com");
    branding.setLogoUrl("https://test.com/logo.png");
    branding.setPrimaryColor("#FF0000");
    branding.setTheme("dark");

    // Then
    assertNull(branding.getBrandingId());
    assertEquals("test-company", branding.getCompanyId());
    assertEquals("test.com", branding.getDomain());
    assertEquals("https://test.com/logo.png", branding.getLogoUrl());
    assertEquals("#FF0000", branding.getPrimaryColor());
    assertEquals("dark", branding.getTheme());
  }

  @Test
  void shouldAllowNullOptionalFields() {
    // Given
    Branding branding = new Branding();
    branding.setCompanyId("company-456");

    // Then
    assertNull(branding.getBrandingId());
    assertEquals("company-456", branding.getCompanyId());
    assertNull(branding.getDomain());
    assertNull(branding.getLogoUrl());
    assertNull(branding.getFaviconUrl());
    assertNull(branding.getPrimaryColor());
    assertNull(branding.getSecondaryColor());
    assertNull(branding.getAccentColor());
    assertNull(branding.getTextColor());
    assertNull(branding.getTheme());
    assertNull(branding.getLoginBackgroundUrl());
    assertNull(branding.getCustomCss());
    assertNull(branding.getUpdatedAt());
  }

  @Test
  void shouldUpdateBrandingFields() {
    // Given
    Branding branding = new Branding();
    branding.setBrandingId(10L);
    branding.setCompanyId("initial-company");
    branding.setPrimaryColor("#000000");

    // When
    branding.setCompanyId("updated-company");
    branding.setPrimaryColor("#FFFFFF");
    branding.setTheme("dark");

    // Then
    assertEquals(10L, branding.getBrandingId());
    assertEquals("updated-company", branding.getCompanyId());
    assertEquals("#FFFFFF", branding.getPrimaryColor());
    assertEquals("dark", branding.getTheme());
  }
}
