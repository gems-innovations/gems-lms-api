package com.gems.api.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {
  private final MicroserviceConfig microserviceConfig;
  private final InternalAuthFilter internalAuthFilter;
  private final RateLimitFilter rateLimitFilter;
  private final SecurityHeadersFilter securityHeadersFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public RouteConfig(MicroserviceConfig microserviceConfig, 
                   InternalAuthFilter internalAuthFilter,
                   RateLimitFilter rateLimitFilter,
                   SecurityHeadersFilter securityHeadersFilter,
                   JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.microserviceConfig = microserviceConfig;
    this.internalAuthFilter = internalAuthFilter;
    this.rateLimitFilter = rateLimitFilter;
    this.securityHeadersFilter = securityHeadersFilter;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public RouteLocator createRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route(microserviceConfig.getAuth().getId(), route -> route
            .path(microserviceConfig.getAuth().getPath())
            .filters(f -> f.filter(securityHeadersFilter)
                          .filter(rateLimitFilter)
                          .filter(jwtAuthenticationFilter)
                          .filter(internalAuthFilter))
            .uri(microserviceConfig.getAuth().getUrl()))
        .route(microserviceConfig.getAdmin().getId(), route -> route
            .path(microserviceConfig.getAdmin().getPath())
            .filters(f -> f.filter(securityHeadersFilter)
                          .filter(rateLimitFilter)
                          .filter(jwtAuthenticationFilter)
                          .filter(internalAuthFilter))
            .uri(microserviceConfig.getAdmin().getUrl()))
        .route(microserviceConfig.getEducation().getId(), route -> route
            .path(microserviceConfig.getEducation().getPath())
            .filters(f -> f.filter(securityHeadersFilter)
                          .filter(rateLimitFilter)
                          .filter(jwtAuthenticationFilter)
                          .filter(internalAuthFilter))
            .uri(microserviceConfig.getEducation().getUrl()))
        .build();
  }
}
