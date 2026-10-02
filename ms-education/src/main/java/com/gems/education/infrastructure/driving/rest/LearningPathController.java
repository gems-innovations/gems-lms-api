package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.infrastructure.driving.rest.mapper.LearningPathMapper;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;
import com.gems.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/learning-paths")
public class LearningPathController {
  private static final String PUBLISHED = "published";

  private final CreateLearningPathUseCase createLearningPathUseCase;
  private final GetLearningPathByIdUseCase getLearningPathByIdUseCase;
  private final GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase;
  private final UpdateLearningPathUseCase updateLearningPathUseCase;
  private final DeleteLearningPathUseCase deleteLearningPathUseCase;
  private final GetAllLearningPathsUseCase getAllLearningPathsUseCase;
  private final EducationAccess access;
  private final StudentView studentView;

  public LearningPathController(CreateLearningPathUseCase createLearningPathUseCase,
                                GetLearningPathByIdUseCase getLearningPathByIdUseCase,
                                GetLearningPathsByInstitutionUseCase getLearningPathsByInstitutionUseCase,
                                UpdateLearningPathUseCase updateLearningPathUseCase,
                                DeleteLearningPathUseCase deleteLearningPathUseCase,
                                GetAllLearningPathsUseCase getAllLearningPathsUseCase,
                                EducationAccess access,
                                StudentView studentView) {
    this.createLearningPathUseCase = createLearningPathUseCase;
    this.getLearningPathByIdUseCase = getLearningPathByIdUseCase;
    this.getLearningPathsByInstitutionUseCase = getLearningPathsByInstitutionUseCase;
    this.updateLearningPathUseCase = updateLearningPathUseCase;
    this.deleteLearningPathUseCase = deleteLearningPathUseCase;
    this.getAllLearningPathsUseCase = getAllLearningPathsUseCase;
    this.access = access;
    this.studentView = studentView;
  }

  @GetMapping
  public Mono<ResponseEntity<Flux<LearningPathResponse>>> getAllLearningPaths() {
    // Only the super admin lists across institutions.
    return CurrentUser.get().map(caller -> ResponseEntity.ok((caller.isSuperAdmin()
      ? getAllLearningPathsUseCase.execute()
      : getLearningPathsByInstitutionUseCase.execute(caller.institutionId()))
      .filter(path -> !caller.isStudent() || PUBLISHED.equals(path.getStatus()))
      .map(path -> studentView.path(caller, path))));
  }

  @PostMapping
  public Mono<ResponseEntity<LearningPathResponse>> createLearningPath(@Valid @RequestBody LearningPathRequest request) {
    return access.staffOf(request.getInstitutionId())
      .flatMap(caller -> createLearningPathUseCase.execute(LearningPathMapper.toCommand(request)))
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<LearningPathResponse>> getLearningPathById(@PathVariable Long id) {
    return CurrentUser.get()
      .flatMap(caller -> access.readablePath(id).map(path -> studentView.path(caller, path)))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/institution/{institutionId}")
  public Mono<ResponseEntity<Flux<LearningPathResponse>>> getLearningPathsByInstitution(@PathVariable String institutionId) {
    return CurrentUser.require(caller -> caller.belongsTo(institutionId), "You can only list your institution's learning paths")
      .map(caller -> ResponseEntity.ok(getLearningPathsByInstitutionUseCase.execute(institutionId)
        .filter(path -> !caller.isStudent() || PUBLISHED.equals(path.getStatus()))
        .map(path -> studentView.path(caller, path))));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<LearningPathResponse>> updateLearningPath(
      @PathVariable Long id,
      @Valid @RequestBody LearningPathRequest request
  ) {
    return access.editablePath(id)
      .flatMap(path -> access.staffOf(request.getInstitutionId()))
      .flatMap(caller -> updateLearningPathUseCase.execute(id, LearningPathMapper.toCommand(request)))
      .map(ResponseEntity::ok);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteLearningPath(@PathVariable Long id) {
    return access.editablePath(id)
      .flatMap(path -> deleteLearningPathUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()));
  }
}
