package com.gems.admin.application.home;

import com.gems.admin.application.exceptions.InstitutionNotFoundException;
import com.gems.admin.application.gateway.BrandingGateway;
import com.gems.admin.application.gateway.HomeGateway;
import com.gems.admin.application.gateway.InstitutionGateway;
import com.gems.admin.application.home.HomeModels.Draft;
import com.gems.admin.application.home.HomeModels.HomeRecord;
import com.gems.admin.application.home.HomeModels.HomeVersion;
import com.gems.admin.application.home.HomeModels.HomeView;
import com.gems.admin.application.home.HomeModels.PublicBranding;
import com.gems.admin.application.home.HomeModels.PublicHome;
import com.gems.admin.application.home.HomeModels.VersionSummary;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Home personalizada: borrador, publicación con historial de versiones y resolución pública por slug. */
public class HomeService {
  static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$");
  static final Set<String> RESERVED = Set.of("www", "api", "app", "admin", "lms", "auth", "edu", "mail", "static",
    "assets", "cdn", "login", "status", "docs", "blog", "soporte", "gems",
    // Ambientes: nunca son una institución.
    "dev", "qa", "pdn", "prod", "staging", "test", "preview");

  private final HomeGateway homeGateway;
  private final InstitutionGateway institutionGateway;
  private final BrandingGateway brandingGateway;

  public HomeService(HomeGateway homeGateway, InstitutionGateway institutionGateway, BrandingGateway brandingGateway) {
    this.homeGateway = homeGateway;
    this.institutionGateway = institutionGateway;
    this.brandingGateway = brandingGateway;
  }

  public Mono<HomeView> get(String institutionId) {
    return ensureInstitution(institutionId).then(Mono.defer(() -> load(institutionId))).map(HomeService::view);
  }

  public Mono<HomeView> saveDraft(String institutionId, String mode, String html, String css, Long userId) {
    String cleanMode = normalizeMode(mode);
    if (cleanMode == null) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El modo debe ser BASE o CUSTOM"));
    }
    if (html != null && html.length() > HomeContentSanitizer.MAX_HTML
      || css != null && css.length() > HomeContentSanitizer.MAX_CSS) {
      return Mono.error(new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
        "El HTML admite hasta " + HomeContentSanitizer.MAX_HTML + " caracteres y el CSS hasta "
          + HomeContentSanitizer.MAX_CSS));
    }
    String cleanHtml = HomeContentSanitizer.cleanHtml(html);
    String cleanCss = HomeContentSanitizer.cleanCss(css);
    return ensureInstitution(institutionId).then(Mono.defer(() -> load(institutionId)))
      .flatMap(record -> homeGateway.save(record.withDraft(cleanMode, cleanHtml, cleanCss, userId)))
      .map(HomeService::view);
  }

  /** Congela el borrador como una nueva versión y la deja como la publicada. */
  public Mono<HomeView> publish(String institutionId, String note, Long userId) {
    return ensureInstitution(institutionId).then(Mono.defer(() -> load(institutionId))).flatMap(record -> {
      if (HomeModels.MODE_CUSTOM.equals(record.draftMode()) && isBlank(record.draftHtml())) {
        return Mono.<HomeRecord>error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "En modo CUSTOM el HTML no puede estar vacío"));
      }
      return homeGateway.versions(institutionId).map(HomeVersion::version).reduce(0, Math::max)
        .flatMap(max -> homeGateway.saveVersion(new HomeVersion(institutionId, max + 1, record.draftMode(), record.draftHtml(),
          record.draftCss(), trim(note, 200), userId, LocalDateTime.now())))
        .flatMap(saved -> homeGateway.save(record.withPublished(saved.version())));
    }).map(HomeService::view);
  }

  /** Deja de mostrar la home personalizada (se conserva el historial). */
  public Mono<HomeView> unpublish(String institutionId) {
    return ensureInstitution(institutionId).then(Mono.defer(() -> load(institutionId)))
      .flatMap(record -> homeGateway.save(record.withPublished(null))).map(HomeService::view);
  }

  public Flux<VersionSummary> versions(String institutionId) {
    return ensureInstitution(institutionId).then(Mono.defer(() -> load(institutionId))).flatMapMany(record ->
      homeGateway.versions(institutionId)
        .sort((a, b) -> Integer.compare(b.version(), a.version()))
        .map(v -> new VersionSummary(v.version(), v.mode(), v.note(), v.publishedBy(), v.publishedAt(),
          record.publishedVersion() != null && record.publishedVersion() == v.version())));
  }

  public Mono<HomeVersion> version(String institutionId, int version) {
    return homeGateway.version(institutionId, version)
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Versión no encontrada")));
  }

  /** Copia una versión anterior al borrador; para publicarla de nuevo hay que publicar. */
  public Mono<HomeView> restore(String institutionId, int version, Long userId) {
    return version(institutionId, version).flatMap(v -> load(institutionId)
      .flatMap(record -> homeGateway.save(record.withDraft(v.mode(), v.html(), v.css(), userId)))).map(HomeService::view);
  }

  /** Solo el super admin asigna el slug; es único y es el subdominio de la institución. */
  public Mono<HomeView> assignSlug(String institutionId, String rawSlug) {
    String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase(Locale.ROOT);
    if (!SLUG.matcher(slug).matches()) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "El slug debe tener de 3 a 40 caracteres: minúsculas, números y guiones, sin empezar ni terminar con guion"));
    }
    if (RESERVED.contains(slug)) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese slug está reservado"));
    }
    return ensureInstitution(institutionId)
      .then(Mono.defer(() -> homeGateway.slugTaken(slug, institutionId)))
      .flatMap(taken -> taken
        ? Mono.<HomeRecord>error(new ResponseStatusException(HttpStatus.CONFLICT, "Ese slug ya lo usa otra institución"))
        : load(institutionId).flatMap(record -> homeGateway.save(record.withSlug(slug))))
      .map(HomeService::view);
  }

  /** Lo que ve el visitante: marca y versión publicada; 404 si el slug no existe. */
  public Mono<PublicHome> publicBySlug(String rawSlug) {
    String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase(Locale.ROOT);
    return homeGateway.findBySlug(slug)
      .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Institución no encontrada")))
      .flatMap(record -> institutionGateway.findById(record.institutionId()).flatMap(institution ->
        publishedContent(record).flatMap(version -> brandingGateway.findByCompanyId(record.institutionId())
          .map(b -> new PublicBranding(b.getPrimaryColor(), b.getSecondaryColor(), b.getAccentColor(),
            b.getLogoUrl(), b.getFaviconUrl(), b.getDarkMode()))
          .map(branding -> toPublic(record, institution.getName(), version, branding))
          .switchIfEmpty(Mono.fromSupplier(() -> toPublic(record, institution.getName(), version, null))))));
  }

  private Mono<HomeVersion> publishedContent(HomeRecord record) {
    HomeVersion none = new HomeVersion(record.institutionId(), 0, HomeModels.MODE_BASE, null, null, null, null, null);
    if (record.publishedVersion() == null) {
      return Mono.just(none);
    }
    return homeGateway.version(record.institutionId(), record.publishedVersion()).defaultIfEmpty(none);
  }

  private static PublicHome toPublic(HomeRecord record, String name, HomeVersion version, PublicBranding branding) {
    return new PublicHome(record.institutionId(), name, record.slug(), version.version() == 0 ? null : version.version(),
      version.mode(), version.html(), version.css(), branding);
  }

  private Mono<Void> ensureInstitution(String institutionId) {
    return institutionGateway.findById(institutionId)
      .switchIfEmpty(Mono.error(new InstitutionNotFoundException("Institution not found: " + institutionId)))
      .then();
  }

  private Mono<HomeRecord> load(String institutionId) {
    return homeGateway.find(institutionId)
      .defaultIfEmpty(new HomeRecord(institutionId, null, HomeModels.MODE_BASE, null, null, null, null, null));
  }

  private static HomeView view(HomeRecord record) {
    return new HomeView(record.institutionId(), record.slug(),
      new Draft(record.draftMode(), record.draftHtml(), record.draftCss(), record.draftUpdatedAt(), record.draftUpdatedBy()),
      record.publishedVersion());
  }

  /** Null si el modo no es válido; BASE cuando no se indica. */
  private static String normalizeMode(String mode) {
    if (mode == null || mode.isBlank()) {
      return HomeModels.MODE_BASE;
    }
    String upper = mode.trim().toUpperCase(Locale.ROOT);
    return HomeModels.MODE_BASE.equals(upper) || HomeModels.MODE_CUSTOM.equals(upper) ? upper : null;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String trim(String value, int max) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
  }
}
