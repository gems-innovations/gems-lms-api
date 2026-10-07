package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.GroupGateway;
import com.gems.education.domain.entities.Group;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Repository
public class GroupRepositoryAdapter implements GroupGateway {
  private final IGroupRepository repository;

  public GroupRepositoryAdapter(IGroupRepository repository) {
    this.repository = repository;
  }

  @Override
  public Mono<Group> save(Group group) {
    GroupEntity e = new GroupEntity();
    e.setId(group.id());
    e.setName(group.name());
    e.setInstitutionId(group.institutionId());
    e.setInstructorId(group.instructorId());
    e.setStudentIds(group.studentIds().toArray(Long[]::new));
    e.setCourseIds(group.courseIds().toArray(Long[]::new));
    e.setPathIds(group.pathIds().toArray(Long[]::new));
    e.setCreatedAt(group.createdAt());
    return repository.save(e).map(this::toDomain);
  }

  @Override
  public Mono<Group> findById(Long id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Flux<Group> findAll() {
    return repository.findAll().map(this::toDomain);
  }

  @Override
  public Flux<Group> findByInstitution(String institutionId) {
    return repository.findByInstitutionIdOrderByIdAsc(institutionId).map(this::toDomain);
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return repository.deleteById(id);
  }

  private Group toDomain(GroupEntity e) {
    return new Group(e.getId(), e.getName(), e.getInstitutionId(), e.getInstructorId(),
      list(e.getStudentIds()), list(e.getCourseIds()), list(e.getPathIds()), e.getCreatedAt());
  }

  private static List<Long> list(Long[] ids) {
    return ids == null ? List.of() : Arrays.asList(ids);
  }
}
