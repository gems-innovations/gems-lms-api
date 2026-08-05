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
  public BrandingGateway brandingGateway(IBrandingRepository brandingRepository) {
    return new BrandingRepositoryAdapter(brandingRepository);
  }

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
  public InstitutionGateway institutionGateway(IInstitutionRepository institutionRepository,
                                                IInstitutionMetadataRepository metadataRepository) {
    return new InstitutionRepositoryAdapter(institutionRepository, metadataRepository);
  }

  @Bean
  public CreateInstitutionUseCase createInstitutionUseCase(InstitutionGateway institutionGateway) {
    return new CreateInstitutionUseCase(institutionGateway);
  }

  @Bean
  public GetInstitutionByIdUseCase getInstitutionByIdUseCase(InstitutionGateway institutionGateway) {
    return new GetInstitutionByIdUseCase(institutionGateway);
  }

  @Bean
  public GetAllInstitutionsUseCase getAllInstitutionsUseCase(InstitutionGateway institutionGateway) {
    return new GetAllInstitutionsUseCase(institutionGateway);
  }

  @Bean
  public UpdateInstitutionUseCase updateInstitutionUseCase(InstitutionGateway institutionGateway) {
    return new UpdateInstitutionUseCase(institutionGateway);
  }

  @Bean
  public DeleteInstitutionUseCase deleteInstitutionUseCase(InstitutionGateway institutionGateway) {
    return new DeleteInstitutionUseCase(institutionGateway);
  }
}

