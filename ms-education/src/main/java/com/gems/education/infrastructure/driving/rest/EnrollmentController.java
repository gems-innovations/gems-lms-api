package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
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
  private final UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase;

  public EnrollmentController(EnrollStudentUseCase enrollStudentUseCase,
                              BulkEnrollStudentsUseCase bulkEnrollStudentsUseCase,
                              GetStudentEnrollmentsUseCase getStudentEnrollmentsUseCase,
                              UpdateEnrollmentProgressUseCase updateEnrollmentProgressUseCase) {
    this.enrollStudentUseCase = enrollStudentUseCase;
    this.bulkEnrollStudentsUseCase = bulkEnrollStudentsUseCase;
    this.getStudentEnrollmentsUseCase = getStudentEnrollmentsUseCase;
    this.updateEnrollmentProgressUseCase = updateEnrollmentProgressUseCase;
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

  @PutMapping("/{id}/progress")
  public Mono<ResponseEntity<EnrollmentResponse>> updateEnrollmentProgress(
      @PathVariable Long id,
      @RequestParam Integer progress
  ) {
    return updateEnrollmentProgressUseCase.execute(id, progress)
      .map(ResponseEntity::ok);
  }
}
