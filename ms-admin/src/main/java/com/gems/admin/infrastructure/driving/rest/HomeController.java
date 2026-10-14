package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.home.HomeModels.HomeVersion;
import com.gems.admin.application.home.HomeModels.HomeView;
import com.gems.admin.application.home.HomeModels.PublicHome;
import com.gems.admin.application.home.HomeModels.VersionSummary;
import com.gems.admin.application.home.HomeService;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Home personalizada de cada institución (HTML + CSS sin JavaScript). La administran el admin de la
 * institución y el super admin; el slug (subdominio) solo lo asigna el super admin.
 */
@RestController
@Tag(name = "Home", description = "Home personalizada por institución")
public class HomeController {
  private final HomeService homeService;

  public HomeController(HomeService homeService) {
    this.homeService = homeService;
  }

  public record DraftRequest(@Size(max = 10) String mode, @Size(max = 200_000) String html,
                             @Size(max = 100_000) String css) {
  }

  public record PublishRequest(@Size(max = 200) String note) {
  }

  public record SlugRequest(@NotBlank @Size(max = 40) String slug) {
  }

  @GetMapping("/api/v1/home/{companyId}")
  @Operation(summary = "Slug, borrador y versión publicada de la home")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> get(@PathVariable String companyId) {
    return canEdit(companyId).flatMap(caller -> homeService.get(companyId));
  }

  @PutMapping("/api/v1/home/{companyId}/draft")
  @Operation(summary = "Guarda el borrador (se sanea: sin JavaScript)")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> saveDraft(@PathVariable String companyId, @Valid @RequestBody DraftRequest request) {
    return canEdit(companyId).flatMap(caller -> homeService.saveDraft(companyId, request.mode(), request.html(), request.css(),
      caller.userId()));
  }

  @PostMapping("/api/v1/home/{companyId}/publish")
  @Operation(summary = "Publica el borrador como una nueva versión")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> publish(@PathVariable String companyId,
                                @Valid @RequestBody(required = false) PublishRequest request) {
    return canEdit(companyId).flatMap(caller -> homeService.publish(companyId,
      request == null ? null : request.note(), caller.userId()));
  }

  @DeleteMapping("/api/v1/home/{companyId}/publish")
  @Operation(summary = "Deja de mostrar la home personalizada; el historial se conserva")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> unpublish(@PathVariable String companyId) {
    return canEdit(companyId).flatMap(caller -> homeService.unpublish(companyId));
  }

  @GetMapping("/api/v1/home/{companyId}/versions")
  @Operation(summary = "Historial de versiones publicadas")
  @SecurityRequirement(name = "bearerAuth")
  public Flux<VersionSummary> versions(@PathVariable String companyId) {
    return canEdit(companyId).flatMapMany(caller -> homeService.versions(companyId));
  }

  @GetMapping("/api/v1/home/{companyId}/versions/{version}")
  @Operation(summary = "Contenido de una versión publicada")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeVersion> version(@PathVariable String companyId, @PathVariable int version) {
    return canEdit(companyId).flatMap(caller -> homeService.version(companyId, version));
  }

  @PostMapping("/api/v1/home/{companyId}/versions/{version}/restore")
  @Operation(summary = "Copia una versión anterior al borrador")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> restore(@PathVariable String companyId, @PathVariable int version) {
    return canEdit(companyId).flatMap(caller -> homeService.restore(companyId, version, caller.userId()));
  }

  @PutMapping("/api/v1/home/{companyId}/slug")
  @Operation(summary = "Asigna el slug (subdominio) de la institución; solo el super admin")
  @SecurityRequirement(name = "bearerAuth")
  public Mono<HomeView> assignSlug(@PathVariable String companyId, @Valid @RequestBody SlugRequest request) {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin can assign the slug")
      .flatMap(caller -> homeService.assignSlug(companyId, request.slug()));
  }

  /** Sin sesión: lo consume el front al abrir {slug}.dominio para cargar marca y home. */
  @GetMapping("/api/v1/public/home/{slug}")
  @Operation(summary = "Marca y home publicada de una institución por su slug")
  public Mono<ResponseEntity<PublicHome>> publicHome(@PathVariable String slug) {
    return homeService.publicBySlug(slug).map(home -> ResponseEntity.ok()
      .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
      .body(home));
  }

  private Mono<AuthenticatedUser> canEdit(String companyId) {
    return CurrentUser.require(caller -> caller.isSuperAdmin() || (caller.isAdmin() && caller.belongsTo(companyId)),
      "Only the super admin or the institution admin can manage the home");
  }
}
