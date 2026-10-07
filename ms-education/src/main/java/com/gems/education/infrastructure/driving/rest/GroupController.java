package com.gems.education.infrastructure.driving.rest;

import com.gems.education.infrastructure.driven.auth.InstitutionMembers;
import com.gems.education.application.GroupUseCase;
import com.gems.education.domain.entities.Group;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cohorts of an institution. Only its staff (admins and instructors) see and manage them;
 * the courses and paths a group is linked to must belong to the same institution.
 */
@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
  private final GroupUseCase groupUseCase;
  private final EducationAccess access;
  private final InstitutionMembers members;

  public GroupController(GroupUseCase groupUseCase, EducationAccess access, InstitutionMembers members) {
    this.groupUseCase = groupUseCase;
    this.access = access;
    this.members = members;
  }

  /** Groups of the caller's institution; the super admin sees all, or one institution's with the param. */
  @GetMapping
  public Mono<ResponseEntity<List<GroupResponse>>> list(@RequestParam(required = false) String institutionId) {
    return access.staff().flatMap(caller -> {
      Flux<Group> groups;
      if (caller.isSuperAdmin()) {
        groups = institutionId == null ? groupUseCase.all() : groupUseCase.ofInstitution(institutionId);
      } else {
        groups = groupUseCase.ofInstitution(caller.institutionId());
      }
      return groups.map(GroupResponse::from).collectList().map(ResponseEntity::ok);
    });
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<GroupResponse>> get(@PathVariable Long id) {
    return manageable(id).map(g -> ResponseEntity.ok(GroupResponse.from(g)));
  }

  @PostMapping
  public Mono<ResponseEntity<GroupResponse>> create(@RequestBody GroupRequest request) {
    return CurrentUser.get().flatMap(caller -> {
      String institutionId = request.institutionId() != null ? request.institutionId() : caller.institutionId();
      if (institutionId == null) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "institutionId is required"));
      }
      return access.staffOf(institutionId)
        .then(checkTargets(request.courseIds(), request.pathIds()))
        .then(Mono.defer(() -> members.requireMembers(people(request), institutionId)))
        .then(Mono.defer(() -> groupUseCase.create(request.name(), institutionId, request.instructorId(),
          request.studentIds(), request.courseIds(), request.pathIds())));
    }).map(g -> ResponseEntity.status(HttpStatus.CREATED).body(GroupResponse.from(g)));
  }

  /** Partial update: fields left out stay as they are; {@code clearInstructor} unassigns the instructor. */
  @PutMapping("/{id}")
  public Mono<ResponseEntity<GroupResponse>> update(@PathVariable Long id, @RequestBody GroupRequest request) {
    return manageable(id)
      .flatMap(g -> request.institutionId() != null && !request.institutionId().equals(g.institutionId())
        ? Mono.error(new ForbiddenException("A group cannot move to another institution"))
        : Mono.just(g))
      .flatMap(g -> checkTargets(request.courseIds(), request.pathIds())
        .then(Mono.defer(() -> members.requireMembers(people(request), g.institutionId()))))
      .then(Mono.defer(() -> groupUseCase.update(id, request.name(), request.instructorId(),
        Boolean.TRUE.equals(request.clearInstructor()),
        request.studentIds(), request.courseIds(), request.pathIds())))
      .map(g -> ResponseEntity.ok(GroupResponse.from(g)));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
    return manageable(id).then(Mono.defer(() -> groupUseCase.delete(id))).thenReturn(ResponseEntity.noContent().build());
  }

  private Mono<Group> manageable(Long id) {
    return CurrentUser.get().flatMap(caller -> groupUseCase.get(id).flatMap(g -> canManage(caller, g)
      ? Mono.just(g)
      : Mono.error(new ForbiddenException("You cannot manage this group"))));
  }

  private static boolean canManage(AuthenticatedUser caller, Group group) {
    return caller.isStaff() && caller.belongsTo(group.institutionId());
  }

  /** Every linked course and path must be editable by the caller (same institution). */
  private Mono<Void> checkTargets(List<Long> courseIds, List<Long> pathIds) {
    return Flux.fromIterable(courseIds == null ? List.of() : courseIds).concatMap(access::editableCourse)
      .thenMany(Flux.fromIterable(pathIds == null ? List.of() : pathIds).concatMap(access::editablePath))
      .then();
  }

  /** The students and the instructor named in the request (they must belong to the institution). */
  private static List<Long> people(GroupRequest request) {
    List<Long> ids = new java.util.ArrayList<>(request.studentIds() == null ? List.of() : request.studentIds());
    if (request.instructorId() != null) ids.add(request.instructorId());
    return ids;
  }

  public record GroupRequest(String name, String institutionId, Long instructorId, Boolean clearInstructor,
                             List<Long> studentIds, List<Long> courseIds, List<Long> pathIds) {
  }

  public record GroupResponse(Long id, String name, String institutionId, Long instructorId,
                              List<Long> studentIds, List<Long> courseIds, List<Long> pathIds,
                              LocalDateTime createdAt) {
    static GroupResponse from(Group g) {
      return new GroupResponse(g.id(), g.name(), g.institutionId(), g.instructorId(),
        g.studentIds(), g.courseIds(), g.pathIds(), g.createdAt());
    }
  }
}
