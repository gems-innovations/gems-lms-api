package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.CreateQuizUseCase;
import com.gems.education.application.GetQuizByLessonUseCase;
import com.gems.education.application.SubmitQuizUseCase;
import com.gems.education.application.command.QuizCommand;
import com.gems.education.application.command.QuizSubmissionCommand;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.infrastructure.driving.rest.mapper.QuizMapper;
import com.gems.education.infrastructure.driving.rest.request.QuizRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizSubmissionRequest;
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
  private final SubmitQuizUseCase submitQuizUseCase;

  public QuizController(CreateQuizUseCase createQuizUseCase,
                        GetQuizByLessonUseCase getQuizByLessonUseCase,
                        SubmitQuizUseCase submitQuizUseCase) {
    this.createQuizUseCase = createQuizUseCase;
    this.getQuizByLessonUseCase = getQuizByLessonUseCase;
    this.submitQuizUseCase = submitQuizUseCase;
  }

  @PostMapping
  public Mono<ResponseEntity<QuizResponse>> createQuiz(@Valid @RequestBody QuizRequest request) {
    QuizCommand command = QuizMapper.toCommand(request);
    return createQuizUseCase.execute(command)
      .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @GetMapping("/lesson/{lessonId}")
  public Mono<ResponseEntity<QuizResponse>> getQuizByLesson(@PathVariable Long lessonId) {
    return getQuizByLessonUseCase.execute(lessonId)
      .map(ResponseEntity::ok);
  }

  @PostMapping("/{id}/submit")
  public Mono<ResponseEntity<QuizGradingResponse>> submitQuiz(
      @PathVariable Long id,
      @Valid @RequestBody QuizSubmissionRequest request
  ) {
    QuizSubmissionCommand command = QuizMapper.toCommand(request);
    return submitQuizUseCase.execute(id, command)
      .map(ResponseEntity::ok);
  }
}
