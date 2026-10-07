package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.QuestionBankUseCase;
import com.gems.education.application.gateway.QuestionBankGateway.CategoryCount;
import com.gems.education.domain.entities.BankQuestion;
import com.gems.shared.web.Paging;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Question bank of an institution, managed by its staff. Quizzes draw from it with
 * `questionPools` ([{category, count}]). The super admin passes institutionId.
 */
@RestController
@RequestMapping("/api/v1/question-bank")
public class QuestionBankController {
  private static final int MAX_LIMIT = Paging.MAX_LIMIT;

  private final QuestionBankUseCase bank;
  private final EducationAccess access;

  public QuestionBankController(QuestionBankUseCase bank, EducationAccess access) {
    this.bank = bank;
    this.access = access;
  }

  @GetMapping
  public Mono<ResponseEntity<List<QuestionResponse>>> list(@RequestParam(required = false) String institutionId,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int limit) {
    int size = Math.max(1, Math.min(limit, MAX_LIMIT));
    return scope(institutionId)
      .flatMap(inst -> bank.list(inst, category, search, Math.max(0, page), size))
      .map(p -> ResponseEntity.ok().header(Paging.TOTAL_HEADER, String.valueOf(p.total()))
        .body(p.items().stream().map(QuestionResponse::from).toList()));
  }

  @GetMapping("/categories")
  public Mono<ResponseEntity<List<CategoryCount>>> categories(@RequestParam(required = false) String institutionId) {
    return scope(institutionId).flatMap(inst -> bank.categories(inst).collectList()).map(ResponseEntity::ok);
  }

  @PostMapping
  public Mono<ResponseEntity<QuestionResponse>> create(@RequestParam(required = false) String institutionId,
                                                       @Valid @RequestBody QuestionRequest request) {
    return access.staff().flatMap(caller -> scope(institutionId)
        .flatMap(inst -> bank.create(inst, request.category(), request.type(), payload(request), caller.userId())))
      .map(q -> ResponseEntity.status(HttpStatus.CREATED).body(QuestionResponse.from(q)));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<QuestionResponse>> update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
    return ownScope()
      .flatMap(inst -> bank.update(id, inst.orElse(null), request.category(), request.type(), payload(request)))
      .map(q -> ResponseEntity.ok(QuestionResponse.from(q)));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
    return ownScope().flatMap(inst -> bank.delete(id, inst.orElse(null))).thenReturn(ResponseEntity.noContent().<Void>build());
  }

  /** Staff work in their institution; the super admin must say which one. */
  private Mono<String> scope(String requested) {
    return access.staff().flatMap(caller -> {
      if (!caller.isSuperAdmin()) return Mono.just(caller.institutionId());
      return requested == null || requested.isBlank()
        ? Mono.error(new IllegalArgumentException("institutionId is required"))
        : Mono.just(requested);
    });
  }

  /** Institution of the caller for single questions; empty for the super admin (any institution). */
  private Mono<java.util.Optional<String>> ownScope() {
    return access.staff().map(caller -> java.util.Optional.ofNullable(caller.isSuperAdmin() ? null : caller.institutionId()));
  }

  /** The stored question JSON: the client's question with the validated type, without a client id. */
  static String payload(QuestionRequest request) {
    if (!(request.question() instanceof ObjectNode q)) throw new IllegalArgumentException("question must be an object");
    if (q.path("question").asText("").isBlank()) throw new IllegalArgumentException("The question text is required");
    ObjectNode copy = q.deepCopy();
    copy.remove("id");
    copy.put("type", request.type());
    switch (request.type()) {
      case "multiple-choice" -> {
        Set<String> ids = new HashSet<>();
        for (JsonNode o : copy.path("options")) {
          String oid = o.path("id").asText("");
          if (oid.isBlank() || o.path("text").asText("").isBlank() || !ids.add(oid)) {
            throw new IllegalArgumentException("Every option needs a unique id and a text");
          }
        }
        if (ids.size() < 2) throw new IllegalArgumentException("A multiple-choice question needs at least two options");
        if (!copy.path("correctAnswers").isArray() || copy.path("correctAnswers").isEmpty()) {
          throw new IllegalArgumentException("Mark at least one correct option");
        }
        for (JsonNode c : copy.path("correctAnswers")) {
          if (!ids.contains(c.asText())) throw new IllegalArgumentException("Correct answers must be options of the question");
        }
      }
      case "true-false" -> {
        if (!copy.path("correctAnswer").isBoolean()) throw new IllegalArgumentException("correctAnswer must be true or false");
      }
      default -> { }
    }
    return copy.toString();
  }

  public record QuestionRequest(@NotBlank(message = "Category is required") String category,
                                @NotBlank(message = "Type is required") String type,
                                @NotNull(message = "Question is required") JsonNode question) {
  }

  public record QuestionResponse(Long id, String institutionId, String category, String type,
                                 @JsonRawValue String question, LocalDateTime createdAt, LocalDateTime updatedAt) {
    static QuestionResponse from(BankQuestion q) {
      return new QuestionResponse(q.id(), q.institutionId(), q.category(), q.type(), q.payload(), q.createdAt(),
        q.updatedAt());
    }
  }
}
