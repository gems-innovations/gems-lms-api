package com.gems.admin.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("GEMS LMS API - Admin Microservice")
        .version("1.0.0")
        .description("API documentation for the Admin microservice. This service handles administrative operations including branding management.")
        .contact(new Contact()
          .name("GEMS LMS Team")
          .email("support@gems.com"))
        .license(new License()
          .name("Apache 2.0")
          .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
      .servers(List.of(
        new Server()
          .url("http://localhost:8082")
          .description("Local development server"),
        new Server()
          .url("https://api.admin.gemsinnovations.com")
          .description("Production server")))
      .components(new io.swagger.v3.oas.models.Components()
        .addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
          .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")));
  }
}
