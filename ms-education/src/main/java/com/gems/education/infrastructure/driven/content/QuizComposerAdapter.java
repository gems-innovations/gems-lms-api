package com.gems.education.infrastructure.driven.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.gateway.ContentBlockGateway.Grading;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.QuestionBankGateway;
import com.gems.education.application.gateway.QuizComposer;
import com.gems.education.domain.entities.BankQuestion;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Composes a quiz attempt from the block JSON: its own questions (open questions are not part of
 * attempts) plus `questionPools` ([{category, count}] drawn at random from the institution's bank),
 * shuffled when `shuffleQuestions` is set (options too, unless `shuffleOptions` is false).
 * `timeLimit` is in minutes.
 */
@Component
public class QuizComposerAdapter implements QuizComposer {
  static final Set<String> ANSWER_KEYS = Set.of("correctAnswers", "correctAnswer", "sampleAnswer", "explanation");
  private static final int DEFAULT_PASSING_SCORE = 70;
  private static final int MAX_POOL_COUNT = 100;

  private final ContentBlockAdapter blocks;
  private final CourseGateway courses;
  private final QuestionBankGateway bank;
  private final ObjectMapper mapper;
  private final Random random = new SecureRandom();

  public QuizComposerAdapter(ContentBlockAdapter blocks, CourseGateway courseGateway, QuestionBankGateway bank,
                             ObjectMapper mapper) {
    this.blocks = blocks;
    this.courses = courseGateway;
    this.bank = bank;
    this.mapper = mapper;
  }

  @Override
  public Mono<ComposedQuiz> compose(Long courseId, Long blockId) {
    return courses.findById(courseId).flatMap(course -> blocks.block(courseId, blockId).flatMap(content -> {
      JsonNode quiz = blocks.parse(content.getValue());
      return Flux.fromIterable(pools(quiz).entrySet())
        .concatMap(pool -> bank.draw(course.getInstitutionId(), pool.getKey(), pool.getValue()))
        .collectList()
        .map(drawn -> build(quiz, drawn));
    }));
  }

  @Override
  public Grading grade(String questions, int passingScore, String answers) {
    ObjectNode quiz = mapper.createObjectNode();
    quiz.set("questions", blocks.parse(questions));
    quiz.put("passingScore", passingScore);
    return blocks.grade(quiz, blocks.parse(answers));
  }

  ComposedQuiz build(JsonNode quiz, List<BankQuestion> drawn) {
    List<JsonNode> questions = new ArrayList<>();
    for (JsonNode q : quiz.path("questions")) {
      if (!"open".equals(q.path("type").asText())) questions.add(q.deepCopy());
    }
    for (BankQuestion b : drawn) {
      JsonNode payload = blocks.parse(b.payload());
      if (!(payload instanceof ObjectNode node) || "open".equals(b.type())) continue;
      node.put("id", "bank-" + b.id());
      node.put("type", b.type());
      questions.add(node);
    }

    boolean shuffle = quiz.path("shuffleQuestions").asBoolean(false);
    if (shuffle) Collections.shuffle(questions, random);
    if (shuffle && quiz.path("shuffleOptions").asBoolean(true)) {
      for (JsonNode q : questions) {
        if (q.path("options") instanceof ArrayNode options) shuffle(options);
      }
    }

    ArrayNode full = mapper.createArrayNode().addAll(questions);
    ArrayNode visible = full.deepCopy();
    for (JsonNode q : visible) ((ObjectNode) q).remove(ANSWER_KEYS);

    int passing = quiz.path("passingScore").asInt(0) > 0 ? quiz.path("passingScore").asInt() : DEFAULT_PASSING_SCORE;
    int minutes = Math.max(0, quiz.path("timeLimit").asInt(0));
    return new ComposedQuiz(full.toString(), visible.toString(), passing, minutes * 60);
  }

  /** Requested questions per category; repeated categories add up so draws do not repeat questions. */
  private static Map<String, Integer> pools(JsonNode quiz) {
    Map<String, Integer> pools = new LinkedHashMap<>();
    for (JsonNode pool : quiz.path("questionPools")) {
      String category = pool.path("category").asText("").trim();
      int count = pool.path("count").asInt(0);
      if (!category.isEmpty() && count > 0) pools.merge(category, Math.min(count, MAX_POOL_COUNT), Integer::sum);
    }
    return pools;
  }

  private void shuffle(ArrayNode options) {
    List<JsonNode> list = new ArrayList<>();
    options.forEach(list::add);
    Collections.shuffle(list, random);
    options.removeAll();
    options.addAll(list);
  }
}
