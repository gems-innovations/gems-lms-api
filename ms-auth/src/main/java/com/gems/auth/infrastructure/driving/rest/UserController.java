package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DeleteUserUseCase;
import com.gems.auth.application.GetAllUsersUseCase;
import com.gems.auth.application.GetUserByIdUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.ToggleUserStatusUseCase;
import com.gems.auth.application.UpdateUserUseCase;
import com.gems.auth.application.command.UpdateUserCommand;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.request.UpdateUserRequest;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import com.gems.shared.web.Paging;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Access rules: the super admin manages every user; an admin manages the non-super-admin
 * users of their institution; staff (admin, instructor) can list their institution; every
 * user can read and edit their own profile (but not their role or institution).
 */
@RestController
@RequestMapping(RestConstants.USERS_API_BASE_PATH)
public class UserController {
  private final DeleteUserUseCase deleteUserUseCase;
  private final GetUsersByInstitutionUseCase getUsersByInstitutionUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final GetAllUsersUseCase getAllUsersUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final ToggleUserStatusUseCase toggleUserStatusUseCase;

  public UserController(DeleteUserUseCase deleteUserUseCase,
                        GetUsersByInstitutionUseCase getUsersByInstitutionUseCase,
                        GetUserByIdUseCase getUserByIdUseCase,
                        GetAllUsersUseCase getAllUsersUseCase,
                        UpdateUserUseCase updateUserUseCase,
                        ToggleUserStatusUseCase toggleUserStatusUseCase) {
    this.deleteUserUseCase = deleteUserUseCase;
    this.getUsersByInstitutionUseCase = getUsersByInstitutionUseCase;
    this.getUserByIdUseCase = getUserByIdUseCase;
    this.getAllUsersUseCase = getAllUsersUseCase;
    this.updateUserUseCase = updateUserUseCase;
    this.toggleUserStatusUseCase = toggleUserStatusUseCase;
  }

  @GetMapping
  public Mono<ResponseEntity<Flux<UserResponse>>> getAllUsers() {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin can list every user")
      .map(caller -> ResponseEntity.ok(getAllUsersUseCase.execute()));
  }

  /**
   * Active users per institution. The super admin gets every institution; staff only theirs.
   * Declared before {@code /{id}} so "counts" is never read as an id.
   */
  @GetMapping("/counts")
  public Mono<ResponseEntity<Map<String, Long>>> countByInstitution() {
    return CurrentUser.require(AuthenticatedUser::isStaff, "Only staff can see user counts")
      .flatMap(caller -> getAllUsersUseCase.countByInstitution().map(counts -> caller.isSuperAdmin()
        ? counts
        : Map.of(caller.institutionId(), counts.getOrDefault(caller.institutionId(), 0L))))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable("id") Long id) {
    return CurrentUser.get()
      .flatMap(caller -> getUserByIdUseCase.execute(id)
        .flatMap(target -> caller.isUser(id) || (caller.isStaff() && caller.belongsTo(target.institutionId()))
          ? Mono.just(target)
          : Mono.<UserResponse>error(new ForbiddenException("You cannot see this user"))))
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<UserResponse>> updateUser(@PathVariable("id") Long id,
                                                       @Valid @RequestBody UpdateUserRequest request) {
    return CurrentUser.get()
      .flatMap(caller -> getUserByIdUseCase.execute(id).flatMap(target -> {
        boolean selfEdit = caller.isUser(id)
          && request.getRole().equalsIgnoreCase(target.role())
          && sameInstitution(request.getInstitutionId(), target.institutionId());
        if (!selfEdit) {
          ensureCanManage(caller, target);
          ensureCanAssign(caller, request.getRole(), request.getInstitutionId());
        }
        UpdateUserCommand command = new UpdateUserCommand(id, request.getFirstName(), request.getLastName(),
          request.getUsername(), request.getRole(), request.getInstitutionId(), request.getAvatarUrl());
        return updateUserUseCase.execute(command);
      }))
      .map(ResponseEntity::ok);
  }

  // PUT as well: browsers go through the gateway, whose CORS policy does not allow PATCH.
  @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
  public Mono<ResponseEntity<UserResponse>> toggleUserStatus(@PathVariable("id") Long id) {
    return manageable(id)
      .flatMap(target -> toggleUserStatusUseCase.execute(new UserId(id)))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteUser(@PathVariable("id") Long id) {
    return manageable(id)
      .flatMap(target -> deleteUserUseCase.execute(new UserId(id)))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }

  /** All the institution's users, or one page with {@code page} (total in X-Total-Count). */
  @GetMapping("/institution/{instId}")
  public Mono<ResponseEntity<List<UserResponse>>> getUsersByInstitution(
      @PathVariable("instId") String instId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer limit,
      @RequestParam(required = false) String search) {
    return CurrentUser.require(caller -> caller.isStaff() && caller.belongsTo(instId),
        "You can only list users of your institution")
      .flatMap(caller -> {
        if (page == null) return getUsersByInstitutionUseCase.execute(instId).collectList().map(ResponseEntity::ok);
        int size = Paging.limit(limit);
        return getUsersByInstitutionUseCase.search(instId, search, size, Paging.offset(page, size))
          .flatMap(result -> Paging.ok(result.users(), result.total()));
      });
  }

  /** Loads the target user if the caller may change it (and it is not the caller). */
  private Mono<UserResponse> manageable(Long id) {
    return CurrentUser.get().flatMap(caller -> {
      if (caller.isUser(id)) {
        return Mono.error(new ForbiddenException("You cannot change the status of your own account"));
      }
      return getUserByIdUseCase.execute(id).map(target -> {
        ensureCanManage(caller, target);
        return target;
      });
    });
  }

  static void ensureCanManage(AuthenticatedUser caller, UserResponse target) {
    if (caller.isSuperAdmin()) return;
    boolean adminOfTarget = caller.isAdmin()
      && caller.belongsTo(target.institutionId())
      && !AuthenticatedUser.SUPER_ADMIN.equalsIgnoreCase(target.role());
    if (!adminOfTarget) throw new ForbiddenException("You cannot manage this user");
  }

  /** Admins can only give non-super-admin roles inside their own institution. */
  static void ensureCanAssign(AuthenticatedUser caller, String role, String institutionId) {
    if (caller.isSuperAdmin()) return;
    if (!caller.isAdmin()) throw new ForbiddenException("Only administrators can manage users");
    if (AuthenticatedUser.SUPER_ADMIN.equalsIgnoreCase(role)) {
      throw new ForbiddenException("Only the super admin can grant the SUPER_ADMIN role");
    }
    if (!caller.belongsTo(institutionId)) {
      throw new ForbiddenException("You can only manage users of your institution");
    }
  }

  private static boolean sameInstitution(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }
}
