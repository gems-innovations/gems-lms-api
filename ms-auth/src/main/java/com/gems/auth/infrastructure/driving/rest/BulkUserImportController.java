package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.auth.infrastructure.driving.rest.mapper.UserMapper;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alta masiva de usuarios (importación CSV desde el front). Cada fila se valida y se crea por
 * separado: una fila con error no detiene el lote y la respuesta dice qué pasó con cada una.
 * Las cuentas reciben una contraseña temporal que deben cambiar al entrar.
 */
@RestController
@RequestMapping(AuthInfraConstants.AUTH_API_BASE_PATH)
@Tag(name = "Authentication")
@SecurityRequirement(name = "bearerAuth")
public class BulkUserImportController {
  static final int MAX_ROWS = 2000;

  private final RegisterUserUseCase registerUserUseCase;
  private final Validator validator;

  public BulkUserImportController(RegisterUserUseCase registerUserUseCase, Validator validator) {
    this.registerUserUseCase = registerUserUseCase;
    this.validator = validator;
  }

  public record BulkRegisterRequest(List<RegisterUserRequest> users, Boolean dryRun) {}

  /** created | exists | invalid | forbidden | duplicate. temporaryPassword solo en created. */
  public record RowResult(int row, String email, String status, String message, Long userId, String temporaryPassword) {}

  public record BulkRegisterResponse(int total, int created, int failed, boolean dryRun, List<RowResult> rows) {}

  @PostMapping(AuthInfraConstants.REGISTER_ENDPOINT + "/bulk")
  @Operation(summary = "Bulk user import",
    description = "Creates up to 2000 users. Every row is processed on its own; dryRun only validates.")
  public Mono<ResponseEntity<BulkRegisterResponse>> importUsers(@RequestBody BulkRegisterRequest request) {
    List<RegisterUserRequest> users = request.users() == null ? List.of() : request.users();
    if (users.isEmpty() || users.size() > MAX_ROWS) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Send between 1 and " + MAX_ROWS + " users"));
    }
    boolean dryRun = Boolean.TRUE.equals(request.dryRun());
    Set<String> seen = new HashSet<>();

    return CurrentUser.get().flatMap(caller -> Flux.range(0, users.size())
      .concatMap(i -> {
        RegisterUserRequest row = users.get(i);
        int rowNumber = i + 1;
        String email = row.getEmail() == null ? "" : row.getEmail().trim().toLowerCase(Locale.ROOT);
        row.setEmail(email);
        if (caller.isAdmin() && row.getInstitutionId() == null) row.setInstitutionId(caller.institutionId());

        String invalid = violations(row);
        if (invalid != null) return Mono.just(result(rowNumber, email, "invalid", invalid));
        if (!seen.add(email)) return Mono.just(result(rowNumber, email, "duplicate", "Repetido en el archivo"));
        try {
          UserController.ensureCanAssign(caller, row.getRole(), row.getInstitutionId());
        } catch (ForbiddenException e) {
          return Mono.just(result(rowNumber, email, "forbidden", e.getMessage()));
        }
        if (dryRun) return Mono.just(result(rowNumber, email, "valid", null));

        return registerUserUseCase.execute(UserMapper.toDomain(row))
          .map(created -> new RowResult(rowNumber, email, "created", null, created.userId(), created.temporaryPassword()))
          .onErrorResume(UserAlreadyExistsException.class, e -> Mono.just(result(rowNumber, email, "exists", "Ya existe una cuenta con este correo")))
          .onErrorResume(IllegalArgumentException.class, e -> Mono.just(result(rowNumber, email, "invalid", e.getMessage())));
      })
      .collectList()
      .map(rows -> {
        int created = (int) rows.stream().filter(r -> "created".equals(r.status()) || "valid".equals(r.status())).count();
        return ResponseEntity.ok(new BulkRegisterResponse(rows.size(), created, rows.size() - created, dryRun, rows));
      }));
  }

  private String violations(RegisterUserRequest row) {
    Set<ConstraintViolation<RegisterUserRequest>> found = validator.validate(row);
    if (found.isEmpty()) return null;
    return found.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().collect(Collectors.joining("; "));
  }

  private static RowResult result(int row, String email, String status, String message) {
    return new RowResult(row, email, status, message, null, null);
  }
}
