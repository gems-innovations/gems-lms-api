package com.gems.admin.application.response;

import java.util.List;

public record InstitutionListResponse(
  List<InstitutionResponse> institutions,
  long total,
  int page,
  int limit,
  int totalPages,
  boolean hasNext,
  boolean hasPrevious
) {
}
