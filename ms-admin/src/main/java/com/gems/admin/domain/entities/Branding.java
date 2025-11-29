package com.gems.admin.domain.entities;

import java.time.LocalDateTime;

public class Branding {
  private Long brandingId;
  private String companyId;
  private String domain;
  private String logoUrl;
  private String faviconUrl;
  private String primaryColor;
  private String secondaryColor;
  private String accentColor;
  private String textColor;
  private String theme;
  private String loginBackgroundUrl;
  private String customCss;
  private LocalDateTime updatedAt;

  public Branding(Long brandingId, String companyId, String domain, String logoUrl, String faviconUrl,
                  String primaryColor, String secondaryColor, String accentColor, String textColor,
                  String theme, String loginBackgroundUrl, String customCss, LocalDateTime updatedAt) {
    this.brandingId = brandingId;
    this.companyId = companyId;
    this.domain = domain;
    this.logoUrl = logoUrl;
    this.faviconUrl = faviconUrl;
    this.primaryColor = primaryColor;
    this.secondaryColor = secondaryColor;
    this.accentColor = accentColor;
    this.textColor = textColor;
    this.theme = theme;
    this.loginBackgroundUrl = loginBackgroundUrl;
    this.customCss = customCss;
    this.updatedAt = updatedAt;
  }

  public Branding() {
  }

  public Long getBrandingId() {
    return brandingId;
  }

  public void setBrandingId(Long brandingId) {
    this.brandingId = brandingId;
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

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
