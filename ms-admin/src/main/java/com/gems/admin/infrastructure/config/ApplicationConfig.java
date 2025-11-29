package com.gems.admin.infrastructure.config;

import com.gems.admin.application.CreateBrandingUseCase;
import com.gems.admin.application.GetBrandingByCompanyIdUseCase;
import com.gems.admin.application.UpdateBrandingUseCase;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.infrastructure.driven.postgresql.BrandingRepositoryAdapter;
import com.gems.admin.infrastructure.driven.postgresql.IBrandingRepository;
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
}
