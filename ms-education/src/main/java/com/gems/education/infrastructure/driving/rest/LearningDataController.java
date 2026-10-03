package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.LearningDataGateway;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Removes what a user did in their courses (enrollments, attempts, deliveries, survey answers,
 * reviews, path enrollments, group memberships and notifications). Called after deleting the
 * account in ms-auth. An admin purges inside their institution; the super admin everywhere.
 */
@RestController
public class LearningDataController {
  private final LearningDataGateway learningData;

  public LearningDataController(LearningDataGateway learningData) {
    this.learningData = learningData;
  }

  @DeleteMapping("/api/v1/students/{studentId}/learning-data")
  public Mono<ResponseEntity<Void>> purge(@PathVariable Long studentId) {
    return CurrentUser.require(caller -> caller.isSuperAdmin() || caller.isAdmin(),
        "Only administrators remove learning data")
      .flatMap(caller -> learningData.purge(studentId, scope(caller)))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  private static String scope(AuthenticatedUser caller) {
    return caller.isSuperAdmin() ? null : caller.institutionId();
  }
}
