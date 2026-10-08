package com.gems.admin.infrastructure.config;

import com.gems.admin.application.*;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.infrastructure.driven.postgresql.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public CreateBrandingUseCase createBrandingUseCase(BrandingGateway brandingGateway) {
    return new CreateBrandingUseCase(brandingGateway);
  }

  @Bean
  public UpdateBrandingUseCase updateBrandingUseCase(BrandingGateway brandingGateway) {
    return new UpdateBrandingUseCase(brandingGateway);
  }

  @Bean
  public GetBrandingByCompanyIdUseCase getBrandingByCompanyIdUseCase(BrandingGateway brandingGateway) {
    return new GetBrandingByCompanyIdUseCase(brandingGateway);
  }

  @Bean
  public DeleteBrandingUseCase deleteBrandingUseCase(BrandingGateway brandingGateway) {
    return new DeleteBrandingUseCase(brandingGateway);
  }

  @Bean
  public CreateInstitutionUseCase createInstitutionUseCase(InstitutionGateway institutionGateway,
                                                             BrandingGateway brandingGateway) {
    return new CreateInstitutionUseCase(institutionGateway, brandingGateway);
  }

  @Bean
  public GetInstitutionByIdUseCase getInstitutionByIdUseCase(InstitutionGateway institutionGateway,
                                                               BrandingGateway brandingGateway) {
    return new GetInstitutionByIdUseCase(institutionGateway, brandingGateway);
  }

  @Bean
  public GetAllInstitutionsUseCase getAllInstitutionsUseCase(InstitutionGateway institutionGateway,
                                                               BrandingGateway brandingGateway) {
    return new GetAllInstitutionsUseCase(institutionGateway, brandingGateway);
  }

  @Bean
  public UpdateInstitutionUseCase updateInstitutionUseCase(InstitutionGateway institutionGateway,
                                                             BrandingGateway brandingGateway) {
    return new UpdateInstitutionUseCase(institutionGateway, brandingGateway);
  }

  @Bean
  public DeleteInstitutionUseCase deleteInstitutionUseCase(InstitutionGateway institutionGateway,
                                                           BrandingGateway brandingGateway) {
    return new DeleteInstitutionUseCase(institutionGateway, brandingGateway);
  }
}

