package com.gems.education.application;

import com.gems.education.application.exceptions.GroupNotFoundException;
import com.gems.education.application.gateway.GroupGateway;
import com.gems.education.domain.entities.Group;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Cohorts: create, read, partially update and delete. */
public class GroupUseCase {
  private static final String DEFAULT_NAME = "Nuevo grupo";

  private final GroupGateway groupGateway;

  public GroupUseCase(GroupGateway groupGateway) {
    this.groupGateway = groupGateway;
  }

  public Mono<Group> get(Long id) {
    return groupGateway.findById(id)
      .switchIfEmpty(Mono.error(new GroupNotFoundException(id)));
  }

  public Flux<Group> ofInstitution(String institutionId) {
    return groupGateway.findByInstitution(institutionId);
  }

  public Flux<Group> all() {
    return groupGateway.findAll();
  }

  public Mono<Group> create(String name, String institutionId, Long instructorId,
                            List<Long> studentIds, List<Long> courseIds, List<Long> pathIds) {
    return groupGateway.save(new Group(null, nameOr(name, DEFAULT_NAME), institutionId, instructorId,
      distinct(studentIds), distinct(courseIds), distinct(pathIds), LocalDateTime.now()));
  }

  /**
   * Applies the non-null fields of the change; the institution never changes.
   * {@code clearInstructor} unassigns the instructor (a null instructorId alone means "unchanged").
   */
  public Mono<Group> update(Long id, String name, Long instructorId, boolean clearInstructor,
                            List<Long> studentIds, List<Long> courseIds, List<Long> pathIds) {
    return get(id).flatMap(g -> groupGateway.save(new Group(g.id(),
      name == null ? g.name() : nameOr(name, g.name()),
      g.institutionId(),
      clearInstructor ? null : (instructorId == null ? g.instructorId() : instructorId),
      studentIds == null ? g.studentIds() : distinct(studentIds),
      courseIds == null ? g.courseIds() : distinct(courseIds),
      pathIds == null ? g.pathIds() : distinct(pathIds),
      g.createdAt())));
  }

  public Mono<Void> delete(Long id) {
    return get(id).flatMap(g -> groupGateway.deleteById(g.id()));
  }

  private static String nameOr(String name, String fallback) {
    return name == null || name.isBlank() ? fallback : name.trim();
  }

  private static List<Long> distinct(List<Long> ids) {
    return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
  }
}
