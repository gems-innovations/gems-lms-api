package com.gems.education.infrastructure.driven.auth;

import reactor.core.publisher.Mono;

import java.util.Collection;

/** Checks with ms-auth (the owner of user accounts) that users belong to an institution. */
public interface InstitutionMembers {
  /** Completes if every user is a member of the institution; otherwise errors with a 403. */
  Mono<Void> requireMembers(Collection<Long> userIds, String institutionId);
}
