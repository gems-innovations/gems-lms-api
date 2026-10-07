package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.*;
import com.gems.admin.application.response.InstitutionListResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.infrastructure.driving.rest.mapper.InstitutionMapper;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionRequest;
import com.gems.admin.infrastructure.driving.rest.response.ErrorResponse;
import com.gems.admin.infrastructure.driving.rest.schemas.InstitutionResponseSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/institutions")
@Tag(name = "Institutions", description = "Institution management endpoints")
public class InstitutionController {
  private final CreateInstitutionUseCase createInstitutionUseCase;
  private final GetInstitutionByIdUseCase getInstitutionByIdUseCase;
  private final GetAllInstitutionsUseCase getAllInstitutionsUseCase;
  private final UpdateInstitutionUseCase updateInstitutionUseCase;
  private final DeleteInstitutionUseCase deleteInstitutionUseCase;

  public InstitutionController(CreateInstitutionUseCase createInstitutionUseCase,
                               GetInstitutionByIdUseCase getInstitutionByIdUseCase,
                               GetAllInstitutionsUseCase getAllInstitutionsUseCase,
                               UpdateInstitutionUseCase updateInstitutionUseCase,
                               DeleteInstitutionUseCase deleteInstitutionUseCase) {
    this.createInstitutionUseCase = createInstitutionUseCase;
    this.getInstitutionByIdUseCase = getInstitutionByIdUseCase;
    this.getAllInstitutionsUseCase = getAllInstitutionsUseCase;
    this.updateInstitutionUseCase = updateInstitutionUseCase;
    this.deleteInstitutionUseCase = deleteInstitutionUseCase;
  }

  @PostMapping
  @Operation(summary = "Create a new institution", description = "Creates a new educational or corporate institution. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Institution successfully created",
      content = @Content(schema = @Schema(implementation = InstitutionResponseSchema.class))),
    @ApiResponse(responseCode = "400", description = "Invalid request data or institution already exists",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<InstitutionResponse>> createInstitution(@Valid @RequestBody InstitutionRequest request) {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin can create institutions")
      .flatMap(caller -> createInstitutionUseCase.execute(InstitutionMapper.toCommand(request)))
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping
  @Operation(summary = "Get all institutions", description = "Lists registered institutions with optional search/status filters and pagination. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Institutions successfully retrieved"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<InstitutionListResponse>> getAllInstitutions(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int limit) {
    // Everyone but the super admin only gets their own institution.
    return CurrentUser.get().flatMap(caller -> caller.isSuperAdmin()
        ? getAllInstitutionsUseCase.execute(search, status, page, limit)
        : ownInstitution(caller))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get institution by ID", description = "Retrieves details of a specific institution. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Institution found",
      content = @Content(schema = @Schema(implementation = InstitutionResponseSchema.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Institution not found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<InstitutionResponse>> getInstitutionById(@PathVariable String id) {
    return CurrentUser.require(caller -> caller.belongsTo(id), "You can only see your institution")
      .flatMap(caller -> getInstitutionByIdUseCase.execute(id))
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an institution", description = "Updates details of an existing institution. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Institution successfully updated",
      content = @Content(schema = @Schema(implementation = InstitutionResponseSchema.class))),
    @ApiResponse(responseCode = "400", description = "Invalid request data",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Institution not found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<InstitutionResponse>> updateInstitution(
    @PathVariable String id,
    @Valid @RequestBody InstitutionRequest request) {
    return CurrentUser.require(caller -> caller.isSuperAdmin() || (caller.isAdmin() && caller.belongsTo(id)),
        "Only the super admin or the institution admin can edit it")
      .flatMap(caller -> {
        // Suspending or activating an institution is up to the super admin.
        if (!caller.isSuperAdmin()) request.setStatus(null);
        return updateInstitutionUseCase.execute(id, InstitutionMapper.toCommand(request));
      })
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an institution", description = "Deletes an existing institution. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Institution successfully deleted"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Institution not found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<Void>> deleteInstitution(@PathVariable String id) {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin can delete institutions")
      .flatMap(caller -> deleteInstitutionUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().build()));
  }

  private Mono<InstitutionListResponse> ownInstitution(AuthenticatedUser caller) {
    if (caller.institutionId() == null) {
      return Mono.just(new InstitutionListResponse(java.util.List.of(), 0, 1, 1, 0, false, false));
    }
    return getInstitutionByIdUseCase.execute(caller.institutionId())
      .map(inst -> new InstitutionListResponse(java.util.List.of(inst), 1, 1, 1, 1, false, false));
  }
}
