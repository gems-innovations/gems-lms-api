package com.gems.admin.infrastructure.driving.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Branding configuration for an institution")
public class InstitutionBrandingRequest {

  @Schema(description = "Branding type", example = "color-badge", allowableValues = {"logo-text", "icon-only", "color-badge"})
  private String type;

  @Schema(description = "Logo image URL")
  private String logoUrl;

  @Schema(description = "Icon image URL")
  private String iconUrl;

  @Schema(description = "Primary brand color (hex)", example = "#6C63FF")
  private String colorPrimary;

  @Schema(description = "Secondary brand color (hex)", example = "#1E1B4B")
  private String colorSecondary;

  @Schema(description = "Background color (hex)")
  private String backgroundColor;

  @Schema(description = "Whether the institution theme defaults to dark mode")
  private Boolean darkMode;

  public InstitutionBrandingRequest() {
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public void setIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
  }

  public String getColorPrimary() {
    return colorPrimary;
  }

  public void setColorPrimary(String colorPrimary) {
    this.colorPrimary = colorPrimary;
  }

  public String getColorSecondary() {
    return colorSecondary;
  }

  public void setColorSecondary(String colorSecondary) {
    this.colorSecondary = colorSecondary;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public Boolean getDarkMode() {
    return darkMode;
  }

  public void setDarkMode(Boolean darkMode) {
    this.darkMode = darkMode;
  }
}
