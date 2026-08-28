package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.infrastructure.driving.rest.mapper.LearningPathMapper;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/learning-paths")
public class LearningPathController {
  private final CreateLearningPathUseCase createLearningPathUseCase;
  private final GetLearningPathByIdUseCase getLearningPathByIdUseCase;
  private final GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase;
  private final UpdateLearningPathUseCase updateLearningPathUseCase;
  private final DeleteLearningPathUseCase deleteLearningPathUseCase;
  private final GetAllLearningPathsUseCase getAllLearningPathsUseCase;

  public LearningPathController(CreateLearningPathUseCase createLearningPathUseCase,
                                GetLearningPathByIdUseCase getLearningPathByIdUseCase,
                                GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase,
                                UpdateLearningPathUseCase updateLearningPathUseCase,
                                DeleteLearningPathUseCase deleteLearningPathUseCase,
                                GetAllLearningPathsUseCase getAllLearningPathsUseCase) {
    this.createLearningPathUseCase = createLearningPathUseCase;
    this.getLearningPathByIdUseCase = getLearningPathByIdUseCase;
    this.getLearningPathsByInstitutionUseCase = getLearningPathsByInstitutionUseCase;
    this.updateLearningPathUseCase = updateLearningPathUseCase;
    this.deleteLearningPathUseCase = deleteLearningPathUseCase;
    this.getAllLearningPathsUseCase = getAllLearningPathsUseCase;
  }

  @GetMapping
  public Mono<ResponseEntity<Flux<LearningPathResponse>>> getAllLearningPaths() {
    return Mono.just(ResponseEntity.ok(getAllLearningPathsUseCase.execute()));
  }

  @PostMapping
  public Mono<ResponseEntity<LearningPathResponse>> createLearningPath(@Valid @RequestBody LearningPathRequest request) {
    LearningPathCommand command = LearningPathMapper.toCommand(request);
    return createLearningPathUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<LearningPathResponse>> getLearningPathById(@PathVariable Long id) {
    return getLearningPathByIdUseCase.execute(id)
      .map(ResponseEntity::ok);
  }

  @GetMapping("/institution/{institutionId}")
  public Mono<ResponseEntity<Flux<LearningPathResponse>>> getLearningPathsByInstitution(@PathVariable String institutionId) {
    return Mono.just(ResponseEntity.ok(getLearningPathsByInstitutionUseCase.execute(institutionId)));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<LearningPathResponse>> updateLearningPath(
      @PathVariable Long id,
      @Valid @RequestBody LearningPathRequest request
  ) {
    LearningPathCommand command = LearningPathMapper.toCommand(request);
    return updateLearningPathUseCase.execute(id, command)
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteLearningPath(@PathVariable Long id) {
    return deleteLearningPathUseCase.execute(id)
      .then(Mono.just(ResponseEntity.noContent().build()));
  }
}
