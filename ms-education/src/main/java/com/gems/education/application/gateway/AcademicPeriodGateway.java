package com.gems.education.application.gateway;

import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.PeriodGradeRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface AcademicPeriodGateway {
  /** Newest first. */
  Flux<AcademicPeriod> findPeriodsOf(String institutionId);

  Mono<AcademicPeriod> findById(Long id);

  Mono<AcademicPeriod> save(AcademicPeriod period);

  Mono<Void> delete(Long id);

  /** Marks the period closed, or reopens it with null values. */
  Mono<AcademicPeriod> setClosed(Long id, LocalDateTime closedAt, Long closedBy);

  // ── Records of a closed period (the acta) ──────────────────────────────────
  Mono<Void> replaceRecords(Long periodId, List<PeriodGradeRecord> records);

  Flux<PeriodGradeRecord> findRecords(Long periodId);

  Mono<Void> deleteRecords(Long periodId);
}
