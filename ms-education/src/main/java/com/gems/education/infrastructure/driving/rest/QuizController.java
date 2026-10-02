package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.CreateQuizUseCase;
import com.gems.education.application.DeleteQuizUseCase;
import com.gems.education.application.GetQuizByIdUseCase;
import com.gems.education.application.GetQuizByLessonUseCase;
import com.gems.education.application.SubmitQuizUseCase;
import com.gems.education.application.UpdateQuizUseCase;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.infrastructure.driving.rest.mapper.QuizMapper;
import com.gems.education.infrastructure.driving.rest.request.QuizRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizSubmissionRequest;
import com.gems.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {
  private final CreateQuizUseCase createQuizUseCase;
  private final GetQuizByLessonUseCase getQuizByLessonUseCase;
  private final GetQuizByIdUseCase getQuizByIdUseCase;
  private final UpdateQuizUseCase updateQuizUseCase;
  private final DeleteQuizUseCase deleteQuizUseCase;
  private final SubmitQuizUseCase submitQuizUseCase;
  private final EducationAccess access;
  private final StudentView studentView;

  public QuizController(CreateQuizUseCase createQuizUseCase,
                        GetQuizByLessonUseCase getQuizByLessonUseCase,
                        GetQuizByIdUseCase getQuizByIdUseCase,
                        UpdateQuizUseCase updateQuizUseCase,
                        DeleteQuizUseCase deleteQuizUseCase,
                        SubmitQuizUseCase submitQuizUseCase,
                        EducationAccess access,
                        StudentView studentView) {
    this.createQuizUseCase = createQuizUseCase;
    this.getQuizByLessonUseCase = getQuizByLessonUseCase;
    this.getQuizByIdUseCase = getQuizByIdUseCase;
    this.updateQuizUseCase = updateQuizUseCase;
    this.deleteQuizUseCase = deleteQuizUseCase;
    this.submitQuizUseCase = submitQuizUseCase;
    this.access = access;
    this.studentView = studentView;
  }

  @PostMapping
  public Mono<ResponseEntity<QuizResponse>> createQuiz(@Valid @RequestBody QuizRequest request) {
    return access.staff()
      .flatMap(caller -> createQuizUseCase.execute(QuizMapper.toCommand(request)))
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<QuizResponse>> getQuizById(@PathVariable Long id) {
    return CurrentUser.get()
      .flatMap(caller -> getQuizByIdUseCase.execute(id).map(quiz -> studentView.quiz(caller, quiz)))
      .map(ResponseEntity::ok)
      .onErrorResume(ex -> ex.getMessage() != null && ex.getMessage().contains("not found"),
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
  }

  @GetMapping("/lesson/{lessonId}")
  public Mono<ResponseEntity<QuizResponse>> getQuizByLesson(@PathVariable Long lessonId) {
    return CurrentUser.get()
      .flatMap(caller -> getQuizByLessonUseCase.execute(lessonId).map(quiz -> studentView.quiz(caller, quiz)))
      .map(ResponseEntity::ok);
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<QuizResponse>> updateQuiz(@PathVariable Long id,
                                                       @Valid @RequestBody QuizRequest request) {
    return access.staff()
      .flatMap(caller -> updateQuizUseCase.execute(id, QuizMapper.toCommand(request)))
      .map(ResponseEntity::ok)
      .onErrorResume(ex -> ex.getMessage() != null && ex.getMessage().contains("not found"),
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteQuiz(@PathVariable Long id) {
    return access.staff()
      .flatMap(caller -> deleteQuizUseCase.execute(id))
      .then(Mono.just(ResponseEntity.noContent().<Void>build()))
      .onErrorResume(ex -> ex.getMessage() != null && ex.getMessage().contains("not found"),
        ex -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
  }

  @PostMapping("/{id}/submit")
  public Mono<ResponseEntity<QuizGradingResponse>> submitQuiz(
      @PathVariable Long id,
      @Valid @RequestBody QuizSubmissionRequest request) {
    return CurrentUser.require(caller -> caller.isUser(request.getStudentId()), "You can only submit your own answers")
      .flatMap(caller -> submitQuizUseCase.execute(id, QuizMapper.toCommand(request)))
      .map(ResponseEntity::ok);
  }
}
