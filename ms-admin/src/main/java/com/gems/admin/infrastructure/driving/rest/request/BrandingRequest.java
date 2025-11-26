package com.gems.admin.infrastructure.driving.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request object for creating or updating branding")
public class BrandingRequest {

  @Schema(description = "Company ID", example = "company-123", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Company ID is required")
  private String companyId;

  @Schema(description = "Domain for the branding", example = "example.com")
  private String domain;

  @Schema(description = "URL of the company logo", example = "https://cdn.example.com/logo.png")
  private String logoUrl;

  @Schema(description = "URL of the favicon", example = "https://cdn.example.com/favicon.ico")
  private String faviconUrl;

  @Schema(description = "Primary color in hex format", example = "#3B82F6")
  @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Primary color must be a valid hex color")
  private String primaryColor;

  @Schema(description = "Secondary color in hex format", example = "#8B5CF6")
  @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Secondary color must be a valid hex color")
  private String secondaryColor;

  @Schema(description = "Accent color in hex format", example = "#10B981")
  @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Accent color must be a valid hex color")
  private String accentColor;

  @Schema(description = "Text color in hex format", example = "#1F2937")
  @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Text color must be a valid hex color")
  private String textColor;

  @Schema(description = "Theme mode", example = "light", allowableValues = {"light", "dark"})
  private String theme;

  @Schema(description = "URL of the login background image", example = "https://cdn.example.com/login-bg.jpg")
  private String loginBackgroundUrl;

  @Schema(description = "Custom CSS for additional styling")
  private String customCss;

  public BrandingRequest() {
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getFaviconUrl() {
    return faviconUrl;
  }

  public void setFaviconUrl(String faviconUrl) {
    this.faviconUrl = faviconUrl;
  }

  public String getPrimaryColor() {
    return primaryColor;
  }

  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  public String getSecondaryColor() {
    return secondaryColor;
  }

  public void setSecondaryColor(String secondaryColor) {
    this.secondaryColor = secondaryColor;
  }

  public String getAccentColor() {
    return accentColor;
  }

  public void setAccentColor(String accentColor) {
    this.accentColor = accentColor;
  }

  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String textColor) {
    this.textColor = textColor;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getLoginBackgroundUrl() {
    return loginBackgroundUrl;
  }

  public void setLoginBackgroundUrl(String loginBackgroundUrl) {
    this.loginBackgroundUrl = loginBackgroundUrl;
  }

  public String getCustomCss() {
    return customCss;
  }

  public void setCustomCss(String customCss) {
    this.customCss = customCss;
  }
}
