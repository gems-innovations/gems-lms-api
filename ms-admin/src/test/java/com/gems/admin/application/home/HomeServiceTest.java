package com.gems.admin.application.home;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.HomeGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.home.HomeModels.HomeRecord;
import com.gems.admin.application.home.HomeModels.HomeVersion;
import com.gems.admin.domain.entities.Institution;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class HomeServiceTest {
  private HomeGateway homeGateway;
  private InstitutionGateway institutionGateway;
  private HomeService service;

  @BeforeEach
  void setUp() {
    homeGateway = mock(HomeGateway.class);
    institutionGateway = mock(InstitutionGateway.class);
    service = new HomeService(homeGateway, institutionGateway, mock(BrandingGateway.class));
    when(institutionGateway.findById("inst-1")).thenReturn(Mono.just(new Institution()));
  }

  private static HomeRecord record(String html, String css, Integer published) {
    return new HomeRecord("inst-1", "unal", "CUSTOM", html, css, LocalDateTime.now(), 1L, published);
  }

  @Test
  void shouldRejectInvalidSlug() {
    StepVerifier.create(service.assignSlug("inst-1", "Mi Universidad!"))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.BAD_REQUEST)
      .verify();
  }

  @Test
  void shouldRejectReservedSlug() {
    StepVerifier.create(service.assignSlug("inst-1", "www"))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.BAD_REQUEST)
      .verify();
  }

  @Test
  void shouldRejectSlugUsedByAnotherInstitution() {
    when(homeGateway.slugTaken("unal", "inst-1")).thenReturn(Mono.just(true));

    StepVerifier.create(service.assignSlug("inst-1", " UNAL "))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.CONFLICT)
      .verify();
  }

  @Test
  void shouldAssignSlugInLowerCase() {
    when(homeGateway.slugTaken("unal", "inst-1")).thenReturn(Mono.just(false));
    when(homeGateway.find("inst-1")).thenReturn(Mono.empty());
    when(homeGateway.save(any(HomeRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

    StepVerifier.create(service.assignSlug("inst-1", " UNAL "))
      .expectNextMatches(view -> "unal".equals(view.slug()))
      .verifyComplete();
  }

  @Test
  void shouldSanitizeDraftBeforeSaving() {
    when(homeGateway.find("inst-1")).thenReturn(Mono.empty());
    when(homeGateway.save(any(HomeRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

    StepVerifier.create(service.saveDraft("inst-1", "CUSTOM", "<p>Hola</p><script>x()</script>", ".a{color:red}", 7L))
      .expectNextMatches(view -> "<p>Hola</p>".equals(view.draft().html()) && view.draft().updatedBy() == 7L)
      .verifyComplete();
  }

  @Test
  void shouldPublishDraftAsNextVersion() {
    when(homeGateway.find("inst-1")).thenReturn(Mono.just(record("<p>x</p>", "", 2)));
    when(homeGateway.versions("inst-1")).thenReturn(Flux.just(
      new HomeVersion("inst-1", 1, "CUSTOM", "a", "", null, 1L, LocalDateTime.now()),
      new HomeVersion("inst-1", 2, "CUSTOM", "b", "", null, 1L, LocalDateTime.now())));
    when(homeGateway.saveVersion(any(HomeVersion.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
    when(homeGateway.save(any(HomeRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

    StepVerifier.create(service.publish("inst-1", "ajuste", 7L))
      .expectNextMatches(view -> view.publishedVersion() == 3)
      .verifyComplete();
  }

  @Test
  void shouldNotPublishEmptyDraft() {
    when(homeGateway.find("inst-1")).thenReturn(Mono.just(record("", "", null)));

    StepVerifier.create(service.publish("inst-1", null, 7L))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.BAD_REQUEST)
      .verify();
  }

  @Test
  void shouldRestoreVersionIntoDraftWithoutPublishing() {
    when(homeGateway.version("inst-1", 1)).thenReturn(
      Mono.just(new HomeVersion("inst-1", 1, "CUSTOM", "<p>viejo</p>", ".v{}", null, 1L, LocalDateTime.now())));
    when(homeGateway.find("inst-1")).thenReturn(Mono.just(record("<p>nuevo</p>", "", 2)));
    when(homeGateway.save(any(HomeRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

    StepVerifier.create(service.restore("inst-1", 1, 7L))
      .expectNextMatches(view -> "<p>viejo</p>".equals(view.draft().html()) && view.publishedVersion() == 2)
      .verifyComplete();
    verify(homeGateway).save(any(HomeRecord.class));
  }

  @Test
  void shouldReturnNotFoundForUnknownSlug() {
    when(homeGateway.findBySlug("nadie")).thenReturn(Mono.empty());

    StepVerifier.create(service.publicBySlug("nadie"))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.NOT_FOUND)
      .verify();
  }

  @Test
  void shouldRejectUnknownMode() {
    StepVerifier.create(service.saveDraft("inst-1", "OTRO", "<p>x</p>", "", 7L))
      .expectErrorMatches(e -> e instanceof ResponseStatusException r && r.getStatusCode() == HttpStatus.BAD_REQUEST)
      .verify();
  }

  @Test
  void shouldDefaultToBaseModeAndPublishBaseWithoutHtml() {
    when(homeGateway.find("inst-1")).thenReturn(Mono.just(
      new HomeRecord("inst-1", "unal", "BASE", null, ".tb-hero{color:red}", LocalDateTime.now(), 1L, null)));
    when(homeGateway.versions("inst-1")).thenReturn(Flux.empty());
    when(homeGateway.saveVersion(any(HomeVersion.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
    when(homeGateway.save(any(HomeRecord.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

    StepVerifier.create(service.publish("inst-1", null, 7L))
      .expectNextMatches(view -> view.publishedVersion() == 1 && "BASE".equals(view.draft().mode()))
      .verifyComplete();
  }
}
