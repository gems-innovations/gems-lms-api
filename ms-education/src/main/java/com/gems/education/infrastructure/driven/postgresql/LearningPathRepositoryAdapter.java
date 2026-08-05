package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class LearningPathRepositoryAdapter implements LearningPathGateway {
  private final ILearningPathRepository learningPathRepository;
  private final ILearningPathCourseRepository learningPathCourseRepository;
  private final CourseGateway courseGateway;

  public LearningPathRepositoryAdapter(ILearningPathRepository learningPathRepository,
                                       ILearningPathCourseRepository learningPathCourseRepository,
                                       CourseGateway courseGateway) {
    this.learningPathRepository = learningPathRepository;
    this.learningPathCourseRepository = learningPathCourseRepository;
    this.courseGateway = courseGateway;
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

    return learningPathRepository.save(entity)
      .flatMap(savedPath -> {
        if (learningPath.getCourses() == null || learningPath.getCourses().isEmpty()) {
          return Mono.just(mapToDomain(savedPath, new ArrayList<>()));
        }

        Mono<Void> cleanUp = Mono.empty();
        if (learningPath.getId() != null) {
          cleanUp = learningPathCourseRepository.deleteByLearningPathId(savedPath.getId());
        }

        return cleanUp.then(
          Flux.just(learningPath.getCourses())
            .flatMap(Flux::fromIterable)
            .index() // (index, Course)
            .flatMap(tuple -> {
              int orderIndex = tuple.getT1().intValue();
              Course course = tuple.getT2();
              LearningPathCourseEntity lpcEntity = new LearningPathCourseEntity(savedPath.getId(), course.getId(), orderIndex);
              return learningPathCourseRepository.save(lpcEntity)
                .then(courseGateway.findById(course.getId()));
            })
            .collectList()
            .map(courses -> mapToDomain(savedPath, courses))
        );
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
  public Mono<Void> deleteById(Long id) {
    return learningPathRepository.deleteById(id);
  }

  private Mono<LearningPath> loadFullLearningPath(LearningPathEntity entity) {
    return learningPathCourseRepository.findByLearningPathId(entity.getId())
      .collectList()
      .flatMap(relations -> {
        relations.sort(Comparator.comparingInt(LearningPathCourseEntity::getOrderIndex));
        return Flux.fromIterable(relations)
          .flatMap(rel -> courseGateway.findById(rel.getCourseId()))
          .collectList()
          .map(courses -> mapToDomain(entity, courses));
      });
  }

  private LearningPath mapToDomain(LearningPathEntity entity, List<Course> courses) {
    return new LearningPath(
      entity.getId(),
      entity.getTitle(),
      entity.getDescription(),
      entity.getInstitutionId(),
      entity.getCreatedAt(),
      courses
    );
  }
}
