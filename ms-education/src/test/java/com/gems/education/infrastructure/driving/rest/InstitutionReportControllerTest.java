package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.InstitutionReportUseCase;
import com.gems.education.application.InstitutionReportUseCase.InstitutionReport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.*;

class InstitutionReportControllerTest extends ControllerTestSupport {
  private final InstitutionReportUseCase reports = mock(InstitutionReportUseCase.class);
  private final InstitutionReportController controller = new InstitutionReportController(reports);
  private final InstitutionReport report = new InstitutionReport("inst-1", "2026-01-01T00:00:00Z", 1, 2, 1, 1, 2, 50, 60, 1, List.of());

  @Test
  void staffOfTheInstitutionGetsTheReportAndCsv() {
    when(reports.execute("inst-1")).thenReturn(Mono.just(report));
    client(controller, ADMIN).get().uri("/api/v1/reports/institutions/inst-1").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.totalCourses").isEqualTo(1);
    client(controller, ADMIN).get().uri("/api/v1/reports/institutions/inst-1/export").exchange().expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith("text/csv");
  }

  @Test
  void studentsAndOtherInstitutionsAreForbidden() {
    client(controller, STUDENT).get().uri("/api/v1/reports/institutions/inst-1").exchange().expectStatus().isForbidden();
    client(controller, OTHER_ADMIN).get().uri("/api/v1/reports/institutions/inst-1").exchange().expectStatus().isForbidden();
    verify(reports, never()).execute(anyString());
  }
}
