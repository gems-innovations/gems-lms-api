package com.gems.admin.infrastructure.driving.rest.mapper;

import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.infrastructure.driving.rest.request.BrandingRequest;

public class BrandingMapper {

  private BrandingMapper() {
  }

  public static BrandingCommand toCommand(BrandingRequest request) {
    return new BrandingCommand(
      request.getCompanyId(),
      request.getDomain(),
      request.getLogoUrl(),
      request.getFaviconUrl(),
      request.getPrimaryColor(),
      request.getSecondaryColor(),
      request.getAccentColor(),
      request.getTextColor(),
      request.getTheme(),
      request.getLoginBackgroundUrl(),
      request.getCustomCss()
    );
  }
}
