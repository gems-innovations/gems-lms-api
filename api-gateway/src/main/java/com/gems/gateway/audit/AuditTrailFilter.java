package com.gems.gateway.audit;

import com.gems.shared.security.InternalApiKey;
import com.gems.shared.security.RateLimitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/** Captures authenticated mutations after the downstream service has produced its status. */
@Component
public class AuditTrailFilter implements GlobalFilter, Ordered {
  private static final Logger LOG = LoggerFactory.getLogger(AuditTrailFilter.class);
  private static final Set<HttpMethod> MUTATIONS = Set.of(HttpMethod.POST, HttpMethod.PUT,
    HttpMethod.PATCH, HttpMethod.DELETE);
  private final WebClient auth;
  private final InternalApiKey auditKey;

  public AuditTrailFilter(@Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authUrl,
                          InternalApiKey auditKey) {
    this.auth = WebClient.builder().baseUrl(authUrl).build();
    this.auditKey = auditKey;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!shouldAudit(exchange)) return chain.filter(exchange);
    return chain.filter(exchange).then(Mono.defer(() -> record(exchange)
      .onErrorResume(error -> {
        LOG.warn("Could not persist audit event for {} {}: {}", exchange.getRequest().getMethod(),
          exchange.getRequest().getPath().value(), error.getMessage());
        return Mono.empty();
      })));
  }

  private Mono<Void> record(ServerWebExchange exchange) {
    String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    int status = exchange.getResponse().getStatusCode() == null ? 0
      : exchange.getResponse().getStatusCode().value();
    var event = new AuditRequest(action(exchange.getRequest().getMethod()),
      exchange.getRequest().getMethod().name(), exchange.getRequest().getPath().value(), status,
      clientIp(exchange), exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT));
    return auth.post().uri("/internal/audit/events")
      .header(HttpHeaders.AUTHORIZATION, authorization)
      .header("X-Audit-Key", auditKey.value())
      .bodyValue(event).retrieve().toBodilessEntity().then();
  }

  private static boolean shouldAudit(ServerWebExchange exchange) {
    HttpMethod method = exchange.getRequest().getMethod();
    String path = exchange.getRequest().getPath().value();
    return MUTATIONS.contains(method)
      && exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)
      && !path.startsWith("/api/v1/audit/")
      && !path.equals("/api/v1/auth/login")
      && !path.equals("/api/v1/auth/change-password");
  }

  private static String action(HttpMethod method) {
    if (HttpMethod.POST.equals(method)) return "CREATE";
    if (HttpMethod.DELETE.equals(method)) return "DELETE";
    return "UPDATE";
  }

  /**
   * Same rule as the rate limiter: forwarding headers count only when they come from a trusted
   * proxy and the hop appended by that proxy is used, so a client cannot write its own address
   * into the audit trail.
   */
  private static String clientIp(ServerWebExchange exchange) {
    return RateLimitFilter.getClientId(exchange.getRequest());
  }

  private record AuditRequest(String action, String method, String path, int status,
                              String clientIp, String userAgent) {}

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
