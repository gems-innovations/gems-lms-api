package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class CourseRepositoryAdapter implements CourseGateway {
  private final ICourseRepository courseRepository;
  private final IModuleRepository moduleRepository;
  private final ILessonRepository lessonRepository;
  private final IContentRepository contentRepository;
  private final R2dbcEntityTemplate template;
  private final TransactionalOperator tx;

  public CourseRepositoryAdapter(ICourseRepository courseRepository,
                                 IModuleRepository moduleRepository,
                                 ILessonRepository lessonRepository,
                                 IContentRepository contentRepository,
                                 R2dbcEntityTemplate template,
                                 TransactionalOperator tx) {
    this.courseRepository = courseRepository;
    this.moduleRepository = moduleRepository;
    this.lessonRepository = lessonRepository;
    this.contentRepository = contentRepository;
    this.template = template;
    this.tx = tx;
  }

  /**
   * Persists the course and synchronizes its module/lesson/content tree in place:
   * items that carry an id are updated, items without id are inserted and items that
   * are no longer present are deleted. Ids therefore stay stable across updates, which
   * matters because quizzes (and student progress) reference lesson ids, and lessons
   * cascade-delete their quizzes. It all happens in one transaction: a failure half way leaves
   * the stored tree as it was instead of half rewritten.
   */
  @Override
  public Mono<Course> save(Course course) {
    CourseEntity courseEntity = mapToEntity(course);
    return tx.transactional(courseRepository.save(courseEntity)
      .flatMap(savedCourse -> {
        if (course.getModules() == null) {
          return Mono.just(mapToDomain(savedCourse, new ArrayList<>()));
        }
        return syncModules(savedCourse.getId(), course.getModules())
          .map(modules -> mapToDomain(savedCourse, modules));
      }));
  }

  @Override
  public Mono<Course> findHeaderById(Long id) {
    return courseRepository.findById(id).map(entity -> mapToDomain(entity, new ArrayList<>()));
  }

  @Override
  public Flux<Course> findHeadersByInstitutionId(String institutionId) {
    return courseRepository.findByInstitutionId(institutionId).map(entity -> mapToDomain(entity, new ArrayList<>()));
  }

  @Override
  public Mono<Long> findCourseIdByLessonId(Long lessonId) {
    return template.getDatabaseClient().sql("""
      SELECT m.course_id FROM lessons l JOIN modules m ON m.id = l.module_id WHERE l.id = :lessonId
      """).bind("lessonId", lessonId)
      .map((row, metadata) -> row.get("course_id", Long.class)).one();
  }

  private Mono<List<Module>> syncModules(Long courseId, List<Module> modules) {
    return moduleRepository.findByCourseId(courseId).collectList()
      .flatMap(existing -> {
        Map<Long, ModuleEntity> byId = existing.stream()
          .collect(Collectors.toMap(ModuleEntity::getId, Function.identity()));
        Set<Long> kept = modules.stream().map(Module::getId).filter(byId::containsKey).collect(Collectors.toSet());

        Mono<Void> removeMissing = Flux.fromIterable(existing)
          .filter(e -> !kept.contains(e.getId()))
          .concatMap(e -> moduleRepository.deleteById(e.getId()))
          .then();

        Flux<Module> upserts = Flux.fromIterable(modules)
          .sort(Comparator.comparingInt(Module::getOrderIndex))
          .concatMap(module -> {
            ModuleEntity current = module.getId() != null ? byId.get(module.getId()) : null;
            ModuleEntity entity = current != null
              ? new ModuleEntity(current.getId(), courseId, module.getTitle(), module.getOrderIndex(), current.getCreatedAt())
              : new ModuleEntity(null, courseId, module.getTitle(), module.getOrderIndex(), LocalDateTime.now());
            entity.setDescription(module.getDescription());
            return moduleRepository.save(entity)
              .flatMap(saved -> syncLessons(saved.getId(), module.getLessons())
                .map(lessons -> new Module(saved.getId(), saved.getCourseId(), saved.getTitle(), saved.getOrderIndex(), lessons)
                  .details(saved.getDescription())));
          });

        return removeMissing.thenMany(upserts).collectList();
      });
  }

  private Mono<List<Lesson>> syncLessons(Long moduleId, List<Lesson> lessons) {
    List<Lesson> incoming = lessons != null ? lessons : List.of();
    return lessonRepository.findByModuleId(moduleId).collectList()
      .flatMap(existing -> {
        Map<Long, LessonEntity> byId = existing.stream()
          .collect(Collectors.toMap(LessonEntity::getId, Function.identity()));
        Set<Long> kept = incoming.stream().map(Lesson::getId).filter(byId::containsKey).collect(Collectors.toSet());

        Mono<Void> removeMissing = Flux.fromIterable(existing)
          .filter(e -> !kept.contains(e.getId()))
          .concatMap(e -> lessonRepository.deleteById(e.getId()))
          .then();

        Flux<Lesson> upserts = Flux.fromIterable(incoming)
          .sort(Comparator.comparingInt(Lesson::getOrderIndex))
          .concatMap(lesson -> {
            LessonEntity current = lesson.getId() != null ? byId.get(lesson.getId()) : null;
            LessonEntity entity = current != null
              ? new LessonEntity(current.getId(), moduleId, lesson.getTitle(), lesson.getOrderIndex(), current.getCreatedAt())
              : new LessonEntity(null, moduleId, lesson.getTitle(), lesson.getOrderIndex(), LocalDateTime.now());
            entity.details(lesson.getDescription(), lesson.getIsFree());
            return lessonRepository.save(entity)
              .flatMap(saved -> syncContents(saved.getId(), lesson.getContents())
                .map(contents -> new Lesson(saved.getId(), saved.getModuleId(), saved.getTitle(), saved.getOrderIndex(), contents)
                  .details(saved.getDescription(), saved.getIsFree())));
          });

        return removeMissing.thenMany(upserts).collectList();
      });
  }

  private Mono<List<Content>> syncContents(Long lessonId, List<Content> contents) {
    List<Content> incoming = contents != null ? contents : List.of();
    return contentRepository.findByLessonId(lessonId).collectList()
      .flatMap(existing -> {
        Set<Long> existingIds = existing.stream().map(ContentEntity::getId).collect(Collectors.toSet());
        Set<Long> kept = incoming.stream().map(Content::getId).filter(existingIds::contains).collect(Collectors.toSet());

        Mono<Void> removeMissing = Flux.fromIterable(existing)
          .filter(e -> !kept.contains(e.getId()))
          .concatMap(e -> contentRepository.deleteById(e.getId()))
          .then();

        Flux<Content> upserts = Flux.fromIterable(incoming)
          .sort(Comparator.comparingInt(Content::getOrderIndex))
          .concatMap(content -> {
            Long id = content.getId() != null && existingIds.contains(content.getId()) ? content.getId() : null;
            return contentRepository.save(new ContentEntity(id, lessonId, content.getType(), content.getValue(), content.getOrderIndex()))
              .map(saved -> new Content(saved.getId(), saved.getLessonId(), saved.getType(), saved.getValue(), saved.getOrderIndex()));
          });

        return removeMissing.thenMany(upserts).collectList();
      });
  }

  @Override
  public Mono<Course> findById(Long id) {
    return loadFullCourses(courseRepository.findById(id).flux()).next();
  }

  @Override
  public Flux<Course> findByInstitutionId(String institutionId) {
    return loadFullCourses(courseRepository.findByInstitutionId(institutionId));
  }

  @Override
  public Flux<Course> findAll() {
    return loadFullCourses(courseRepository.findAll());
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return courseRepository.deleteById(id);
  }

  /**
   * The course trees in four queries (courses, their modules, lessons and contents) however many courses
   * there are, instead of three more queries per course. The order of {@code courses} is kept.
   */
  private Flux<Course> loadFullCourses(Flux<CourseEntity> courses) {
    return courses.collectList().flatMapMany(courseEntities -> {
      if (courseEntities.isEmpty()) return Flux.empty();
      return moduleRepository.findByCourseIdIn(courseEntities.stream().map(CourseEntity::getId).toList()).collectList()
        .flatMap(moduleEntities -> lessonsOf(moduleEntities).flatMap(lessonEntities -> contentsOf(lessonEntities)
          .map(contentEntities -> trees(courseEntities, moduleEntities, lessonEntities, contentEntities))))
        .flatMapMany(Flux::fromIterable);
    });
  }

  private Mono<List<LessonEntity>> lessonsOf(List<ModuleEntity> modules) {
    return modules.isEmpty() ? Mono.just(List.of())
      : lessonRepository.findByModuleIdIn(modules.stream().map(ModuleEntity::getId).toList()).collectList();
  }

  private Mono<List<ContentEntity>> contentsOf(List<LessonEntity> lessons) {
    return lessons.isEmpty() ? Mono.just(List.of())
      : contentRepository.findByLessonIdIn(lessons.stream().map(LessonEntity::getId).toList()).collectList();
  }

  private List<Course> trees(List<CourseEntity> courses, List<ModuleEntity> modules, List<LessonEntity> lessons,
                             List<ContentEntity> contents) {
    Map<Long, List<ModuleEntity>> modulesByCourse = modules.stream().collect(Collectors.groupingBy(ModuleEntity::getCourseId));
    Map<Long, List<LessonEntity>> lessonsByModule = lessons.stream().collect(Collectors.groupingBy(LessonEntity::getModuleId));
    Map<Long, List<ContentEntity>> contentsByLesson = contents.stream().collect(Collectors.groupingBy(ContentEntity::getLessonId));
    return courses.stream().map(course -> {
      List<ModuleEntity> courseModules = modulesByCourse.getOrDefault(course.getId(), List.of());
      List<LessonEntity> courseLessons = courseModules.stream()
        .flatMap(m -> lessonsByModule.getOrDefault(m.getId(), List.of()).stream()).toList();
      List<ContentEntity> courseContents = courseLessons.stream()
        .flatMap(l -> contentsByLesson.getOrDefault(l.getId(), List.of()).stream()).toList();
      return mapToDomain(course, assemble(courseModules, courseLessons, courseContents));
    }).toList();
  }

  private static List<Module> assemble(List<ModuleEntity> moduleEntities,
                                                 List<LessonEntity> lessonEntities,
                                                 List<ContentEntity> contentEntities) {
    Map<Long, List<Content>> contentsByLesson = new HashMap<>();
    for (ContentEntity c : contentEntities) {
      contentsByLesson.computeIfAbsent(c.getLessonId(), k -> new ArrayList<>())
        .add(new Content(c.getId(), c.getLessonId(), c.getType(), c.getValue(), c.getOrderIndex()));
    }
    Map<Long, List<Lesson>> lessonsByModule = new HashMap<>();
    for (LessonEntity l : lessonEntities) {
      List<Content> contents = contentsByLesson.getOrDefault(l.getId(), new ArrayList<>());
      contents.sort(Comparator.comparingInt(Content::getOrderIndex));
      lessonsByModule.computeIfAbsent(l.getModuleId(), k -> new ArrayList<>())
        .add(new Lesson(l.getId(), l.getModuleId(), l.getTitle(), l.getOrderIndex(), contents)
          .details(l.getDescription(), l.getIsFree()));
    }
    List<Module> modules = new ArrayList<>();
    for (ModuleEntity m : moduleEntities) {
      List<Lesson> lessons = lessonsByModule.getOrDefault(m.getId(), new ArrayList<>());
      lessons.sort(Comparator.comparingInt(Lesson::getOrderIndex));
      modules.add(new Module(m.getId(), m.getCourseId(), m.getTitle(), m.getOrderIndex(), lessons).details(m.getDescription()));
    }
    modules.sort(Comparator.comparingInt(Module::getOrderIndex));
    return modules;
  }

  @Override
  public Flux<Course> findPage(String search, String status, String difficulty, String institutionId, int offset, int limit) {
    org.springframework.data.relational.core.query.Query query =
      org.springframework.data.relational.core.query.Query.query(pageCriteria(search, status, difficulty, institutionId))
        .sort(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at"))
        .offset(offset)
        .limit(limit);

    // The order of the query ("newest first") is kept.
    return loadFullCourses(template.select(query, CourseEntity.class));
  }

  @Override
  public Mono<Long> count(String search, String status, String difficulty, String institutionId) {
    return template.count(
      org.springframework.data.relational.core.query.Query.query(pageCriteria(search, status, difficulty, institutionId)),
      CourseEntity.class);
  }

  private org.springframework.data.relational.core.query.Criteria pageCriteria(
      String search, String status, String difficulty, String institutionId) {
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
    if (institutionId != null && !institutionId.isBlank()) {
      criteria = criteria.and(org.springframework.data.relational.core.query.Criteria.where("institution_id").is(institutionId));
    }
    return criteria;
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
