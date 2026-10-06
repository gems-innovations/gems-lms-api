package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.GetCourseActivityUseCase;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.TextSimilarity;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools that make grading faster: similarity between the submissions of an assignment and the
 * teacher's reusable feedback comments.
 */
@RestController
@RequestMapping("/api/v1")
public class GradingToolsController {
  private static final double DEFAULT_THRESHOLD = 0.35;
  private static final int MAX_SNIPPETS = 100;
  private static final int MAX_SNIPPET_LENGTH = 2000;

  private final GetCourseActivityUseCase activity;
  private final EducationAccess access;
  private final DatabaseClient db;

  public GradingToolsController(GetCourseActivityUseCase activity, EducationAccess access, DatabaseClient db) {
    this.activity = activity;
    this.access = access;
    this.db = db;
  }

  public record SimilarityReport(Long blockId, int compared, double threshold, List<TextSimilarity.Match> matches) {
  }

  /** Pairs of submissions of the block whose text is suspiciously alike. Staff of the course only. */
  @GetMapping("/courses/{courseId}/blocks/{blockId}/similarity")
  public Mono<ResponseEntity<SimilarityReport>> similarity(@PathVariable Long courseId, @PathVariable Long blockId,
                                                           @RequestParam(required = false) Double threshold) {
    double min = threshold == null ? DEFAULT_THRESHOLD : Math.max(0.1, Math.min(1, threshold));
    return access.editableCourse(courseId)
      .then(activity.submissionsOfCourse(courseId).filter(s -> blockId.equals(s.blockId())).collectList())
      .map(subs -> {
        Map<Long, String> texts = new LinkedHashMap<>();
        for (AssignmentSubmission s : subs) if (s.textContent() != null) texts.put(s.id(), s.textContent());
        return ResponseEntity.ok(new SimilarityReport(blockId, texts.size(), min, TextSimilarity.matches(texts, min)));
      });
  }

  // ── Reusable feedback comments (per teacher) ───────────────────────────────

  public record Snippet(Long id, String text) {
  }

  public record SnippetRequest(String text) {
  }

  @GetMapping("/activity/feedback-snippets")
  public Mono<ResponseEntity<List<Snippet>>> snippets() {
    return staff().flatMap(caller -> db.sql("SELECT id, text FROM feedback_snippets WHERE owner_id = :owner ORDER BY uses DESC, id DESC")
        .bind("owner", caller.userId())
        .map((row, meta) -> new Snippet(row.get("id", Long.class), row.get("text", String.class))).all().collectList())
      .map(ResponseEntity::ok);
  }

  @PostMapping("/activity/feedback-snippets")
  public Mono<ResponseEntity<Snippet>> saveSnippet(@RequestBody SnippetRequest body) {
    String text = body == null || body.text() == null ? "" : body.text().trim();
    if (text.isEmpty() || text.length() > MAX_SNIPPET_LENGTH) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Write a comment of up to " + MAX_SNIPPET_LENGTH + " characters"));
    }
    return staff().flatMap(caller -> db.sql("SELECT COUNT(*) AS n FROM feedback_snippets WHERE owner_id = :owner")
        .bind("owner", caller.userId()).map((row, meta) -> row.get("n", Long.class)).one()
        .flatMap(count -> count >= MAX_SNIPPETS
          ? Mono.<Snippet>error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Up to " + MAX_SNIPPETS + " saved comments"))
          : db.sql("INSERT INTO feedback_snippets(owner_id, text) VALUES (:owner, :text) "
                + "ON CONFLICT (owner_id, md5(text)) DO UPDATE SET uses = feedback_snippets.uses RETURNING id, text")
              .bind("owner", caller.userId()).bind("text", text)
              .map((row, meta) -> new Snippet(row.get("id", Long.class), row.get("text", String.class))).one()))
      .map(s -> ResponseEntity.status(HttpStatus.CREATED).body(s));
  }

  /** Counts a use so the most used comments come first. */
  @PostMapping("/activity/feedback-snippets/{id}/use")
  public Mono<ResponseEntity<Void>> useSnippet(@PathVariable Long id) {
    return staff().flatMap(caller -> db.sql("UPDATE feedback_snippets SET uses = uses + 1 WHERE id = :id AND owner_id = :owner")
        .bind("id", id).bind("owner", caller.userId()).then())
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  @DeleteMapping("/activity/feedback-snippets/{id}")
  public Mono<ResponseEntity<Void>> deleteSnippet(@PathVariable Long id) {
    return staff().flatMap(caller -> db.sql("DELETE FROM feedback_snippets WHERE id = :id AND owner_id = :owner")
        .bind("id", id).bind("owner", caller.userId()).then())
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  private Mono<AuthenticatedUser> staff() {
    return CurrentUser.require(AuthenticatedUser::isStaff, "Only staff keep feedback comments");
  }
}
