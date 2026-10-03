package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Certificate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CertificateGateway {
  Mono<Certificate> save(Certificate certificate);
  Mono<Certificate> find(Long studentId, String resourceType, Long resourceId);
  Mono<Certificate> findByCode(String code);
  Flux<Certificate> findByStudent(Long studentId);
}
