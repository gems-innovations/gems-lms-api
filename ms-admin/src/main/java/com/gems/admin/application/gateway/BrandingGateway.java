package com.gems.admin.application.gateway;

import com.gems.admin.domain.entities.Branding;
import reactor.core.publisher.Mono;

public interface BrandingGateway {
  Mono<Branding> save(Branding branding);

  Mono<Branding> findByCompanyId(String companyId);

  Mono<Branding> update(Branding branding);

  Mono<Void> deleteByCompanyId(String companyId);
}

