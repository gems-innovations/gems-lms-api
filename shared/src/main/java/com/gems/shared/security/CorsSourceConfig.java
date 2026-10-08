package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS policy from the {@code cors.*} properties, used by {@link SecurityChains}. It is applied by
 * Spring Security (not by a separate CorsWebFilter), so there is a single CORS pass per request and
 * the response never carries duplicated Access-Control-* headers.
 */
@Configuration
public class CorsSourceConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins}") String allowedOrigins,
      @Value("${cors.allowed-methods}") String allowedMethods,
      @Value("${cors.allowed-headers}") String allowedHeaders,
      @Value("${cors.allow-credentials}") boolean allowCredentials,
      @Value("${cors.max-age}") long maxAge) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(split(allowedOrigins));
    configuration.setAllowedMethods(split(allowedMethods));
    configuration.setAllowedHeaders(split(allowedHeaders));
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAge);
    configuration.setExposedHeaders(List.of("X-Total-Count", "Content-Disposition", "X-Session-Token"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private static List<String> split(String csv) {
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }
}
