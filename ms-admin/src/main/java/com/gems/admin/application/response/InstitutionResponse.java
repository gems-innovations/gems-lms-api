package com.gems.admin.application.response;

import java.time.LocalDateTime;

public record InstitutionResponse(
  String id,
  String name,
  String type,
  String status,
  Integer usersCount,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  InstitutionMetadataResponse metadata
) {
}
