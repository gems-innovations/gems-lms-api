package com.gems.admin.application.response;

import java.time.LocalDateTime;

public record InstitutionMetadataResponse(
  String institutionId,
  String description,
  String website,
  String contactEmail,
  String phoneNumber,
  String address,
  String subscriptionType,
  Integer maxUsers,
  LocalDateTime lastActivity
) {
}
