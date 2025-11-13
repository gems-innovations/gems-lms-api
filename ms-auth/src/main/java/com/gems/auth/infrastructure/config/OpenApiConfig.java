package com.gems.auth.infrastructure.config;

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
            .title("GEMS LMS API - Authentication Microservice")
            .version("1.0.0")
            .description("API documentation for the Authentication microservice. This service handles user registration, login, and authentication operations.")
            .contact(new Contact()
                .name("GEMS LMS Team")
                .email("support@gems.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
        .servers(List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Local development server"),
            new Server()
                .url("https://api.gems.com")
                .description("Production server")));
  }
}

