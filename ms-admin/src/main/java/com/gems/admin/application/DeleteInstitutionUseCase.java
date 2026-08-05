package com.gems.admin.application;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.InstitutionGateway;
import reactor.core.publisher.Mono;

public class DeleteInstitutionUseCase {
  private final InstitutionGateway institutionGateway;

  public DeleteInstitutionUseCase(InstitutionGateway institutionGateway) {
    this.institutionGateway = institutionGateway;
  }

  public Mono<Void> execute(String id) {
    return institutionGateway.findById(id)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found with ID " + id)))
      .flatMap(existing -> institutionGateway.deleteById(id));
  }
}
