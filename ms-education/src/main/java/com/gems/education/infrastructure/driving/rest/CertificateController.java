package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.CertificateUseCase;
import com.gems.education.domain.entities.Certificate;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {
  private final CertificateUseCase certificates;

  public CertificateController(CertificateUseCase certificates) { this.certificates = certificates; }

  @GetMapping("/me")
  public Mono<ResponseEntity<List<CertificateResponse>>> mine() {
    return CurrentUser.get().flatMap(user -> certificates.sync(user.userId()).map(CertificateResponse::from)
      .collectList()).map(ResponseEntity::ok);
  }

  @GetMapping("/verify/{code}")
  public Mono<ResponseEntity<CertificateResponse>> verify(@PathVariable String code) {
    return certificates.verify(code).switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND,
      "Certificate not found"))).map(CertificateResponse::from).map(ResponseEntity::ok);
  }

  public record CertificateResponse(String code, String studentName, String institutionId,
      String resourceType, Long resourceId, String resourceTitle, String instructorName,
      LocalDateTime completedAt, LocalDateTime issuedAt, boolean valid) {
    static CertificateResponse from(Certificate c) {
      return new CertificateResponse(c.code(), c.studentName(), c.institutionId(), c.resourceType(),
        c.resourceId(), c.resourceTitle(), c.instructorName(), c.completedAt(), c.issuedAt(), c.valid());
    }
  }
}
