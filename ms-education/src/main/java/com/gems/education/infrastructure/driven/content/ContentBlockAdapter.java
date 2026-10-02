package com.gems.education.infrastructure.driven.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads content blocks from the course tree. A block's `value` is the JSON the web client
 * stores: for quizzes it holds `questions` (multiple-choice with `correctAnswers`, true-false
 * with `correctAnswer`, or ungraded `open`), `passingScore` (default 70) and `maxAttempts`
 * (0 or missing = unlimited).
 */
@Component
public class ContentBlockAdapter implements ContentBlockGateway {
  private static final int DEFAULT_PASSING_SCORE = 70;

  private final CourseGateway courseGateway;
  private final ObjectMapper mapper;

  public ContentBlockAdapter(CourseGateway courseGateway, ObjectMapper mapper) {
    this.courseGateway = courseGateway;
    this.mapper = mapper;
  }

  @Override
  public Mono<BlockInfo> find(Long courseId, Long blockId) {
    return block(courseId, blockId)
      .map(content -> new BlockInfo(content.getLessonId(), content.getType(),
        parse(content.getValue()).path("maxAttempts").asInt(0)));
  }

  @Override
  public Mono<Grading> grade(Long courseId, Long blockId, String answers) {
    return block(courseId, blockId).map(content -> grade(parse(content.getValue()), parse(answers)));
  }

  private Mono<Content> block(Long courseId, Long blockId) {
    return courseGateway.findById(courseId)
      .flatMap(course -> Mono.justOrEmpty(findContent(course, blockId)));
  }

  private static Optional<Content> findContent(Course course, Long blockId) {
    if (course.getModules() == null) return Optional.empty();
    return course.getModules().stream()
      .filter(m -> m.getLessons() != null)
      .flatMap(m -> m.getLessons().stream())
      .filter(l -> l.getContents() != null)
      .flatMap(l -> l.getContents().stream())
      .filter(c -> blockId.equals(c.getId()))
      .findFirst();
  }

  Grading grade(JsonNode quiz, JsonNode answers) {
    Map<String, JsonNode> given = new HashMap<>();
    if (answers.isArray()) {
      answers.forEach(a -> given.put(a.path("questionId").asText(), a.path("answer")));
    }

    int earned = 0;
    int possible = 0;
    ArrayNode feedback = mapper.createArrayNode();
    for (JsonNode q : quiz.path("questions")) {
      String type = q.path("type").asText();
      if ("open".equals(type)) continue;
      int points = q.path("points").asInt(0) > 0 ? q.path("points").asInt() : 1;
      possible += points;
      String questionId = q.path("id").asText();
      boolean correct = isCorrect(type, q, given.get(questionId));
      if (correct) earned += points;

      ObjectNode item = feedback.addObject().put("questionId", questionId).put("correct", correct);
      if (q.hasNonNull("explanation")) item.put("explanation", q.get("explanation").asText());
    }

    int score = possible > 0 ? Math.round(earned * 100f / possible) : 0;
    int passing = quiz.path("passingScore").asInt(0) > 0 ? quiz.path("passingScore").asInt() : DEFAULT_PASSING_SCORE;
    return new Grading(score, score >= passing, feedback.toString());
  }

  private static boolean isCorrect(String type, JsonNode question, JsonNode answer) {
    if (answer == null || answer.isMissingNode() || answer.isNull()) return false;
    if ("true-false".equals(type)) {
      return answer.isBoolean() && answer.asBoolean() == question.path("correctAnswer").asBoolean();
    }
    if ("multiple-choice".equals(type)) {
      Set<String> expected = new HashSet<>();
      question.path("correctAnswers").forEach(c -> expected.add(c.asText()));
      Set<String> chosen = new HashSet<>();
      if (answer.isArray()) answer.forEach(a -> chosen.add(a.asText()));
      else if (!answer.asText().isEmpty()) chosen.add(answer.asText());
      return !expected.isEmpty() && expected.equals(chosen);
    }
    return false;
  }

  private JsonNode parse(String json) {
    if (json == null || json.isBlank()) return mapper.createObjectNode();
    try {
      return mapper.readTree(json);
    } catch (Exception e) {
      return mapper.createObjectNode();
    }
  }
}
