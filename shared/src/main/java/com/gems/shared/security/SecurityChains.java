package com.gems.shared.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.Arrays;

/**
 * The security filter chain shared by the gateway and the three microservices: stateless bearer-JWT
 * authentication, CORS answered before authentication (a preflight never carries a token) and a
 * per-service list of public paths. Each service only declares its own public paths.
 */
public final class SecurityChains {

  private SecurityChains() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static SecurityWebFilterChain jwtChain(ServerHttpSecurity http, ReactiveAuthenticationManager authenticationManager,
                                                CorsConfigurationSource cors, String... publicPaths) {
    AuthenticationWebFilter jwtWebFilter = new AuthenticationWebFilter(authenticationManager);
    jwtWebFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());
    jwtWebFilter.setRequiresAuthenticationMatcher(new NegatedServerWebExchangeMatcher(
      new OrServerWebExchangeMatcher(Arrays.stream(publicPaths)
        .map(PathPatternParserServerWebExchangeMatcher::new)
        .map(ServerWebExchangeMatcher.class::cast)
        .toList())));

    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      // Registered natively so Spring Security runs it at SecurityWebFiltersOrder.CORS, before
      // AUTHENTICATION: a preflight OPTIONS request gets its CORS answer before the JWT filter.
      .cors(c -> c.configurationSource(cors))
      .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
      .authenticationManager(authenticationManager)
      .addFilterAt(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
      .authorizeExchange(exchanges -> exchanges
        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .pathMatchers(publicPaths).permitAll()
        .anyExchange().authenticated())
      .build();
  }
}
