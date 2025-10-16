package com.gems.auth.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GEMS Auth API")
                        .description("Authentication microservice for GEMS LMS")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GEMS Team")
                                .email("gems@example.com")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .packagesToScan("com.gems.auth.infrastructure.driving.rest")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }
}
