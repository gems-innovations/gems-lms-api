package com.gems.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

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
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();

    List<String> origins = Arrays.asList(allowedOrigins.split(","));
    corsConfig.setAllowedOrigins(origins);

    List<String> methods = Arrays.asList(allowedMethods.split(","));
    corsConfig.setAllowedMethods(methods);

    List<String> headers = Arrays.asList(allowedHeaders.split(","));
    corsConfig.setAllowedHeaders(headers);

    corsConfig.setAllowCredentials(allowCredentials);

    corsConfig.setMaxAge(maxAge);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }
}
