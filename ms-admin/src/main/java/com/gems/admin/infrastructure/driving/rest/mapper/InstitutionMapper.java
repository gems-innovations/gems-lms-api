package com.gems.admin.infrastructure.driving.rest.mapper;

import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.command.InstitutionMetadataCommand;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionMetadataRequest;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionRequest;

public class InstitutionMapper {

  private InstitutionMapper() {
  }

  public static InstitutionCommand toCommand(InstitutionRequest request) {
    InstitutionMetadataCommand metadataCommand = null;
    if (request.getMetadata() != null) {
      InstitutionMetadataRequest metaReq = request.getMetadata();
      metadataCommand = new InstitutionMetadataCommand(
        metaReq.getDescription(),
        metaReq.getWebsite(),
        metaReq.getContactEmail(),
        metaReq.getPhoneNumber(),
        metaReq.getAddress(),
        metaReq.getSubscriptionType(),
        metaReq.getMaxUsers()
      );
    }
    return new InstitutionCommand(
      request.getId(),
      request.getName(),
      request.getType(),
      request.getStatus(),
      metadataCommand
    );
  }
}
