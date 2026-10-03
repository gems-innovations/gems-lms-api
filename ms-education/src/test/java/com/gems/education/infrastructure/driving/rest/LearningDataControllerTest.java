package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.LearningDataGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LearningDataControllerTest extends ControllerTestSupport {
  private final LearningDataGateway learningData = mock(LearningDataGateway.class);
  private LearningDataController controller;

  @BeforeEach
  void setUp() {
    when(learningData.purge(anyLong(), any())).thenReturn(Mono.empty());
    controller = new LearningDataController(learningData, members);
  }

  @Test
  void adminsPurgeInsideTheirInstitutionAndTheSuperAdminEverywhere() {
    client(controller, ADMIN).delete().uri("/api/v1/students/5/learning-data").exchange().expectStatus().isNoContent();
    client(controller, SUPER_ADMIN).delete().uri("/api/v1/students/5/learning-data").exchange().expectStatus().isNoContent();

    verify(learningData).purge(5L, "inst-1");
    verify(learningData).purge(5L, null);
  }

  @Test
  void instructorsAndStudentsCannotPurge() {
    client(controller, INSTRUCTOR).delete().uri("/api/v1/students/5/learning-data").exchange().expectStatus().isForbidden();
    client(controller, STUDENT).delete().uri("/api/v1/students/5/learning-data").exchange().expectStatus().isForbidden();
    verifyNoInteractions(learningData);
  }

  @Test
  void cannotPurgeAnotherInstitutionsUserOrOneself() {
    when(members.requireMembers(any(), any())).thenReturn(Mono.error(
      new com.gems.shared.security.ForbiddenException("Another institution")));
    client(controller, ADMIN).delete().uri("/api/v1/students/7/learning-data").exchange().expectStatus().isForbidden();
    client(controller, ADMIN).delete().uri("/api/v1/students/2/learning-data").exchange().expectStatus().isForbidden();
    verifyNoInteractions(learningData);
  }
}
