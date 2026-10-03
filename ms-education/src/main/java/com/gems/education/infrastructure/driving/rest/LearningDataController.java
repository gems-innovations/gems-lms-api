package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.LearningDataGateway;
import com.gems.education.infrastructure.driven.auth.InstitutionMembers;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Removes what a user did in their courses (enrollments, attempts, deliveries, survey answers,
 * reviews, path enrollments, group memberships and notifications). Called before deleting the
 * account in ms-auth. An admin purges inside their institution; the super admin everywhere.
 */
@RestController
public class LearningDataController {
  private final LearningDataGateway learningData;
  private final InstitutionMembers members;

  public LearningDataController(LearningDataGateway learningData, InstitutionMembers members) {
    this.learningData = learningData;
    this.members = members;
  }

  @DeleteMapping("/api/v1/students/{studentId}/learning-data")
  public Mono<ResponseEntity<Void>> purge(@PathVariable Long studentId) {
    return CurrentUser.require(caller -> (caller.isSuperAdmin() || caller.isAdmin()) && !caller.isUser(studentId),
        "Only administrators remove learning data")
      .flatMap(caller -> (caller.isSuperAdmin() ? Mono.<Void>empty()
          : members.requireMembers(java.util.List.of(studentId), scope(caller)))
        .then(Mono.defer(() -> learningData.purge(studentId, scope(caller)))))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  private static String scope(AuthenticatedUser caller) {
    return caller.isSuperAdmin() ? null : caller.institutionId();
  }
}
