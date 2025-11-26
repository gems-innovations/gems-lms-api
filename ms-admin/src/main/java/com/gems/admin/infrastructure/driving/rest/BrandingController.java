package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.CreateBrandingUseCase;
import com.gems.admin.application.GetBrandingByCompanyIdUseCase;
import com.gems.admin.application.UpdateBrandingUseCase;
import com.gems.admin.application.command.BrandingCommand;
import com.gems.admin.application.response.BrandingResponse;
import com.gems.admin.infrastructure.driving.rest.mapper.BrandingMapper;
import com.gems.admin.infrastructure.driving.rest.request.BrandingRequest;
import com.gems.admin.infrastructure.driving.rest.response.ErrorResponse;
import com.gems.admin.infrastructure.driving.rest.schemas.BrandingResponseSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/branding")
@Tag(name = "Branding", description = "Company branding management endpoints")
public class BrandingController {
  private final CreateBrandingUseCase createBrandingUseCase;
  private final UpdateBrandingUseCase updateBrandingUseCase;
  private final GetBrandingByCompanyIdUseCase getBrandingByCompanyIdUseCase;

  public BrandingController(CreateBrandingUseCase createBrandingUseCase,
                            UpdateBrandingUseCase updateBrandingUseCase,
                            GetBrandingByCompanyIdUseCase getBrandingByCompanyIdUseCase) {
    this.createBrandingUseCase = createBrandingUseCase;
    this.updateBrandingUseCase = updateBrandingUseCase;
    this.getBrandingByCompanyIdUseCase = getBrandingByCompanyIdUseCase;
  }

  @PostMapping
  @Operation(
    summary = "Create new branding",
    description = "Creates a new branding configuration for a company. Requires authentication."
  )
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "201",
      description = "Branding successfully created",
      content = @Content(schema = @Schema(implementation = BrandingResponseSchema.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Invalid request data or validation errors",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
  })
  public Mono<ResponseEntity<BrandingResponse>> createBranding(@Valid @RequestBody BrandingRequest request) {
    BrandingCommand command = BrandingMapper.toCommand(request);
    return createBrandingUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PutMapping("/{companyId}")
  @Operation(
    summary = "Update branding",
    description = "Updates an existing branding configuration for a company. Requires authentication."
  )
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Branding successfully updated",
      content = @Content(schema = @Schema(implementation = BrandingResponseSchema.class))
    ),
    @ApiResponse(
      responseCode = "400",
      description = "Invalid request data or validation errors",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Branding not found for the specified company",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
  })
  public Mono<ResponseEntity<BrandingResponse>> updateBranding(
    @PathVariable String companyId,
    @Valid @RequestBody BrandingRequest request) {
    BrandingCommand command = BrandingMapper.toCommand(request);
    return updateBrandingUseCase.execute(companyId, command)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{companyId}")
  @Operation(
    summary = "Get branding by company ID",
    description = "Retrieves the branding configuration for a specific company"
  )
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Branding found",
      content = @Content(schema = @Schema(implementation = BrandingResponseSchema.class))
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Branding not found for the specified company",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
  })
  public Mono<ResponseEntity<BrandingResponse>> getBrandingByCompanyId(@PathVariable String companyId) {
    return getBrandingByCompanyIdUseCase.execute(companyId)
      .map(ResponseEntity::ok);
  }
}
