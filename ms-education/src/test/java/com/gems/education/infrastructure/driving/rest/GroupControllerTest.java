package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.GroupUseCase;
import com.gems.education.application.exceptions.GroupNotFoundException;
import com.gems.education.domain.entities.Group;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupControllerTest extends ControllerTestSupport {
  private final GroupUseCase groupUseCase = mock(GroupUseCase.class);
  private GroupController controller;

  private static Group group(Long id, String institutionId) {
    return new Group(id, "Grupo 1", institutionId, 3L, List.of(5L), List.of(1L), List.of(), LocalDateTime.now());
  }

  @BeforeEach
  void setUp() {
    givenCourses();
    when(groupUseCase.get(1L)).thenReturn(Mono.just(group(1L, "inst-1")));
    controller = new GroupController(groupUseCase, access);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void staffListTheirInstitutionsGroups() {
    when(groupUseCase.ofInstitution("inst-1")).thenReturn(Flux.just(group(1L, "inst-1")));

    as(INSTRUCTOR).get().uri("/api/v1/groups?institutionId=inst-2").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].studentIds[0]").isEqualTo(5);
    verify(groupUseCase).ofInstitution("inst-1");
  }

  @Test
  void superAdminListsEveryGroupOrOneInstitution() {
    when(groupUseCase.all()).thenReturn(Flux.just(group(1L, "inst-1"), group(2L, "inst-2")));
    when(groupUseCase.ofInstitution("inst-2")).thenReturn(Flux.just(group(2L, "inst-2")));

    as(SUPER_ADMIN).get().uri("/api/v1/groups").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.length()").isEqualTo(2);
    as(SUPER_ADMIN).get().uri("/api/v1/groups?institutionId=inst-2").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.length()").isEqualTo(1);
  }

  @Test
  void studentsCannotSeeGroups() {
    as(STUDENT).get().uri("/api/v1/groups").exchange().expectStatus().isForbidden();
    as(STUDENT).get().uri("/api/v1/groups/1").exchange().expectStatus().isForbidden();
  }

  @Test
  void otherInstitutionsCannotReadOrTouchTheGroup() {
    as(OTHER_ADMIN).get().uri("/api/v1/groups/1").exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).delete().uri("/api/v1/groups/1").exchange().expectStatus().isForbidden();
    verify(groupUseCase, never()).delete(any());
  }

  @Test
  void createDefaultsToTheCallersInstitution() {
    when(groupUseCase.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(group(9L, "inst-1")));

    as(ADMIN).post().uri("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"name\":\"Grupo 1\",\"studentIds\":[5],\"courseIds\":[1]}")
      .exchange().expectStatus().isCreated()
      .expectBody().jsonPath("$.id").isEqualTo(9);
    verify(groupUseCase).create("Grupo 1", "inst-1", null, List.of(5L), List.of(1L), null);
  }

  @Test
  void createRejectsAnotherInstitutionOrItsCourses() {
    when(getCourseByIdUseCase.execute(2L)).thenReturn(Mono.just(course(2L, "published", "inst-2")));

    as(ADMIN).post().uri("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"name\":\"X\",\"institutionId\":\"inst-2\"}")
      .exchange().expectStatus().isForbidden();
    as(ADMIN).post().uri("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"name\":\"X\",\"courseIds\":[2]}")
      .exchange().expectStatus().isForbidden();
    verify(groupUseCase, never()).create(any(), any(), any(), any(), any(), any());
  }

  @Test
  void superAdminMustNameTheInstitution() {
    as(SUPER_ADMIN).post().uri("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"name\":\"X\"}")
      .exchange().expectStatus().isBadRequest();
  }

  @Test
  void patchUpdatesOnlyTheGivenFields() {
    when(groupUseCase.update(eq(1L), any(), any(), anyBoolean(), any(), any(), any()))
      .thenReturn(Mono.just(group(1L, "inst-1")));

    as(INSTRUCTOR).put().uri("/api/v1/groups/1").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"clearInstructor\":true,\"studentIds\":[5,6]}")
      .exchange().expectStatus().isOk();
    verify(groupUseCase).update(1L, null, null, true, List.of(5L, 6L), null, null);
  }

  @Test
  void aGroupCannotMoveToAnotherInstitution() {
    as(ADMIN).put().uri("/api/v1/groups/1").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"institutionId\":\"inst-2\"}")
      .exchange().expectStatus().isForbidden();
    verify(groupUseCase, never()).update(any(), any(), any(), anyBoolean(), any(), any(), any());
  }

  @Test
  void staffDeleteTheirGroupsAndMissingOnesAre404() {
    when(groupUseCase.delete(1L)).thenReturn(Mono.empty());
    when(groupUseCase.get(99L)).thenReturn(Mono.error(new GroupNotFoundException(99L)));

    as(ADMIN).delete().uri("/api/v1/groups/1").exchange().expectStatus().isNoContent();
    as(ADMIN).get().uri("/api/v1/groups/99").exchange().expectStatus().isNotFound();
  }
}
