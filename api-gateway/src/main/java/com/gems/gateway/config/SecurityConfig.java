package com.gems.gateway.config;

import com.gems.shared.security.JwtReactiveAuthenticationManager;
import com.gems.shared.security.SecurityChains;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/** Public paths of this service; the chain itself (JWT, CORS, 401 entry point) is shared. */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/login",
    "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/verify-email", "/api/v1/auth/email-preferences/by-link",
    "/api/v1/auth/guest",
    "/actuator/health/**", "/actuator/prometheus",
    "/api/v1/files/public/**", "/api/v1/certificates/verify/**",
    "/api/v1/public/**",
    "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-ui.html"
  };

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                        JwtReactiveAuthenticationManager authenticationManager,
                                                        CorsConfigurationSource cors) {
    return SecurityChains.jwtChain(http, authenticationManager, cors, PUBLIC_PATHS);
  }
}
