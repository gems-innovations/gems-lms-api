package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.infrastructure.driving.rest.constants.RestConstants;
import com.gems.education.infrastructure.driving.rest.mapper.StudentMapper;
import com.gems.education.infrastructure.driving.rest.request.StudentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

@RestController
@RequestMapping(RestConstants.STUDENTS_API_BASE_PATH)
public class StudentController {

  private final RegisterStudentUseCase registerStudentUseCase;
  private final GetStudentByIdUseCase getStudentByIdUseCase;
  private final GetAllStudentsUseCase getAllStudentsUseCase;
  private final UpdateStudentUseCase updateStudentUseCase;
  private final DeleteStudentUseCase deleteStudentUseCase;

  public StudentController(
    RegisterStudentUseCase registerStudentUseCase,
    GetStudentByIdUseCase getStudentByIdUseCase,
    GetAllStudentsUseCase getAllStudentsUseCase,
    UpdateStudentUseCase updateStudentUseCase,
    DeleteStudentUseCase deleteStudentUseCase
  ) {
    this.registerStudentUseCase = registerStudentUseCase;
    this.getStudentByIdUseCase = getStudentByIdUseCase;
    this.getAllStudentsUseCase = getAllStudentsUseCase;
    this.updateStudentUseCase = updateStudentUseCase;
    this.deleteStudentUseCase = deleteStudentUseCase;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  public Mono<ResponseEntity<StudentResponse>> registerStudent(@Valid @RequestBody StudentRequest request) {
    StudentCommand command = StudentMapper.toDomain(request);
    return registerStudentUseCase.execute(command)
      .map(studentResponse -> ResponseEntity.status(HttpStatus.CREATED).body(studentResponse));
  }

  @GetMapping
  public Flux<StudentResponse> getAllStudents() {
    return getAllStudentsUseCase.execute();
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<StudentResponse>> updateStudent(
    @PathVariable Long id,
    @Valid @RequestBody StudentRequest request
  ) {
    StudentCommand command = StudentMapper.toDomain(request);
    return updateStudentUseCase.execute(id, command)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<StudentResponse>> getStudentById(@PathVariable Long id) {
    return getStudentByIdUseCase.execute(id)
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteStudent(@PathVariable Long id) {
    return deleteStudentUseCase.execute(id)
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }
}