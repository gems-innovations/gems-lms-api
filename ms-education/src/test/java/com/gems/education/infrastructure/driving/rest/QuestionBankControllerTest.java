package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.QuestionBankUseCase;
import com.gems.education.application.gateway.QuestionBankGateway;
import com.gems.education.domain.entities.BankQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuestionBankControllerTest extends ControllerTestSupport {
  private final QuestionBankGateway gateway = mock(QuestionBankGateway.class);
  private QuestionBankController controller;

  private static final Map<String, Object> TF = Map.of("category", "SQL", "type", "true-false",
    "question", Map.of("question", "¿SELECT lee datos?", "correctAnswer", true));

  @BeforeEach
  void setUp() {
    when(gateway.save(any())).thenAnswer(inv -> {
      BankQuestion q = inv.getArgument(0);
      return Mono.just(new BankQuestion(q.id() == null ? 1L : q.id(), q.institutionId(), q.category(), q.type(),
        q.payload(), q.createdBy(), q.createdAt(), q.updatedAt()));
    });
    when(gateway.find(any(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.empty());
    when(gateway.count(any(), any(), any())).thenReturn(Mono.just(0L));
    controller = new QuestionBankController(new QuestionBankUseCase(gateway), access);
  }

  @Test
  void staffCreateQuestionsInTheirInstitution() {
    client(controller, INSTRUCTOR).post().uri("/api/v1/question-bank").bodyValue(TF).exchange()
      .expectStatus().isCreated()
      .expectBody().jsonPath("$.institutionId").isEqualTo("inst-1").jsonPath("$.question.correctAnswer").isEqualTo(true);
  }

  @Test
  void studentsCannotUseTheBank() {
    client(controller, STUDENT).get().uri("/api/v1/question-bank").exchange().expectStatus().isForbidden();
    client(controller, STUDENT).post().uri("/api/v1/question-bank").bodyValue(TF).exchange().expectStatus().isForbidden();
    verify(gateway, never()).save(any());
  }

  @Test
  void theSuperAdminMustNameTheInstitution() {
    client(controller, SUPER_ADMIN).get().uri("/api/v1/question-bank").exchange().expectStatus().isBadRequest();
    client(controller, SUPER_ADMIN).get().uri("/api/v1/question-bank?institutionId=inst-2").exchange()
      .expectStatus().isOk().expectHeader().valueEquals("X-Total-Count", "0");
    verify(gateway).find(eq("inst-2"), isNull(), isNull(), eq(0), eq(20));
  }

  @Test
  void invalidQuestionsAreRejected() {
    Map<String, Object> noCorrect = Map.of("category", "SQL", "type", "multiple-choice", "question", Map.of(
      "question", "¿Cuál?", "options", List.of(Map.of("id", "a", "text", "1"), Map.of("id", "b", "text", "2")),
      "correctAnswers", List.of("z")));
    client(controller, ADMIN).post().uri("/api/v1/question-bank").bodyValue(noCorrect).exchange().expectStatus().isBadRequest();
    client(controller, ADMIN).post().uri("/api/v1/question-bank")
      .bodyValue(Map.of("category", "SQL", "type", "essay", "question", Map.of("question", "x"))).exchange()
      .expectStatus().isBadRequest();
    verify(gateway, never()).save(any());
  }

  @Test
  void questionsOfAnotherInstitutionCannotBeChanged() {
    when(gateway.findById(8L)).thenReturn(Mono.just(new BankQuestion(8L, "inst-2", "SQL", "true-false", "{}", null,
      LocalDateTime.now(), LocalDateTime.now())));
    client(controller, ADMIN).put().uri("/api/v1/question-bank/8").bodyValue(TF).exchange().expectStatus().isBadRequest();
    client(controller, ADMIN).delete().uri("/api/v1/question-bank/8").exchange().expectStatus().isBadRequest();
    verify(gateway, never()).delete(anyLong());
  }
}
