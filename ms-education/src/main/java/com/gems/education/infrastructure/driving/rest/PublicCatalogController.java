package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import com.gems.shared.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What anyone can see without signing in: the free courses of the open institution and the form
 * for institutions that want their own space. Only titles and structure are public; the lessons
 * themselves need a (guest) session.
 */
@RestController
public class PublicCatalogController {

  /** Optional so the class can be built in tests without e-mail. */
  private com.gems.education.application.gateway.EmailNoticeGateway emails;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  void setEmails(com.gems.education.application.gateway.EmailNoticeGateway emails) {
    this.emails = emails;
  }
  private static final int MAX_REQUESTS_PER_DAY = 5;

  private final CourseGateway courses;
  private final DatabaseClient db;
  private final String openInstitutionId;
  private final Map<String, int[]> requestsByClient = new ConcurrentHashMap<>();

  public PublicCatalogController(@org.springframework.beans.factory.annotation.Qualifier("courseGateway") CourseGateway courses, DatabaseClient db,
                                 @Value("${open.institution.id:gems-abierto}") String openInstitutionId) {
    this.courses = courses;
    this.db = db;
    this.openInstitutionId = openInstitutionId;
  }

  public record LessonSummary(Long id, String title, int blocks, boolean free) {}

  public record ModuleSummary(Long id, String title, List<LessonSummary> lessons) {}

  public record PublicCourse(Long id, String title, String description, String thumbnailUrl, String difficulty,
                             List<String> tags, String instructorName, int modules, int lessons, Integer durationMinutes,
                             Integer enrolled, LocalDateTime publishedAt, List<ModuleSummary> outline) {}

  @GetMapping("/api/v1/public/courses")
  public Mono<ResponseEntity<List<PublicCourse>>> catalog() {
    return courses.findByInstitutionId(openInstitutionId)
      .filter(c -> "published".equalsIgnoreCase(c.getStatus()))
      .map(c -> summary(c, false))
      .collectSortedList(Comparator.comparing(PublicCourse::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
      .map(list -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(list));
  }

  @GetMapping("/api/v1/public/courses/{id}")
  public Mono<ResponseEntity<PublicCourse>> course(@PathVariable Long id) {
    return courses.findById(id)
      .filter(c -> openInstitutionId.equals(c.getInstitutionId()) && "published".equalsIgnoreCase(c.getStatus()))
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found")))
      .map(c -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(summary(c, true)));
  }

  private static PublicCourse summary(Course c, boolean withOutline) {
    List<Module> modules = c.getModules() == null ? List.of() : c.getModules();
    int lessons = modules.stream().mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size()).sum();
    List<ModuleSummary> outline = !withOutline ? List.of() : modules.stream()
      .sorted(Comparator.comparing(m -> Objects.requireNonNullElse(m.getOrderIndex(), 0)))
      .map(m -> new ModuleSummary(m.getId(), m.getTitle(), (m.getLessons() == null ? List.<Lesson>of() : m.getLessons()).stream()
        .sorted(Comparator.comparing(l -> Objects.requireNonNullElse(l.getOrderIndex(), 0)))
        .map(l -> new LessonSummary(l.getId(), l.getTitle(), l.getContents() == null ? 0 : l.getContents().size(),
          Boolean.TRUE.equals(l.getIsFree())))
        .toList()))
      .toList();
    return new PublicCourse(c.getId(), c.getTitle(), c.getDescription(), c.getThumbnailUrl(), c.getDifficulty(), c.getTags(),
      c.getInstructorName(), modules.size(), lessons, c.getTotalDuration(), c.getEnrolledCount(), c.getPublishedAt(), outline);
  }

  // ── Institutions that want their own space ─────────────────────────────────

  public record InstitutionRequest(String institutionName, String contactName, String email, String phone,
                                   String role, Integer students, String message) {}

  public record StoredRequest(Long id, String institutionName, String contactName, String email, String phone,
                              String role, Integer students, String message, String status, LocalDateTime createdAt) {}

  @PostMapping("/api/v1/public/institution-requests")
  public Mono<ResponseEntity<Map<String, Object>>> requestInstitution(@RequestBody InstitutionRequest body, ServerWebExchange exchange) {
    if (body == null || blank(body.institutionName()) || blank(body.contactName()) || blank(body.email())
        || !body.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Institution, contact name and a valid email are required"));
    }
    String client = RateLimitFilter.getClientId(exchange.getRequest());
    int day = (int) (Instant.now().getEpochSecond() / 86_400);
    int[] slot = requestsByClient.compute(client, (k, v) -> v == null || v[0] != day ? new int[]{day, 1} : new int[]{day, v[1] + 1});
    if (slot[1] > MAX_REQUESTS_PER_DAY) {
      return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests from this network today"));
    }
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO institution_requests(institution_name, contact_name, email, phone, role, students, message) "
        + "VALUES (:name, :contact, :email, :phone, :role, :students, :message) RETURNING id")
      .bind("name", cut(body.institutionName(), 160)).bind("contact", cut(body.contactName(), 120))
      .bind("email", cut(body.email().trim().toLowerCase(), 160));
    spec = blank(body.phone()) ? spec.bindNull("phone", String.class) : spec.bind("phone", cut(body.phone(), 40));
    spec = blank(body.role()) ? spec.bindNull("role", String.class) : spec.bind("role", cut(body.role(), 60));
    spec = body.students() == null ? spec.bindNull("students", Integer.class) : spec.bind("students", Math.max(0, body.students()));
    spec = blank(body.message()) ? spec.bindNull("message", String.class) : spec.bind("message", cut(body.message(), 2000));
    return spec.map((row, meta) -> row.get("id", Long.class)).one()
      .flatMap(id -> notifyRequest(body).thenReturn(id))
      .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of("id", id, "status", "received")));
  }

  private Mono<Void> notifyRequest(InstitutionRequest body) {
    if (emails == null) return Mono.empty();
    String name = cut(body.institutionName(), 160);
    return emails.sendToAddress(body.email().trim(), cut(body.contactName(), 80), "Recibimos tu solicitud para " + name,
        "Gracias por querer enseñar con GEMS. Recibimos la solicitud de «" + name + "» y te escribiremos pronto a este "
          + "correo para crear tu espacio. Mientras tanto, puedes ver cómo se estudia en nuestros cursos gratis.",
        "/", "Ver los cursos gratis")
      .then(emails.sendToAddress(com.gems.education.application.gateway.EmailNoticeGateway.SUPER_ADMINS, null,
        "Nueva solicitud de espacio: " + name,
        cut(body.contactName(), 120) + " (" + body.email().trim() + ") pidió un espacio para «" + name + "»"
          + (body.students() == null ? "" : ", con hasta " + body.students() + " personas") + ".",
        "/solicitudes", "Ver las solicitudes"));
  }

  /** The super admin follows up the requests. */
  @GetMapping("/api/v1/institution-requests")
  public Mono<ResponseEntity<List<StoredRequest>>> requests() {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin sees institution requests")
      .flatMap(caller -> db.sql("SELECT id, institution_name, contact_name, email, phone, role, students, message, status, created_at "
          + "FROM institution_requests ORDER BY created_at DESC LIMIT 500")
        .map((r, meta) -> new StoredRequest(r.get("id", Long.class), r.get("institution_name", String.class),
          r.get("contact_name", String.class), r.get("email", String.class), r.get("phone", String.class),
          r.get("role", String.class), r.get("students", Integer.class), r.get("message", String.class),
          r.get("status", String.class), r.get("created_at", LocalDateTime.class)))
        .all().collectList())
      .map(ResponseEntity::ok);
  }

  private static boolean blank(String s) { return s == null || s.isBlank(); }

  private static String cut(String s, int max) { String t = s.trim(); return t.length() > max ? t.substring(0, max) : t; }
}
