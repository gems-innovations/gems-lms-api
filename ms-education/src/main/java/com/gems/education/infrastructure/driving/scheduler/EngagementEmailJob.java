package com.gems.education.infrastructure.driving.scheduler;

import com.gems.education.application.EngagementPlanner;
import com.gems.education.application.gateway.EmailNoticeGateway;
import com.gems.shared.security.InternalApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monday to Saturday (11 a.m., Bogotá, never on public holidays) decides and sends the motivation e-mails of {@link EngagementPlanner}.
 * ms-auth applies each student's «tips» preference and skips guests. What was sent is logged in
 * {@code engagement_emails}, so nothing is repeated.
 */
@RestController
@EnableScheduling
public class EngagementEmailJob {
  private static final Logger log = LoggerFactory.getLogger(EngagementEmailJob.class);
  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

  private final DatabaseClient db;
  private final EmailNoticeGateway emails;
  private final com.gems.education.application.NotificationUseCase notifications;
  private final InternalApiKey internalKey;
  private final Clock clock;

  public EngagementEmailJob(DatabaseClient db, EmailNoticeGateway emails,
                            com.gems.education.application.NotificationUseCase notifications,
                            InternalApiKey internalKey) {
    this.notifications = notifications;
    this.db = db;
    this.emails = emails;
    this.internalKey = internalKey;
    this.clock = Clock.system(BOGOTA);
  }

  /**
   * Lunes a sábado a las 11 a. m. y nunca en festivos: dentro del horario que la Ley 2300 de 2023 permite
   * para mensajes promocionales (lun-vie 7 a. m.-7 p. m., sáb 8 a. m.-3 p. m., sin domingos ni festivos).
   */
  @Scheduled(cron = "${engagement.cron:0 0 11 * * MON-SAT}", zone = "America/Bogota")
  public void daily() {
    if (com.gems.education.application.ColombianHolidays.isHoliday(LocalDate.now(clock))) {
      log.info("Engagement e-mails skipped: public holiday");
      return;
    }
    run().subscribe(sent -> log.info("Engagement e-mails sent: {}", sent),
      error -> log.warn("Engagement e-mails failed: {}", error.getMessage()));
  }

  /** Runs it now (to try it locally). Service-to-service only: internal key, not routed by the gateway. */
  @PostMapping("/internal/engagement/run")
  public Mono<ResponseEntity<Map<String, Long>>> runNow(@RequestHeader(InternalApiKey.HEADER) String key) {
    internalKey.require(key);
    return run().map(sent -> ResponseEntity.ok(Map.of("sent", sent)));
  }

  Mono<Long> run() {
    LocalDate today = LocalDate.now(clock);
    return Mono.zip(activeEnrollments(), activityDays(today.minusDays(60)), sentLog(today.minusDays(30)))
      .flatMapMany(t -> Flux.fromIterable(students(t.getT1(), t.getT2(), t.getT3(), today)))
      .concatMap(student -> Mono.justOrEmpty(EngagementPlanner.plan(student, today))
        // Shown in the bell too, so it also reaches students who turned motivation e-mails off.
        .flatMap(email -> notifications.motivation(student.studentId(), email.courseId(), email.subject(), email.message())
          .then(emails.sendTip(student.studentId(), email.subject(), email.message(), email.linkPath(),
            email.linkLabel())
          .then(db.sql("INSERT INTO engagement_emails(student_id, kind, sent_on) VALUES (:s, :k, :d)")
            .bind("s", student.studentId()).bind("k", email.kind()).bind("d", today).then())
          .thenReturn(1L))))
      .reduce(0L, Long::sum);
  }

  private record Row(Long studentId, Long courseId, String title, int progress) {
  }

  private Mono<List<Row>> activeEnrollments() {
    return db.sql("SELECT e.student_id, e.course_id, c.title, COALESCE(e.progress, 0) AS progress FROM enrollments e "
        + "JOIN courses c ON c.id = e.course_id WHERE e.status <> 'completed' AND e.completed_at IS NULL")
      .map((r, m) -> new Row(r.get("student_id", Long.class), r.get("course_id", Long.class),
        r.get("title", String.class), r.get("progress", Integer.class)))
      .all().collectList();
  }

  private Mono<Map<Long, Set<LocalDate>>> activityDays(LocalDate from) {
    return db.sql("SELECT student_id, day FROM student_activity_days WHERE day >= :from").bind("from", from)
      .map((r, m) -> Map.entry(r.get("student_id", Long.class), r.get("day", LocalDate.class)))
      .all()
      .collect(HashMap::new, (map, e) -> map.computeIfAbsent(e.getKey(), k -> new HashSet<>()).add(e.getValue()));
  }

  /** Kinds sent per student in the last 30 days (also tells who got something this week). */
  private Mono<Map<Long, List<Map.Entry<String, LocalDate>>>> sentLog(LocalDate from) {
    return db.sql("SELECT student_id, kind, sent_on FROM engagement_emails WHERE sent_on >= :from").bind("from", from)
      .map((r, m) -> Map.entry(r.get("student_id", Long.class), Map.entry(r.get("kind", String.class),
        r.get("sent_on", LocalDate.class))))
      .all()
      .collect(HashMap::new, (map, e) -> map.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue()));
  }

  private static List<EngagementPlanner.Student> students(List<Row> rows, Map<Long, Set<LocalDate>> days,
      Map<Long, List<Map.Entry<String, LocalDate>>> sent, LocalDate today) {
    Map<Long, List<Integer>> classProgress = new HashMap<>();
    rows.forEach(r -> classProgress.computeIfAbsent(r.courseId(), k -> new ArrayList<>()).add(r.progress()));
    Map<Long, List<EngagementPlanner.Course>> byStudent = new HashMap<>();
    for (Row r : rows) {
      List<Integer> peers = classProgress.get(r.courseId());
      byStudent.computeIfAbsent(r.studentId(), k -> new ArrayList<>()).add(new EngagementPlanner.Course(r.courseId(),
        r.title(), r.progress(), EngagementPlanner.percentile(r.progress(), peers), peers.size()));
    }
    List<EngagementPlanner.Student> students = new ArrayList<>();
    byStudent.forEach((id, courses) -> {
      Set<LocalDate> active = days.getOrDefault(id, Set.of());
      LocalDate lastActive = active.stream().max(LocalDate::compareTo).orElse(LocalDate.MIN);
      List<Map.Entry<String, LocalDate>> log = sent.getOrDefault(id, List.of());
      // At most one motivation e-mail a week, whatever its kind.
      boolean sentThisWeek = log.stream().anyMatch(e -> e.getValue().isAfter(today.minusDays(7)));
      // Only what was sent during the current absence counts (a new absence can get "miss3" again);
      // «top» counts for the past 6 days so it stays weekly.
      Set<String> kinds = new HashSet<>();
      log.forEach(e -> {
        if (EngagementPlanner.TOP.equals(e.getKey()) ? e.getValue().isAfter(today.minusDays(6)) : e.getValue().isAfter(lastActive)) {
          kinds.add(e.getKey());
        }
      });
      students.add(new EngagementPlanner.Student(id, courses, active, sentThisWeek, kinds));
    });
    return students;
  }
}
