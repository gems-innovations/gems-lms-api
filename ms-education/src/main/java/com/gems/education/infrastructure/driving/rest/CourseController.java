package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.response.CourseListResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.infrastructure.driving.rest.mapper.CourseMapper;
import com.gems.education.infrastructure.driving.rest.request.CourseRequest;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.ForbiddenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
  private final CreateCourseUseCase createCourseUseCase;
  private final GetCourseByIdUseCase getCourseByIdUseCase;
  private final UpdateCourseUseCase updateCourseUseCase;
  private final DeleteCourseUseCase deleteCourseUseCase;
  private final GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase;
  private final GetAllCoursesUseCase getAllCoursesUseCase;
  private final EducationAccess access;
  private final StudentView studentView;

  public CourseController(CreateCourseUseCase createCourseUseCase,
                          GetCourseByIdUseCase getCourseByIdUseCase,
                          UpdateCourseUseCase updateCourseUseCase,
                          DeleteCourseUseCase deleteCourseUseCase,
                          GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase,
                          GetAllCoursesUseCase getAllCoursesUseCase,
                          EducationAccess access,
                          StudentView studentView) {
    this.createCourseUseCase = createCourseUseCase;
    this.getCourseByIdUseCase = getCourseByIdUseCase;
    this.updateCourseUseCase = updateCourseUseCase;
    this.deleteCourseUseCase = deleteCourseUseCase;
    this.getCoursesByInstitutionUseCase = getCoursesByInstitutionUseCase;
    this.getAllCoursesUseCase = getAllCoursesUseCase;
    this.access = access;
    this.studentView = studentView;
  }

  @GetMapping
  public Mono<ResponseEntity<CourseListResponse>> getAllCourses(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String difficulty,
      @RequestParam(required = false) String institutionId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int limit) {
    // Only the super admin lists across institutions; students only see published courses.
    return CurrentUser.get().flatMap(caller -> {
      String scope = caller.isSuperAdmin() ? institutionId : caller.institutionId();
      if (scope == null && !caller.isSuperAdmin()) {
        return Mono.error(new ForbiddenException("Your account has no institution"));
      }
      String visibleStatus = caller.isStudent() ? "published" : status;
      return getAllCoursesUseCase.execute(search, visibleStatus, difficulty, scope, page, limit)
        .map(list -> studentView.courses(caller, list));
    }).map(ResponseEntity::ok);
  }

  @PostMapping
  public Mono<ResponseEntity<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
    return access.staff()
      .flatMap(caller -> {
        if (request.getInstitutionId() == null) request.setInstitutionId(caller.institutionId());
        if (!caller.belongsTo(request.getInstitutionId())) {
          return Mono.error(new ForbiddenException("You can only create courses in your institution"));
        }
        return createCourseUseCase.execute(CourseMapper.toCommand(request));
      })
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<CourseResponse>> getCourseById(@PathVariable Long id) {
    return CurrentUser.get()
      .flatMap(caller -> access.readableCourse(id).map(course -> studentView.course(caller, course)))
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<CourseResponse>> updateCourse(
    @PathVariable Long id,
    @Valid @RequestBody CourseRequest request
  ) {
    return access.editableCourse(id)
      .flatMap(course -> access.staffOf(request.getInstitutionId() != null ? request.getInstitutionId() : course.getInstitutionId()))
      .flatMap(caller -> updateCourseUseCase.execute(id, CourseMapper.toCommand(request)))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteCourse(@PathVariable Long id) {
    return access.editableCourse(id)
      .flatMap(course -> deleteCourseUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }

  @GetMapping("/institution/{institutionId}")
  public Mono<ResponseEntity<Flux<CourseResponse>>> getCoursesByInstitution(@PathVariable String institutionId) {
    return CurrentUser.require(caller -> caller.belongsTo(institutionId), "You can only list your institution's courses")
      .map(caller -> {
        Flux<CourseResponse> courses = getCoursesByInstitutionUseCase.execute(institutionId);
        return ResponseEntity.ok(caller.isStudent()
          ? courses.filter(c -> "published".equals(c.getStatus())).map(c -> studentView.course(caller, c))
          : courses);
      });
  }
}
