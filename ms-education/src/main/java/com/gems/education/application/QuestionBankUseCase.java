package com.gems.education.application;

import com.gems.education.application.gateway.QuestionBankGateway;
import com.gems.education.application.gateway.QuestionBankGateway.CategoryCount;
import com.gems.education.domain.entities.BankQuestion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/** Reusable questions of an institution. The payload is validated by the caller (it is client JSON). */
public class QuestionBankUseCase {
  public static final Set<String> TYPES = Set.of("multiple-choice", "true-false", "open");
  private static final int MAX_CATEGORY = 120;

  private final QuestionBankGateway gateway;

  public QuestionBankUseCase(QuestionBankGateway gateway) {
    this.gateway = gateway;
  }

  public Mono<Page> list(String institutionId, String category, String search, int page, int limit) {
    String c = blankToNull(category);
    String q = blankToNull(search);
    return Mono.zip(gateway.find(institutionId, c, q, page * limit, limit).collectList(), gateway.count(institutionId, c, q))
      .map(t -> new Page(t.getT1(), t.getT2()));
  }

  public Flux<CategoryCount> categories(String institutionId) {
    return gateway.categories(institutionId);
  }

  /** The question if it belongs to the institution (any institution for a null scope). */
  public Mono<BankQuestion> find(Long id, String institutionId) {
    return gateway.findById(id)
      .filter(q -> institutionId == null || Objects.equals(q.institutionId(), institutionId))
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Question not found")));
  }

  public Mono<BankQuestion> create(String institutionId, String category, String type, String payload, Long userId) {
    return validate(category, type).then(Mono.defer(() -> {
      LocalDateTime now = LocalDateTime.now();
      return gateway.save(new BankQuestion(null, institutionId, category.trim(), type, payload, userId, now, now));
    }));
  }

  public Mono<BankQuestion> update(Long id, String institutionId, String category, String type, String payload) {
    return validate(category, type).then(find(id, institutionId))
      .flatMap(q -> gateway.save(new BankQuestion(q.id(), q.institutionId(), category.trim(), type, payload,
        q.createdBy(), q.createdAt(), LocalDateTime.now())));
  }

  public Mono<Void> delete(Long id, String institutionId) {
    return find(id, institutionId).flatMap(q -> gateway.delete(q.id()));
  }

  private static Mono<Void> validate(String category, String type) {
    if (category == null || category.isBlank() || category.trim().length() > MAX_CATEGORY) {
      return Mono.error(new IllegalArgumentException("Category is required (max " + MAX_CATEGORY + " characters)"));
    }
    if (!TYPES.contains(type)) return Mono.error(new IllegalArgumentException("Unknown question type " + type));
    return Mono.empty();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public record Page(java.util.List<BankQuestion> items, long total) {
  }
}
