package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.infrastructure.driving.rest.mapper.CourseMapper;
import com.gems.education.infrastructure.driving.rest.request.CourseRequest;
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

  public CourseController(CreateCourseUseCase createCourseUseCase,
                          GetCourseByIdUseCase getCourseByIdUseCase,
                          UpdateCourseUseCase updateCourseUseCase,
                          DeleteCourseUseCase deleteCourseUseCase,
                          GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase,
                          GetAllCoursesUseCase getAllCoursesUseCase) {
    this.createCourseUseCase = createCourseUseCase;
    this.getCourseByIdUseCase = getCourseByIdUseCase;
    this.updateCourseUseCase = updateCourseUseCase;
    this.deleteCourseUseCase = deleteCourseUseCase;
    this.getCoursesByInstitutionUseCase = getCoursesByInstitutionUseCase;
    this.getAllCoursesUseCase = getAllCoursesUseCase;
  }

  @GetMapping
  public Mono<ResponseEntity<Flux<CourseResponse>>> getAllCourses() {
    return Mono.just(ResponseEntity.ok(getAllCoursesUseCase.execute()));
  }

  @PostMapping
  public Mono<ResponseEntity<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
    CourseCommand command = CourseMapper.toCommand(request);
    return createCourseUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<CourseResponse>> getCourseById(@PathVariable Long id) {
    return getCourseByIdUseCase.execute(id)
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<CourseResponse>> updateCourse(
    @PathVariable Long id,
    @Valid @RequestBody CourseRequest request
  ) {
    CourseCommand command = CourseMapper.toCommand(request);
    return updateCourseUseCase.execute(id, command)
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteCourse(@PathVariable Long id) {
    return deleteCourseUseCase.execute(id)
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }

  @GetMapping("/institution/{institutionId}")
  public Mono<ResponseEntity<Flux<CourseResponse>>> getCoursesByInstitution(@PathVariable String institutionId) {
    return Mono.just(ResponseEntity.ok(getCoursesByInstitutionUseCase.execute(institutionId)));
  }
}
