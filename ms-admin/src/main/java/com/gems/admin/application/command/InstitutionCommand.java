package com.gems.admin.application.command;

public record InstitutionCommand(
  String id,
  String name,
  String type,
  String status,
  InstitutionMetadataCommand metadata
) {
}
