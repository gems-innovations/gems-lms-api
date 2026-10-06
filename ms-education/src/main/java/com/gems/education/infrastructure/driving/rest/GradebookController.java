package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.PeriodClosingUseCase;
import com.gems.education.application.GradebookUseCase;
import com.gems.education.application.GradebookUseCase.Gradebook;
import com.gems.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Course gradebook. Staff who can edit the course see every student and set the weights; a
 * student sees only their own row.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/gradebook")
public class GradebookController {
  private final GradebookUseCase gradebook;
  private final EducationAccess access;
  private final PeriodClosingUseCase closing;

  public GradebookController(GradebookUseCase gradebook, EducationAccess access, PeriodClosingUseCase closing) {
    this.closing = closing;
    this.gradebook = gradebook;
    this.access = access;
  }

  @GetMapping
  public Mono<ResponseEntity<Gradebook>> course(@PathVariable Long courseId) {
    return access.editableCourse(courseId)
      .then(Mono.defer(() -> gradebook.course(courseId)))
      .map(ResponseEntity::ok);
  }

  @GetMapping("/me")
  public Mono<ResponseEntity<Gradebook>> mine(@PathVariable Long courseId) {
    return access.readableCourse(courseId)
      .then(CurrentUser.get())
      .flatMap(caller -> gradebook.student(courseId, caller.userId()))
      .map(ResponseEntity::ok);
  }

  /** weights: {blockId: weight 0-100}; blocks left out count 1. */
  @PutMapping("/weights")
  public Mono<ResponseEntity<Gradebook>> weights(@PathVariable Long courseId, @Valid @RequestBody WeightsRequest request) {
    return access.editableCourse(courseId)
      .then(closing.requireOpen(courseId))
      .then(Mono.defer(() -> gradebook.saveWeights(courseId, request.weights())))
      .map(ResponseEntity::ok);
  }

  public record WeightsRequest(@NotNull(message = "Weights are required") Map<Long, Integer> weights) {
  }
}
