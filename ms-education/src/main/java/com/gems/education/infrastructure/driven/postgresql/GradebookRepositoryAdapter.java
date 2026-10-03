package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.GradebookGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Repository
public class GradebookRepositoryAdapter implements GradebookGateway {
  private final DatabaseClient db;
  private final TransactionalOperator tx;

  public GradebookRepositoryAdapter(DatabaseClient db, TransactionalOperator tx) {
    this.db = db;
    this.tx = tx;
  }

  @Override
  public Mono<Map<Long, Integer>> weights(Long courseId) {
    return db.sql("SELECT block_id, weight FROM gradebook_weights WHERE course_id = :course")
      .bind("course", courseId)
      .map((row, meta) -> Map.entry(row.get("block_id", Long.class), row.get("weight", Integer.class)))
      .all()
      .collect(HashMap<Long, Integer>::new, (map, e) -> map.put(e.getKey(), e.getValue()));
  }

  @Override
  public Mono<Void> saveWeights(Long courseId, Map<Long, Integer> weights) {
    return db.sql("DELETE FROM gradebook_weights WHERE course_id = :course").bind("course", courseId).then()
      .thenMany(Flux.fromIterable(weights.entrySet())
        .concatMap(w -> db.sql("INSERT INTO gradebook_weights(course_id, block_id, weight) VALUES (:course, :block, :weight)")
          .bind("course", courseId).bind("block", w.getKey()).bind("weight", w.getValue()).then()))
      .then()
      .as(tx::transactional);
  }
}
