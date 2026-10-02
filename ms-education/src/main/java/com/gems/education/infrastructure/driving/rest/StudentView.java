package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.response.*;
import com.gems.shared.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Removes answer keys from what students receive. Quizzes are graded on the server
 * (CourseActivityController), so students never need correct answers, sample answers or
 * explanations up front; explanations come back as feedback after an attempt.
 */
@Component
public class StudentView {
  private static final String QUIZ = "quiz";
  private static final Set<String> ANSWER_KEYS = Set.of("correctAnswers", "correctAnswer", "sampleAnswer", "explanation");

  private final ObjectMapper mapper;

  public StudentView(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public CourseResponse course(AuthenticatedUser caller, CourseResponse c) {
    if (!caller.isStudent() || c.getModules() == null) return c;
    return new CourseResponse(c.getId(), c.getTitle(), c.getDescription(), c.getStatus(), c.getDifficulty(),
      c.getTags(), c.getThumbnailUrl(), c.getInstructorName(), c.getInstitutionId(), c.getTotalDuration(),
      c.getTotalLessons(), c.getEnrolledCount(), c.getCompletionRate(), c.getAverageRating(), c.getRatingCount(),
      c.getPublishedAt(), c.getCreatedAt(), c.getUpdatedAt(), c.getModules().stream().map(this::module).toList());
  }

  public CourseListResponse courses(AuthenticatedUser caller, CourseListResponse list) {
    if (!caller.isStudent()) return list;
    return new CourseListResponse(list.courses().stream().map(c -> course(caller, c)).toList(), list.total(),
      list.page(), list.limit(), list.totalPages(), list.hasNext(), list.hasPrevious());
  }

  public LearningPathResponse path(AuthenticatedUser caller, LearningPathResponse p) {
    if (!caller.isStudent() || p.getCourses() == null) return p;
    return new LearningPathResponse(p.getId(), p.getTitle(), p.getDescription(), p.getInstitutionId(),
      p.getCreatedAt(), p.getCourses().stream().map(c -> course(caller, c)).toList());
  }

  public QuizResponse quiz(AuthenticatedUser caller, QuizResponse q) {
    if (!caller.isStudent() || q.getQuestions() == null) return q;
    List<QuestionResponse> questions = q.getQuestions().stream()
      .map(x -> new QuestionResponse(x.getId(), x.getQuizId(), x.getText(), x.getOptions(), null))
      .toList();
    return new QuizResponse(q.getId(), q.getLessonId(), q.getTitle(), q.getPassingScore(), questions);
  }

  private ModuleResponse module(ModuleResponse m) {
    List<LessonResponse> lessons = m.getLessons() == null ? null : m.getLessons().stream().map(this::lesson).toList();
    return new ModuleResponse(m.getId(), m.getCourseId(), m.getTitle(), m.getOrderIndex(), lessons);
  }

  private LessonResponse lesson(LessonResponse l) {
    List<ContentResponse> contents = l.getContents() == null ? null : l.getContents().stream().map(this::content).toList();
    return new LessonResponse(l.getId(), l.getModuleId(), l.getTitle(), l.getOrderIndex(), contents);
  }

  private ContentResponse content(ContentResponse c) {
    if (!QUIZ.equals(c.getType()) || c.getValue() == null) return c;
    return new ContentResponse(c.getId(), c.getLessonId(), c.getType(), withoutAnswers(c.getValue()), c.getOrderIndex());
  }

  private String withoutAnswers(String json) {
    try {
      JsonNode root = mapper.readTree(json);
      for (JsonNode question : root.path("questions")) {
        if (question instanceof ObjectNode node) node.remove(ANSWER_KEYS);
      }
      return mapper.writeValueAsString(root);
    } catch (Exception e) {
      // Unparseable quiz payloads are not sent to students at all.
      return "{}";
    }
  }
}
