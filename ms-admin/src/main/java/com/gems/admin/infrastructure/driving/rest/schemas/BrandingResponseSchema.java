package com.gems.admin.infrastructure.driving.rest.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "BrandingResponse", description = "Response object containing branding details")
public class BrandingResponseSchema {
  @Schema(description = "Unique identifier of the branding", example = "1")
  public Long brandingId;

  @Schema(description = "Company ID", example = "company-123")
  public String companyId;

  @Schema(description = "Domain for the branding", example = "example.com")
  public String domain;

  @Schema(description = "URL of the company logo", example = "https://cdn.example.com/logo.png")
  public String logoUrl;

  @Schema(description = "URL of the favicon", example = "https://cdn.example.com/favicon.ico")
  public String faviconUrl;

  @Schema(description = "Primary color in hex format", example = "#3B82F6")
  public String primaryColor;

  @Schema(description = "Secondary color in hex format", example = "#8B5CF6")
  public String secondaryColor;

  @Schema(description = "Accent color in hex format", example = "#10B981")
  public String accentColor;

  @Schema(description = "Text color in hex format", example = "#1F2937")
  public String textColor;

  @Schema(description = "Theme mode", example = "light")
  public String theme;

  @Schema(description = "URL of the login background image", example = "https://cdn.example.com/login-bg.jpg")
  public String loginBackgroundUrl;

  @Schema(description = "Custom CSS for additional styling")
  public String customCss;

  @Schema(description = "Timestamp when the branding was last updated", example = "2023-11-25T10:00:00")
  public LocalDateTime updatedAt;
}
