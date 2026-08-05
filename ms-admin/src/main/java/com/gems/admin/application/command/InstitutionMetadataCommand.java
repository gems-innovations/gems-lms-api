package com.gems.admin.application.command;

public record InstitutionMetadataCommand(
  String description,
  String website,
  String contactEmail,
  String phoneNumber,
  String address,
  String subscriptionType,
  Integer maxUsers
) {
}
