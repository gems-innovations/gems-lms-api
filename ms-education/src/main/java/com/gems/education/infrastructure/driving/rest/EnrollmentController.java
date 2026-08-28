package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.BulkEnrollStudentsUseCase;
import com.gems.education.application.DeleteEnrollmentUseCase;
import com.gems.education.application.EnrollStudentUseCase;
import com.gems.education.application.GetEnrollmentByIdUseCase;
import com.gems.education.application.GetEnrollmentsByCourseUseCase;
import com.gems.education.application.GetStudentEnrollmentsUseCase;
import com.gems.education.application.UpdateEnrollmentProgressUseCase;
import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.command.EnrollmentCommand;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.mapper.EnrollmentMapper;
import com.gems.education.infrastructure.driving.rest.request.BulkEnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {
  private final EnrollStudentUseCase enrollStudentUseCase;
  private final BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase;
  private final GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase;
  private final GetEnrollmentsByCourseUseCase getEnrollmentsByCourseUseCase;
  private final UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase;
  private final DeleteEnrollmentUseCase deleteEnrollmentUseCase;
  private final GetEnrollmentByIdUseCase getEnrollmentByIdUseCase;

  public EnrollmentController(EnrollStudentUseCase enrollStudentUseCase,
                               BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase,
                               GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase,
                               GetEnrollmentsByCourseUseCase getEnrollmentsByCourseUseCase,
                               UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase,
                               DeleteEnrollmentUseCase deleteEnrollmentUseCase,
                               GetEnrollmentByIdUseCase getEnrollmentByIdUseCase) {
    this.enrollStudentUseCase = enrollStudentUseCase;
    this.bulkEnrollStudentsUseCase = bulkEnrollStudentsUseCase;
    this.getStudentEnrollmentsUseCase = getStudentEnrollmentsUseCase;
    this.getEnrollmentsByCourseUseCase = getEnrollmentsByCourseUseCase;
    this.updateEnrollmentProgressUseCase = updateEnrollmentProgressUseCase;
    this.deleteEnrollmentUseCase = deleteEnrollmentUseCase;
    this.getEnrollmentByIdUseCase = getEnrollmentByIdUseCase;
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<EnrollmentResponse>> getEnrollmentById(@PathVariable Long id) {
    return getEnrollmentByIdUseCase.execute(id)
      .map(ResponseEntity::ok)
      .onErrorResume(ex -> ex.getMessage() != null && ex.getMessage().contains("not found"),
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
  }

  @PostMapping
  public Mono<ResponseEntity<EnrollmentResponse>> enrollStudent(@Valid @RequestBody EnrollmentRequest request) {
    EnrollmentCommand command = EnrollmentMapper.toCommand(request);
    return enrollStudentUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/bulk")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> bulkEnrollStudents(@Valid @RequestBody BulkEnrollmentRequest request) {
    BulkEnrollmentCommand command = EnrollmentMapper.toCommand(request);
    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(bulkEnrollStudentsUseCase.execute(command)));
  }

  @GetMapping("/student/{studentId}")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> getStudentEnrollments(@PathVariable Long studentId) {
    return Mono.just(ResponseEntity.ok(getStudentEnrollmentsUseCase.execute(studentId)));
  }

  @GetMapping("/course/{courseId}")
  public Mono<ResponseEntity<Flux<EnrollmentResponse>>> getEnrollmentsByCourse(@PathVariable Long courseId) {
    return Mono.just(ResponseEntity.ok(getEnrollmentsByCourseUseCase.execute(courseId)));
  }

  @PutMapping("/{id}/progress")
  public Mono<ResponseEntity<EnrollmentResponse>> updateEnrollmentProgress(
      @PathVariable Long id,
      @RequestParam Integer progress) {
    return updateEnrollmentProgressUseCase.execute(id, progress)
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteEnrollment(@PathVariable Long id) {
    return deleteEnrollmentUseCase.execute(id)
      .then(Mono.just(ResponseEntity.noContent().<Void>build()))
      .onErrorResume(ex -> ex.getMessage() != null && ex.getMessage().contains("not found"),
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
  }
}
