package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.QuizGateway;
import com.gems.education.domain.entities.Question;
import com.gems.education.domain.entities.Quiz;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class QuizRepositoryAdapter implements QuizGateway {
  private final IQuizRepository quizRepository;
  private final IQuestionRepository questionRepository;

  public QuizRepositoryAdapter(IQuizRepository quizRepository, IQuestionRepository questionRepository) {
    this.quizRepository = quizRepository;
    this.questionRepository = questionRepository;
  }

  @Override
  public Mono<Quiz> save(Quiz quiz) {
    QuizEntity quizEntity = new QuizEntity(quiz.getId(), quiz.getLessonId(), quiz.getTitle(), quiz.getPassingScore());
    return quizRepository.save(quizEntity)
      .flatMap(savedQuiz -> {
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
          return Mono.just(mapToDomain(savedQuiz, new ArrayList<>()));
        }

        Mono<Void> cleanUp = Mono.empty();
        if (quiz.getId() != null) {
          cleanUp = questionRepository.deleteByQuizId(savedQuiz.getId());
        }

        return cleanUp.then(
          Flux.fromIterable(quiz.getQuestions())
            .flatMap(q -> {
              String optionsStr = q.getOptions() != null ? String.join(";", q.getOptions()) : "";
              QuestionEntity qEntity = new QuestionEntity(null, savedQuiz.getId(), q.getText(), optionsStr, q.getCorrectOption());
              return questionRepository.save(qEntity)
                .map(savedQ -> new Question(
                  savedQ.getId(),
                  savedQ.getQuizId(),
                  savedQ.getText(),
                  savedQ.getOptions() != null && !savedQ.getOptions().isEmpty() ? Arrays.asList(savedQ.getOptions().split(";")) : new ArrayList<>(),
                  savedQ.getCorrectOption()
                ));
            })
            .collectList()
            .map(questions -> mapToDomain(savedQuiz, questions))
        );
      });
  }

  @Override
  public Mono<Quiz> findById(Long id) {
    return quizRepository.findById(id)
      .flatMap(this::loadFullQuiz);
  }

  @Override
  public Mono<Quiz> findByLessonId(Long lessonId) {
    return quizRepository.findByLessonId(lessonId)
      .flatMap(this::loadFullQuiz);
  }

  @Override
  public Mono<Void> deleteById(Long id) {
    return quizRepository.deleteById(id);
  }

  private Mono<Quiz> loadFullQuiz(QuizEntity quizEntity) {
    return questionRepository.findByQuizId(quizEntity.getId())
      .map(qEntity -> new Question(
        qEntity.getId(),
        qEntity.getQuizId(),
        qEntity.getText(),
        qEntity.getOptions() != null && !qEntity.getOptions().isEmpty() ? Arrays.asList(qEntity.getOptions().split(";")) : new ArrayList<>(),
        qEntity.getCorrectOption()
      ))
      .collectList()
      .map(questions -> mapToDomain(quizEntity, questions));
  }

  private Quiz mapToDomain(QuizEntity entity, List<Question> questions) {
    return new Quiz(
      entity.getId(),
      entity.getLessonId(),
      entity.getTitle(),
      entity.getPassingScore(),
      questions
    );
  }
}
