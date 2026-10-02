package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
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
  private final EducationAccess access;

  public StudentController(
    RegisterStudentUseCase registerStudentUseCase,
    GetStudentByIdUseCase getStudentByIdUseCase,
    GetAllStudentsUseCase getAllStudentsUseCase,
    UpdateStudentUseCase updateStudentUseCase,
    DeleteStudentUseCase deleteStudentUseCase,
    EducationAccess access
  ) {
    this.registerStudentUseCase = registerStudentUseCase;
    this.getStudentByIdUseCase = getStudentByIdUseCase;
    this.getAllStudentsUseCase = getAllStudentsUseCase;
    this.updateStudentUseCase = updateStudentUseCase;
    this.deleteStudentUseCase = deleteStudentUseCase;
    this.access = access;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  public Mono<ResponseEntity<StudentResponse>> registerStudent(@Valid @RequestBody StudentRequest request) {
    return access.staff()
      .flatMap(caller -> registerStudentUseCase.execute(StudentMapper.toDomain(request)))
      .map(studentResponse -> ResponseEntity.status(HttpStatus.CREATED).body(studentResponse));
  }

  @GetMapping
  public Flux<StudentResponse> getAllStudents() {
    return access.staff().flatMapMany(caller -> getAllStudentsUseCase.execute());
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<StudentResponse>> updateStudent(
    @PathVariable Long id,
    @Valid @RequestBody StudentRequest request
  ) {
    return access.staff()
      .flatMap(caller -> updateStudentUseCase.execute(id, StudentMapper.toDomain(request)))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<StudentResponse>> getStudentById(@PathVariable Long id) {
    return access.staff()
      .flatMap(caller -> getStudentByIdUseCase.execute(id))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteStudent(@PathVariable Long id) {
    return access.staff()
      .flatMap(caller -> deleteStudentUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }
}