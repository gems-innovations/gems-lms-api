package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.Actor;
import com.gems.education.application.CourseCommunityUseCase;
import com.gems.education.application.CourseCommunityUseCase.ThreadPage;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.education.domain.entities.ForumThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseCommunityControllerTest extends ControllerTestSupport {
  private final CourseCommunityUseCase community = mock(CourseCommunityUseCase.class);
  private final UserDirectory users = mock(UserDirectory.class);
  private CourseCommunityController controller;

  @BeforeEach
  void setUp() {
    givenCourses();
    when(community.announcements(anyLong())).thenReturn(Flux.empty());
    when(community.threads(anyLong(), any(), anyInt(), anyInt())).thenReturn(Mono.just(new ThreadPage(List.of(), 3)));
    when(community.enrolled(5L, 1L)).thenReturn(Mono.just(true));
    when(community.enrolled(5L, 2L)).thenReturn(Mono.just(false));
    LocalDateTime t = LocalDateTime.now();
    when(community.openThread(any(), any(), any(), any())).thenReturn(Mono.just(
      new ForumThread(1L, 1L, 5L, "Ana Ruiz", "STUDENT", "Duda", "x", false, false, 0, t, t, t)));
    controller = new CourseCommunityController(community, access, users);
  }

  @Test
  void enrolledStudentsAndStaffTakePart() {
    client(controller, STUDENT).get().uri("/api/v1/courses/1/announcements").exchange().expectStatus().isOk();
    client(controller, INSTRUCTOR).get().uri("/api/v1/courses/2/forum/threads").exchange().expectStatus().isOk()
      .expectHeader().valueEquals("X-Total-Count", "3");
  }

  @Test
  void studentsOutsideTheCourseOrInstitutionCannot() {
    client(controller, STUDENT).get().uri("/api/v1/courses/2/forum/threads").exchange().expectStatus().isForbidden();
    client(controller, OTHER_ADMIN).get().uri("/api/v1/courses/1/announcements").exchange().expectStatus().isForbidden();
    verify(community, never()).threads(anyLong(), any(), anyInt(), anyInt());
  }

  @Test
  void messagesCarryTheAuthorNameFromTheUserDirectory() {
    when(users.find(5L)).thenReturn(Mono.just(new UserDirectory.UserProfile(5L, "Ana Ruiz", "inst-1")));
    client(controller, STUDENT).post().uri("/api/v1/courses/1/forum/threads")
      .bodyValue(Map.of("title", "Duda", "body", "x")).exchange().expectStatus().isCreated();

    ArgumentCaptor<Actor> actor = ArgumentCaptor.forClass(Actor.class);
    verify(community).openThread(actor.capture(), any(), eq("Duda"), eq("x"));
    assertThat(actor.getValue()).isEqualTo(new Actor(5L, "Ana Ruiz", "STUDENT", false));
  }

  @Test
  void anUnavailableDirectoryFallsBackToAGenericName() {
    when(users.find(5L)).thenReturn(Mono.error(new RuntimeException("ms-auth down")));
    client(controller, STUDENT).post().uri("/api/v1/courses/1/forum/threads")
      .bodyValue(Map.of("title", "Duda", "body", "x")).exchange().expectStatus().isCreated();
    verify(community).openThread(argThat(a -> a.name().equals("Usuario")), any(), any(), any());
  }
}
