package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.ConsentGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class ConsentRepositoryAdapter implements ConsentGateway {
  private final DatabaseClient db;

  public ConsentRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> record(Long userId, String kind, String policyVersion, boolean granted) {
    return db.sql("INSERT INTO consents (user_id, kind, policy_version, granted) VALUES (:userId, :kind, :version, :granted)")
      .bind("userId", userId).bind("kind", kind).bind("version", policyVersion).bind("granted", granted)
      .then();
  }
}
