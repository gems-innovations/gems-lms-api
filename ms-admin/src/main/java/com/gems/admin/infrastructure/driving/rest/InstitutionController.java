package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.*;
import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.infrastructure.driving.rest.mapper.InstitutionMapper;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionRequest;
import com.gems.admin.infrastructure.driving.rest.response.ErrorResponse;
import com.gems.admin.infrastructure.driving.rest.schemas.InstitutionResponseSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import reactor.core.publisher.Flux;
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
    InstitutionCommand command = InstitutionMapper.toCommand(request);
    return createInstitutionUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping
  @Operation(summary = "Get all institutions", description = "Lists all registered institutions. Requires authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Institutions successfully retrieved",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = InstitutionResponseSchema.class)))),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal server error",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public Mono<ResponseEntity<Flux<InstitutionResponse>>> getAllInstitutions() {
    Flux<InstitutionResponse> institutions = getAllInstitutionsUseCase.execute();
    return Mono.just(ResponseEntity.ok(institutions));
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
    return getInstitutionByIdUseCase.execute(id)
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
    InstitutionCommand command = InstitutionMapper.toCommand(request);
    return updateInstitutionUseCase.execute(id, command)
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
    return deleteInstitutionUseCase.execute(id)
      .then(Mono.just(ResponseEntity.noContent().build()));
  }
}
