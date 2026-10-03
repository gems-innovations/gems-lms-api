package com.gems.gateway.config;

import com.gems.shared.security.JwtReactiveAuthenticationManager;
import com.gems.shared.security.JwtServerAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/login",
    "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
    "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-ui.html"
  };

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Value("${cors.allowed-methods}")
  private String allowedMethods;

  @Value("${cors.allowed-headers}")
  private String allowedHeaders;

  @Value("${cors.allow-credentials}")
  private boolean allowCredentials;

  @Value("${cors.max-age}")
  private long maxAge;

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                        JwtReactiveAuthenticationManager authenticationManager) {
    AuthenticationWebFilter jwtWebFilter = new AuthenticationWebFilter(authenticationManager);
    jwtWebFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter());
    jwtWebFilter.setRequiresAuthenticationMatcher(new NegatedServerWebExchangeMatcher(
      new OrServerWebExchangeMatcher(publicMatchers())
    ));

    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      // Registered natively so Spring Security runs it at SecurityWebFiltersOrder.CORS —
      // before AUTHENTICATION — so a preflight OPTIONS request (which never carries an
      // Authorization header) gets its CORS response before the JWT filter ever sees it.
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
      .authenticationManager(authenticationManager)
      .addFilterAt(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
      .authorizeExchange(exchanges -> exchanges
        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .pathMatchers(PUBLIC_PATHS).permitAll()
        .anyExchange().authenticated()
      )
      .build();
  }

  private List<ServerWebExchangeMatcher> publicMatchers() {
    return Arrays.stream(PUBLIC_PATHS)
      .map(PathPatternParserServerWebExchangeMatcher::new)
      .map(m -> (ServerWebExchangeMatcher) m)
      .toList();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(new ArrayList<>(Arrays.asList(allowedOrigins.split(","))));
    configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
    configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAge);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
