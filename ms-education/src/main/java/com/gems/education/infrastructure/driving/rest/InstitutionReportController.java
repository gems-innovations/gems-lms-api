package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.InstitutionReportUseCase;
import com.gems.education.application.InstitutionReportUseCase.InstitutionReport;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/reports/institutions/{institutionId}")
public class InstitutionReportController {
  private final InstitutionReportUseCase reports;
  public InstitutionReportController(InstitutionReportUseCase reports) { this.reports = reports; }

  @GetMapping
  public Mono<ResponseEntity<InstitutionReport>> report(@PathVariable String institutionId) {
    return authorized(institutionId).then(Mono.defer(() -> reports.execute(institutionId))).map(ResponseEntity::ok);
  }

  @GetMapping(value = "/export", produces = "text/csv")
  public Mono<ResponseEntity<byte[]>> export(@PathVariable String institutionId) {
    return authorized(institutionId).then(Mono.defer(() -> reports.execute(institutionId))).map(report -> ResponseEntity.ok()
      .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-" + institutionId + ".csv").build().toString())
      .body(csv(report).getBytes(StandardCharsets.UTF_8)));
  }

  private Mono<?> authorized(String institutionId) {
    return CurrentUser.require(caller -> caller.isSuperAdmin() || (caller.isStaff() && caller.belongsTo(institutionId)),
      "Only institution staff can view its reports");
  }

  private static String csv(InstitutionReport report) {
    StringBuilder value = new StringBuilder("\uFEFFCurso,Estado,Matriculas,Activas,Completadas,Finalizacion %,Progreso promedio %,Entregas pendientes,Valoracion,Numero de valoraciones\r\n");
    report.courses().forEach(course -> value.append(escape(course.title())).append(',').append(escape(course.status())).append(',')
      .append(course.enrollments()).append(',').append(course.activeEnrollments()).append(',').append(course.completedEnrollments()).append(',')
      .append(course.completionRate()).append(',').append(course.averageProgress()).append(',').append(course.pendingSubmissions()).append(',')
      .append(course.averageRating() == null ? "" : course.averageRating()).append(',').append(course.ratingCount() == null ? "" : course.ratingCount()).append("\r\n"));
    return value.toString();
  }
  private static String escape(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
}
