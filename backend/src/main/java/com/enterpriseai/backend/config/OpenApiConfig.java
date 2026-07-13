package com.enterpriseai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY_SCHEME = "bearerAuth";

    @Bean
    OpenAPI enterpriseAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise AI Backend API")
                        .description("Authentication and user profile APIs for Enterprise AI Workspace.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Enterprise AI Workspace")))
                .components(new Components()
                        .addSecuritySchemes(
                                JWT_SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(JWT_SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste a valid JWT token. Do not include the Bearer prefix.")));
    }
}
