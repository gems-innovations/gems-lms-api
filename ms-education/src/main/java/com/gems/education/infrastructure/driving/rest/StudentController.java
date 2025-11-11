package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.RegisterStudentUseCase;
import com.gems.education.application.command.RegisterStudentCommand;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.infrastructure.driving.rest.constants.RestConstants;
import com.gems.education.infrastructure.driving.rest.exeption.StudentAlreadyExistsException;
import com.gems.education.infrastructure.driving.rest.mapper.StudentMapper;
import com.gems.education.infrastructure.driving.rest.request.RegisterStudentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(RestConstants.STUDENTS_API_BASE_PATH)
public class StudentController {
  private final RegisterStudentUseCase registerStudentUseCase;

  public StudentController(RegisterStudentUseCase registerStudentUseCase) {
    this.registerStudentUseCase = registerStudentUseCase;
  }

  @PostMapping(RestConstants.REGISTER_ENDPOINT)
  public Mono<ResponseEntity<StudentResponse>> registerStudent(@Valid @RequestBody RegisterStudentRequest request) {
    RegisterStudentCommand command = StudentMapper.toDomain(request);

    return registerStudentUseCase.execute(command)
      .map(studentResponse -> ResponseEntity.status(HttpStatus.CREATED).body(studentResponse))
      .onErrorResume(StudentAlreadyExistsException.class, ex ->
        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
      )
      .onErrorResume(Exception.class, ex ->
        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
      );
  }
}