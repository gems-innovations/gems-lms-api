package com.gems.education.infrastructure.driven.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gems.education.application.gateway.ContentBlockGateway.Grading;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.QuestionBankGateway;
import com.gems.education.application.gateway.QuizComposer.ComposedQuiz;
import com.gems.education.domain.entities.BankQuestion;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QuizComposerAdapterTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final CourseGateway courses = mock(CourseGateway.class);
  private final ContentBlockAdapter blocks = new ContentBlockAdapter(courses, mapper);
  private final QuizComposerAdapter composer = new QuizComposerAdapter(blocks, courses, mock(QuestionBankGateway.class), mapper);

  private static final String QUIZ = "{\"passingScore\":60,\"timeLimit\":5,\"shuffleQuestions\":true,\"questions\":["
    + "{\"id\":\"q1\",\"type\":\"true-false\",\"question\":\"A\",\"correctAnswer\":true,\"explanation\":\"porque\"},"
    + "{\"id\":\"q2\",\"type\":\"open\",\"question\":\"B\",\"sampleAnswer\":\"x\"},"
    + "{\"id\":\"q3\",\"type\":\"multiple-choice\",\"question\":\"C\",\"options\":[{\"id\":\"a\",\"text\":\"1\"},"
    + "{\"id\":\"b\",\"text\":\"2\"},{\"id\":\"c\",\"text\":\"3\"}],\"correctAnswers\":[\"b\"]}]}";

  private final BankQuestion banked = new BankQuestion(42L, "inst-1", "SQL", "true-false",
    "{\"question\":\"D\",\"correctAnswer\":false}", null, null, null);

  @Test
  void addsBankQuestionsDropsOpenOnesAndHidesAnswerKeys() throws Exception {
    ComposedQuiz quiz = composer.build(mapper.readTree(QUIZ), List.of(banked));

    JsonNode full = mapper.readTree(quiz.questions());
    Set<String> ids = new HashSet<>();
    full.forEach(q -> ids.add(q.path("id").asText()));
    assertThat(ids).containsExactlyInAnyOrder("q1", "q3", "bank-42");
    assertThat(quiz.passingScore()).isEqualTo(60);
    assertThat(quiz.timeLimitSeconds()).isEqualTo(300);

    String visible = quiz.studentQuestions();
    assertThat(visible).doesNotContain("correctAnswer", "explanation", "sampleAnswer");
    assertThat(mapper.readTree(visible)).hasSize(3);
  }

  @Test
  void gradesAgainstTheComposedQuestions() throws Exception {
    ComposedQuiz quiz = composer.build(mapper.readTree(QUIZ), List.of(banked));
    Grading grading = composer.grade(quiz.questions(), quiz.passingScore(),
      "[{\"questionId\":\"q1\",\"answer\":true},{\"questionId\":\"q3\",\"answer\":[\"b\"]},"
        + "{\"questionId\":\"bank-42\",\"answer\":true}]");

    assertThat(grading.score()).isEqualTo(67);
    assertThat(grading.passed()).isTrue();
    assertThat(grading.feedback()).contains("porque");
  }

  @Test
  void onlyBankTimedOrShuffledQuizzesNeedASession() throws Exception {
    assertThat(ContentBlockAdapter.sessionRequired(mapper.readTree("{\"questions\":[]}"))).isFalse();
    assertThat(ContentBlockAdapter.sessionRequired(mapper.readTree("{\"timeLimit\":0,\"shuffleQuestions\":false}"))).isFalse();
    assertThat(ContentBlockAdapter.sessionRequired(mapper.readTree("{\"timeLimit\":10}"))).isTrue();
    assertThat(ContentBlockAdapter.sessionRequired(mapper.readTree("{\"questionPools\":[{\"category\":\"SQL\",\"count\":3}]}"))).isTrue();
  }
}
