package com.gems.admin.application.home;

import java.time.LocalDateTime;

/** Registros de la home personalizada de una institución. */
public final class HomeModels {
  /** Landing de GEMS con la marca y el CSS de la institución (el HTML es un banner opcional). */
  public static final String MODE_BASE = "BASE";
  /** El HTML de la institución reemplaza la landing completa. */
  public static final String MODE_CUSTOM = "CUSTOM";

  private HomeModels() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Estado guardado: slug, borrador y versión publicada (null si no hay). */
  public record HomeRecord(String institutionId, String slug, String draftMode, String draftHtml, String draftCss,
                           LocalDateTime draftUpdatedAt, Long draftUpdatedBy, Integer publishedVersion) {
    public HomeRecord withSlug(String newSlug) {
      return new HomeRecord(institutionId, newSlug, draftMode, draftHtml, draftCss, draftUpdatedAt, draftUpdatedBy,
        publishedVersion);
    }

    public HomeRecord withDraft(String mode, String html, String css, Long userId) {
      return new HomeRecord(institutionId, slug, mode, html, css, LocalDateTime.now(), userId, publishedVersion);
    }

    public HomeRecord withPublished(Integer version) {
      return new HomeRecord(institutionId, slug, draftMode, draftHtml, draftCss, draftUpdatedAt, draftUpdatedBy,
        version);
    }
  }

  /** Una versión publicada (historial). */
  public record HomeVersion(String institutionId, int version, String mode, String html, String css, String note,
                            Long publishedBy, LocalDateTime publishedAt) {
  }

  public record Draft(String mode, String html, String css, LocalDateTime updatedAt, Long updatedBy) {
  }

  /** Vista para el panel de administración. */
  public record HomeView(String institutionId, String slug, Draft draft, Integer publishedVersion) {
  }

  /** Versión sin contenido, para listar el historial. */
  public record VersionSummary(int version, String mode, String note, Long publishedBy, LocalDateTime publishedAt,
                               boolean current) {
  }

  public record PublicBranding(String primaryColor, String secondaryColor, String accentColor, String logoUrl,
                               String faviconUrl, Boolean darkMode) {
  }

  /** Lo que cualquier visitante del subdominio necesita para pintar la home. Sin versión publicada: BASE vacía. */
  public record PublicHome(String institutionId, String name, String slug, Integer version, String mode, String html,
                           String css, PublicBranding branding) {
  }
}
