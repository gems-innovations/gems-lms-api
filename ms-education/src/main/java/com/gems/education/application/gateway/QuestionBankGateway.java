package com.gems.education.application.gateway;

import com.gems.education.domain.entities.BankQuestion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface QuestionBankGateway {
  /** category and search are optional filters; newest first. */
  Flux<BankQuestion> find(String institutionId, String category, String search, int offset, int limit);

  Mono<Long> count(String institutionId, String category, String search);

  Mono<BankQuestion> findById(Long id);

  Mono<BankQuestion> save(BankQuestion question);

  Mono<Void> delete(Long id);

  /** Categories in use with their number of questions. */
  Flux<CategoryCount> categories(String institutionId);

  /** count random questions of the category. */
  Flux<BankQuestion> draw(String institutionId, String category, int count);

  /** The given questions of the institution (missing ids are skipped). */
  Flux<BankQuestion> findAll(String institutionId, List<Long> ids);

  record CategoryCount(String category, long count) {
  }
}
