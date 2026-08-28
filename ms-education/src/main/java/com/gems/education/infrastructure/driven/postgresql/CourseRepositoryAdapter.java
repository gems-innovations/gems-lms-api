package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
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

  public CourseRepositoryAdapter(ICourseRepository courseRepository,
                                 IModuleRepository moduleRepository,
                                 ILessonRepository lessonRepository,
                                 IContentRepository contentRepository) {
    this.courseRepository = courseRepository;
    this.moduleRepository = moduleRepository;
    this.lessonRepository = lessonRepository;
    this.contentRepository = contentRepository;
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

  private Course mapToDomain(CourseEntity entity, List<Module> modules) {
    return new Course(
      entity.getId(),
      entity.getTitle(),
      entity.getDescription(),
      entity.getStatus(),
      entity.getInstitutionId(),
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
      domain.getInstitutionId(),
      domain.getCreatedAt(),
      domain.getUpdatedAt()
    );
  }
}
