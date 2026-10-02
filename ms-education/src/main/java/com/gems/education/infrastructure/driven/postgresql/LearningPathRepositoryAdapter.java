package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.LearningPathStep;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class LearningPathRepositoryAdapter implements LearningPathGateway {
  private final ILearningPathRepository learningPathRepository;
  private final ILearningPathCourseRepository learningPathCourseRepository;
  private final CourseGateway courseGateway;
  private final IPathEnrollmentRepository pathEnrollmentRepository;

  public LearningPathRepositoryAdapter(ILearningPathRepository learningPathRepository,
                                       ILearningPathCourseRepository learningPathCourseRepository,
                                       CourseGateway courseGateway,
                                       IPathEnrollmentRepository pathEnrollmentRepository) {
    this.learningPathRepository = learningPathRepository;
    this.learningPathCourseRepository = learningPathCourseRepository;
    this.courseGateway = courseGateway;
    this.pathEnrollmentRepository = pathEnrollmentRepository;
  }

  @Override
  public Mono<LearningPath> save(LearningPath learningPath) {
    LocalDateTime createdAt = learningPath.getCreatedAt() != null ? learningPath.getCreatedAt() : LocalDateTime.now();
    LearningPathEntity entity = new LearningPathEntity(
      learningPath.getId(),
      learningPath.getTitle(),
      learningPath.getDescription(),
      learningPath.getInstitutionId(),
      createdAt
    );
    entity.setStatus(learningPath.getStatus() != null ? learningPath.getStatus() : LearningPath.DRAFT);
    entity.setTags(joinTags(learningPath.getTags()));
    entity.setThumbnailUrl(learningPath.getThumbnailUrl());
    entity.setUpdatedAt(LocalDateTime.now());
    List<Course> courses = learningPath.getCourses() == null ? List.of() : learningPath.getCourses();

    return learningPathRepository.save(entity)
      .flatMap(savedPath -> {
        Mono<Void> cleanUp = learningPath.getId() != null
          ? learningPathCourseRepository.deleteByLearningPathId(savedPath.getId())
          : Mono.empty();
        return cleanUp.then(
          Flux.fromIterable(courses)
            .index()
            .concatMap(tuple -> {
              Course course = tuple.getT2();
              LearningPathStep step = learningPath.stepFor(course.getId());
              LearningPathCourseEntity lpcEntity =
                new LearningPathCourseEntity(savedPath.getId(), course.getId(), tuple.getT1().intValue());
              lpcEntity.setIsRequired(step.required());
              lpcEntity.setMinimumScore(step.minimumScore());
              return learningPathCourseRepository.save(lpcEntity).then(courseGateway.findById(course.getId()));
            })
            .collectList()
            .flatMap(saved -> withEnrolledCount(mapToDomain(savedPath, saved, steps(courses, learningPath)))));
      });
  }

  @Override
  public Mono<LearningPath> findById(Long id) {
    return learningPathRepository.findById(id)
      .flatMap(this::loadFullLearningPath);
  }

  @Override
  public Flux<LearningPath> findByInstitutionId(String institutionId) {
    return learningPathRepository.findByInstitutionId(institutionId)
      .flatMap(this::loadFullLearningPath);
  }

  @Override
  public Flux<LearningPath> findAll() {
    return learningPathRepository.findAll()
      .flatMap(this::loadFullLearningPath);
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return learningPathRepository.deleteById(id);
  }

  private Mono<LearningPath> loadFullLearningPath(LearningPathEntity entity) {
    return learningPathCourseRepository.findByLearningPathId(entity.getId())
      .collectList()
      .flatMap(relations -> {
        relations.sort(Comparator.comparingInt(LearningPathCourseEntity::getOrderIndex));
        List<LearningPathStep> steps = relations.stream()
          .map(rel -> new LearningPathStep(rel.getCourseId(), !Boolean.FALSE.equals(rel.getIsRequired()), rel.getMinimumScore()))
          .toList();
        return Flux.fromIterable(relations)
          .concatMap(rel -> courseGateway.findById(rel.getCourseId()))
          .collectList()
          .flatMap(courses -> withEnrolledCount(mapToDomain(entity, courses, steps)));
      });
  }

  private Mono<LearningPath> withEnrolledCount(LearningPath path) {
    return pathEnrollmentRepository.countByLearningPathId(path.getId())
      .map(count -> {
        path.setEnrolledCount(count);
        return path;
      });
  }

  private static List<LearningPathStep> steps(List<Course> courses, LearningPath path) {
    return courses.stream().map(c -> path.stepFor(c.getId())).toList();
  }

  private LearningPath mapToDomain(LearningPathEntity entity, List<Course> courses, List<LearningPathStep> steps) {
    LearningPath path = new LearningPath(
      entity.getId(),
      entity.getTitle(),
      entity.getDescription(),
      entity.getInstitutionId(),
      entity.getCreatedAt(),
      courses
    );
    path.setStatus(entity.getStatus() != null ? entity.getStatus() : LearningPath.PUBLISHED);
    path.setTags(splitTags(entity.getTags()));
    path.setThumbnailUrl(entity.getThumbnailUrl());
    path.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt());
    path.setSteps(steps);
    return path;
  }

  private static List<String> splitTags(String tags) {
    if (tags == null || tags.isBlank()) return new ArrayList<>();
    return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty())
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private static String joinTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) return null;
    return tags.stream().map(String::trim).filter(s -> !s.isEmpty() && !s.contains(",")).distinct()
      .collect(Collectors.joining(","));
  }
}
