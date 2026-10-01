package com.gems.shared.security;

/**
 * CORS is now configured per-service via Spring Security's native {@code .cors(...)} in each
 * SecurityConfig, registered at {@code SecurityWebFiltersOrder.CORS} — a stage that runs before
 * AUTHENTICATION, so a preflight OPTIONS request (which never carries an Authorization header)
 * is answered before the JWT filter ever sees it.
 *
 * <p>A generic {@code CorsWebFilter} bean here previously ran alongside that per-service Security
 * CORS config with no defined ordering between them, which either duplicated the
 * Access-Control-Allow-Origin header (browsers reject that outright) or — once the per-service
 * config was removed to fix the duplication — left it running after AUTHENTICATION, causing
 * every preflight to be rejected with 401 before CORS could respond. Kept as an empty class
 * (rather than deleted) so any lingering references fail to compile loudly instead of silently
 * reintroducing one of those two bugs.
 */
public final class CorsConfig {
  private CorsConfig() {
  }
}
