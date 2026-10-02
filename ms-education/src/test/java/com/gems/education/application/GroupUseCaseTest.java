package com.gems.education.application;

import com.gems.education.application.exceptions.GroupNotFoundException;
import com.gems.education.application.gateway.GroupGateway;
import com.gems.education.domain.entities.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupUseCaseTest {
  private final GroupGateway gateway = mock(GroupGateway.class);
  private GroupUseCase useCase;

  private final Group existing = new Group(1L, "Grupo 1", "inst-1", 3L, List.of(5L), List.of(10L), List.of(),
    LocalDateTime.of(2026, 1, 1, 0, 0));

  @BeforeEach
  void setUp() {
    useCase = new GroupUseCase(gateway);
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(gateway.findById(1L)).thenReturn(Mono.just(existing));
    when(gateway.findById(99L)).thenReturn(Mono.empty());
  }

  @Test
  void createTrimsTheNameAndDropsDuplicateIds() {
    StepVerifier.create(useCase.create("  Cohorte A ", "inst-1", null, Arrays.asList(5L, 5L, null, 6L), null, null))
      .assertNext(g -> {
        assertEquals("Cohorte A", g.name());
        assertEquals(List.of(5L, 6L), g.studentIds());
        assertEquals(List.of(), g.courseIds());
      })
      .verifyComplete();
  }

  @Test
  void createWithoutANameUsesTheDefault() {
    StepVerifier.create(useCase.create(" ", "inst-1", null, null, null, null))
      .assertNext(g -> assertEquals("Nuevo grupo", g.name()))
      .verifyComplete();
  }

  @Test
  void updateKeepsWhatIsNotSent() {
    StepVerifier.create(useCase.update(1L, null, null, false, List.of(5L, 6L), null, null))
      .assertNext(g -> {
        assertEquals("Grupo 1", g.name());
        assertEquals(3L, g.instructorId());
        assertEquals(List.of(5L, 6L), g.studentIds());
        assertEquals(List.of(10L), g.courseIds());
        assertEquals("inst-1", g.institutionId());
      })
      .verifyComplete();
  }

  @Test
  void updateCanUnassignTheInstructor() {
    StepVerifier.create(useCase.update(1L, "Nuevo", null, true, null, null, null))
      .assertNext(g -> {
        assertEquals("Nuevo", g.name());
        assertNull(g.instructorId());
      })
      .verifyComplete();
  }

  @Test
  void missingGroupsFail() {
    StepVerifier.create(useCase.get(99L)).expectError(GroupNotFoundException.class).verify();
    StepVerifier.create(useCase.delete(99L)).expectError(GroupNotFoundException.class).verify();
    verify(gateway, never()).deleteById(any());
  }

  @Test
  void deleteRemovesAnExistingGroup() {
    when(gateway.deleteById(1L)).thenReturn(Mono.empty());

    StepVerifier.create(useCase.delete(1L)).verifyComplete();
    verify(gateway).deleteById(1L);
  }
}
