package com.gems.auth.infrastructure.driven.postgresql;

/** Row of the active-users-per-institution query. */
public record InstitutionUserCount(String institutionId, Long total) {
}
