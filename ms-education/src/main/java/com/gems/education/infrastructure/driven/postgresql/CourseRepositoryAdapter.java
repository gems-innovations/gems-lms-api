package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class CourseRepositoryAdapter implements CourseGateway {
  private final ICourseRepository courseRepository;
  private final IModuleRepository moduleRepository;
  private final ILessonRepository lessonRepository;
  private final IContentRepository contentRepository;
  private final R2dbcEntityTemplate template;

  public CourseRepositoryAdapter(ICourseRepository courseRepository,
                                 IModuleRepository moduleRepository,
                                 ILessonRepository lessonRepository,
                                 IContentRepository contentRepository,
                                 R2dbcEntityTemplate template) {
    this.courseRepository = courseRepository;
    this.moduleRepository = moduleRepository;
    this.lessonRepository = lessonRepository;
    this.contentRepository = contentRepository;
    this.template = template;
  }

  @Override
  public Mono<Course> save(Course course) {
    CourseEntity courseEntity = mapToEntity(course);
    return courseRepository.save(courseEntity)
      .flatMap(savedCourse -> {
        if (course.getModules() == null || course.getModules().isEmpty()) {
          return Mono.just(mapToDomain(savedCourse, new ArrayList<>()));
        }

        // Clean up hierarchy on update to write updated list of modules/lessons
        Mono<Void> cleanUp = Mono.empty();
        if (course.getId() != null) {
          cleanUp = moduleRepository.findByCourseId(savedCourse.getId())
            .flatMap(existingModule -> 
              lessonRepository.findByModuleId(existingModule.getId())
                .flatMap(existingLesson -> 
                  contentRepository.findByLessonId(existingLesson.getId())
                    .flatMap(existingContent -> contentRepository.deleteById(existingContent.getId()))
                    .then(lessonRepository.deleteById(existingLesson.getId()))
                )
                .then(moduleRepository.deleteById(existingModule.getId()))
            )
            .then();
        }

        return cleanUp.then(
          Flux.fromIterable(course.getModules())
            .flatMap(module -> {
              ModuleEntity moduleEntity = new ModuleEntity(null, savedCourse.getId(), module.getTitle(), module.getOrderIndex(), java.time.LocalDateTime.now());
              return moduleRepository.save(moduleEntity)
                .flatMap(savedModule -> {
                  if (module.getLessons() == null || module.getLessons().isEmpty()) {
                    return Mono.just(new Module(savedModule.getId(), savedModule.getCourseId(), savedModule.getTitle(), savedModule.getOrderIndex(), new ArrayList<>()));
                  }
                  return Flux.fromIterable(module.getLessons())
                    .flatMap(lesson -> {
                      LessonEntity lessonEntity = new LessonEntity(null, savedModule.getId(), lesson.getTitle(), lesson.getOrderIndex(), java.time.LocalDateTime.now());
                      return lessonRepository.save(lessonEntity)
                        .flatMap(savedLesson -> {
                          if (lesson.getContents() == null || lesson.getContents().isEmpty()) {
                            return Mono.just(new Lesson(savedLesson.getId(), savedLesson.getModuleId(), savedLesson.getTitle(), savedLesson.getOrderIndex(), new ArrayList<>()));
                          }
                          return Flux.fromIterable(lesson.getContents())
                            .flatMap(content -> {
                              ContentEntity contentEntity = new ContentEntity(null, savedLesson.getId(), content.getType(), content.getValue(), content.getOrderIndex());
                              return contentRepository.save(contentEntity)
                                .map(savedContent -> new Content(savedContent.getId(), savedContent.getLessonId(), savedContent.getType(), savedContent.getValue(), savedContent.getOrderIndex()));
                            })
                            .collectList()
                            .map(contents -> new Lesson(savedLesson.getId(), savedLesson.getModuleId(), savedLesson.getTitle(), savedLesson.getOrderIndex(), contents));
                        });
                    })
                    .collectList()
                    .map(lessons -> new Module(savedModule.getId(), savedModule.getCourseId(), savedModule.getTitle(), savedModule.getOrderIndex(), lessons));
                });
            })
            .collectList()
            .map(modules -> {
              modules.forEach(m -> {
                if (m.getLessons() != null) {
                  m.getLessons().forEach(l -> {
                    if (l.getContents() != null) {
                      l.getContents().sort(Comparator.comparingInt(Content::getOrderIndex));
                    }
                  });
                  m.getLessons().sort(Comparator.comparingInt(Lesson::getOrderIndex));
                }
              });
              modules.sort(Comparator.comparingInt(Module::getOrderIndex));
              return mapToDomain(savedCourse, modules);
            })
        );
      });
  }

  @Override
  public Mono<Course> findById(Long id) {
    return courseRepository.findById(id)
      .flatMap(this::loadFullCourse);
  }

  @Override
  public Flux<Course> findByInstitutionId(String institutionId) {
    return courseRepository.findByInstitutionId(institutionId)
      .flatMap(this::loadFullCourse);
  }

  @Override
  public Flux<Course> findAll() {
    return courseRepository.findAll()
      .flatMap(this::loadFullCourse);
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return courseRepository.deleteById(id);
  }

  private Mono<Course> loadFullCourse(CourseEntity courseEntity) {
    return moduleRepository.findByCourseId(courseEntity.getId())
      .flatMap(moduleEntity -> 
        lessonRepository.findByModuleId(moduleEntity.getId())
          .flatMap(lessonEntity -> 
            contentRepository.findByLessonId(lessonEntity.getId())
              .map(contentEntity -> new Content(
                contentEntity.getId(),
                contentEntity.getLessonId(),
                contentEntity.getType(),
                contentEntity.getValue(),
                contentEntity.getOrderIndex()
              ))
              .collectList()
              .map(contents -> {
                contents.sort(Comparator.comparingInt(Content::getOrderIndex));
                return new Lesson(
                  lessonEntity.getId(),
                  lessonEntity.getModuleId(),
                  lessonEntity.getTitle(),
                  lessonEntity.getOrderIndex(),
                  contents
                );
              })
          )
          .collectList()
          .map(lessons -> {
            lessons.sort(Comparator.comparingInt(Lesson::getOrderIndex));
            return new Module(
              moduleEntity.getId(),
              moduleEntity.getCourseId(),
              moduleEntity.getTitle(),
              moduleEntity.getOrderIndex(),
              lessons
            );
          })
      )
      .collectList()
      .map(modules -> {
        modules.sort(Comparator.comparingInt(Module::getOrderIndex));
        return mapToDomain(courseEntity, modules);
      });
  }

  @Override
  public Flux<Course> findPage(String search, String status, String difficulty, int offset, int limit) {
    org.springframework.data.relational.core.query.Criteria criteria =
      org.springframework.data.relational.core.query.Criteria.empty();
    if (search != null && !search.isBlank()) {
      criteria = criteria.and(
        org.springframework.data.relational.core.query.Criteria.where("title").like("%" + search.trim() + "%").ignoreCase(true));
    }
    if (status != null && !status.isBlank()) {
      criteria = criteria.and(org.springframework.data.relational.core.query.Criteria.where("status").is(status));
    }
    if (difficulty != null && !difficulty.isBlank()) {
      criteria = criteria.and(org.springframework.data.relational.core.query.Criteria.where("difficulty").is(difficulty));
    }
    org.springframework.data.relational.core.query.Query query =
      org.springframework.data.relational.core.query.Query.query(criteria)
        .sort(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"))
        .offset(offset)
        .limit(limit);

    return template.select(query, CourseEntity.class).flatMap(this::loadFullCourse);
  }

  @Override
  public Mono<Long> count(String search, String status, String difficulty) {
    org.springframework.data.relational.core.query.Criteria criteria =
      org.springframework.data.relational.core.query.Criteria.empty();
    if (search != null && !search.isBlank()) {
      criteria = criteria.and(
        org.springframework.data.relational.core.query.Criteria.where("title").like("%" + search.trim() + "%").ignoreCase(true));
    }
    if (status != null && !status.isBlank()) {
      criteria = criteria.and(org.springframework.data.relational.core.query.Criteria.where("status").is(status));
    }
    if (difficulty != null && !difficulty.isBlank()) {
      criteria = criteria.and(org.springframework.data.relational.core.query.Criteria.where("difficulty").is(difficulty));
    }
    return template.count(org.springframework.data.relational.core.query.Query.query(criteria), CourseEntity.class);
  }

  @Override
  public Mono<Void> incrementEnrolledCount(Long courseId) {
    return courseRepository.findById(courseId)
      .flatMap(entity -> {
        entity.setEnrolledCount((entity.getEnrolledCount() == null ? 0 : entity.getEnrolledCount()) + 1);
        return courseRepository.save(entity);
      })
      .then();
  }

  private List<String> splitTags(String tags) {
    if (tags == null || tags.isBlank()) return new ArrayList<>();
    return new ArrayList<>(java.util.Arrays.asList(tags.split(",")));
  }

  private String joinTags(List<String> tags) {
    return tags == null ? null : String.join(",", tags);
  }

  private Course mapToDomain(CourseEntity entity, List<Module> modules) {
    return new Course(
      entity.getId(),
      entity.getTitle(),
      entity.getDescription(),
      entity.getStatus(),
      entity.getDifficulty(),
      splitTags(entity.getTags()),
      entity.getThumbnailUrl(),
      entity.getInstructorName(),
      entity.getInstitutionId(),
      entity.getTotalDuration(),
      entity.getTotalLessons(),
      entity.getEnrolledCount(),
      entity.getCompletionRate(),
      entity.getAverageRating(),
      entity.getRatingCount(),
      entity.getPublishedAt(),
      entity.getCreatedAt(),
      entity.getUpdatedAt(),
      modules
    );
  }

  private CourseEntity mapToEntity(Course domain) {
    return new CourseEntity(
      domain.getId(),
      domain.getTitle(),
      domain.getDescription(),
      domain.getStatus(),
      domain.getDifficulty(),
      joinTags(domain.getTags()),
      domain.getThumbnailUrl(),
      domain.getInstructorName(),
      domain.getInstitutionId(),
      domain.getTotalDuration(),
      domain.getTotalLessons(),
      domain.getEnrolledCount(),
      domain.getCompletionRate(),
      domain.getAverageRating(),
      domain.getRatingCount(),
      domain.getPublishedAt(),
      domain.getCreatedAt(),
      domain.getUpdatedAt()
    );
  }
}
