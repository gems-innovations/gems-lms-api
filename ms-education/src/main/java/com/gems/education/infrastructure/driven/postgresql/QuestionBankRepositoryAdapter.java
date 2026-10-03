package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.QuestionBankGateway;
import com.gems.education.domain.entities.BankQuestion;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class QuestionBankRepositoryAdapter implements QuestionBankGateway {
  private static final String COLUMNS = "id, institution_id, category, type, payload, created_by, created_at, updated_at";
  private static final String FILTER = "institution_id = :institution "
    + "AND (CAST(:category AS VARCHAR) IS NULL OR category = :category) "
    + "AND (CAST(:search AS VARCHAR) IS NULL OR payload ILIKE '%' || :search || '%')";

  private final DatabaseClient db;

  public QuestionBankRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Flux<BankQuestion> find(String institutionId, String category, String search, int offset, int limit) {
    return filtered("SELECT " + COLUMNS + " FROM question_bank WHERE " + FILTER
        + " ORDER BY updated_at DESC, id DESC LIMIT :limit OFFSET :offset", institutionId, category, search)
      .bind("limit", limit).bind("offset", offset)
      .map((row, meta) -> map(row)).all();
  }

  @Override
  public Mono<Long> count(String institutionId, String category, String search) {
    return filtered("SELECT COUNT(*) AS total FROM question_bank WHERE " + FILTER, institutionId, category, search)
      .map((row, meta) -> row.get("total", Long.class)).one();
  }

  @Override
  public Mono<BankQuestion> findById(Long id) {
    return db.sql("SELECT " + COLUMNS + " FROM question_bank WHERE id = :id").bind("id", id)
      .map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<BankQuestion> save(BankQuestion q) {
    DatabaseClient.GenericExecuteSpec spec = q.id() == null
      ? db.sql("INSERT INTO question_bank(institution_id, category, type, payload, created_by, created_at, updated_at) "
          + "VALUES (:institution, :category, :type, :payload, :createdBy, :createdAt, :updatedAt) RETURNING " + COLUMNS)
      : db.sql("UPDATE question_bank SET category = :category, type = :type, payload = :payload, updated_at = :updatedAt "
          + "WHERE id = :id RETURNING " + COLUMNS).bind("id", q.id());
    if (q.id() == null) {
      spec = spec.bind("institution", q.institutionId()).bind("createdAt", q.createdAt());
      spec = q.createdBy() == null ? spec.bindNull("createdBy", Long.class) : spec.bind("createdBy", q.createdBy());
    }
    return spec.bind("category", q.category()).bind("type", q.type()).bind("payload", q.payload())
      .bind("updatedAt", q.updatedAt())
      .map((row, meta) -> map(row)).one();
  }

  @Override
  public Mono<Void> delete(Long id) {
    return db.sql("DELETE FROM question_bank WHERE id = :id").bind("id", id).then();
  }

  @Override
  public Flux<CategoryCount> categories(String institutionId) {
    return db.sql("SELECT category, COUNT(*) AS total FROM question_bank WHERE institution_id = :institution "
        + "GROUP BY category ORDER BY category")
      .bind("institution", institutionId)
      .map((row, meta) -> new CategoryCount(row.get("category", String.class), row.get("total", Long.class)))
      .all();
  }

  @Override
  public Flux<BankQuestion> draw(String institutionId, String category, int count) {
    return db.sql("SELECT " + COLUMNS + " FROM question_bank WHERE institution_id = :institution AND category = :category "
        + "ORDER BY random() LIMIT :count")
      .bind("institution", institutionId).bind("category", category).bind("count", count)
      .map((row, meta) -> map(row)).all();
  }

  @Override
  public Flux<BankQuestion> findAll(String institutionId, List<Long> ids) {
    if (ids.isEmpty()) return Flux.empty();
    return db.sql("SELECT " + COLUMNS + " FROM question_bank WHERE institution_id = :institution AND id = ANY(:ids)")
      .bind("institution", institutionId).bind("ids", ids.toArray(Long[]::new))
      .map((row, meta) -> map(row)).all();
  }

  private DatabaseClient.GenericExecuteSpec filtered(String sql, String institutionId, String category, String search) {
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("institution", institutionId);
    spec = category == null ? spec.bindNull("category", String.class) : spec.bind("category", category);
    return search == null ? spec.bindNull("search", String.class) : spec.bind("search", search);
  }

  private static BankQuestion map(Row row) {
    return new BankQuestion(row.get("id", Long.class), row.get("institution_id", String.class),
      row.get("category", String.class), row.get("type", String.class), row.get("payload", String.class),
      row.get("created_by", Long.class), row.get("created_at", LocalDateTime.class),
      row.get("updated_at", LocalDateTime.class));
  }
}
