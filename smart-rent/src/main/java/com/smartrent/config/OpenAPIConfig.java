package com.smartrent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${open.api.title}") String appTitle,
            @Value("${open.api.version}") String appVersion,
            @Value("${open.api.description}") String appDescription,
            @Value("${open.api.server.url}") String appServerUrl,
            @Value("${open.api.server.description}") String appServerDescription
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title(appTitle)
                        .version(appVersion)
                        .description(appDescription + "\n\n" +
                                "## Authentication\n" +
                                "This API uses JWT (JSON Web Token) for authentication. To access protected endpoints:\n" +
                                "1. Authenticate using the `/v1/auth` endpoint to obtain access and refresh tokens\n" +
                                "2. Include the access token in the Authorization header: `Bearer <access_token>`\n" +
                                "3. Use the refresh token to obtain new access tokens when they expire\n\n" +
                                "## Rate Limiting\n" +
                                "API requests are rate-limited to prevent abuse. If you exceed the rate limit, you'll receive a 429 status code.\n\n" +
                                "## Error Handling\n" +
                                "All API responses follow a consistent format with a `code`, `message`, and `data` field. " +
                                "Error codes are standardized across the application.")
                        .termsOfService("https://smartrent.com/terms")
                        .contact(new Contact()
                                .name("SmartRent API Support")
                                .email("api-support@smartrent.com")
                                .url("https://smartrent.com/support"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(appServerUrl).description(appServerDescription),
                        new Server().url("https://api.smartrent.com").description("Production Server"),
                        new Server().url("https://staging-api.smartrent.com").description("Staging Server")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("SmartRent API Documentation")
                        .url("https://docs.smartrent.com"))
                .components(
                        new Components()
                                .addSecuritySchemes("Bearer Authentication",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .name("Authorization")
                                                .description("JWT Authorization header using the Bearer scheme. " +
                                                        "Enter 'Bearer' [space] and then your token in the text input below. " +
                                                        "Example: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'")
                                )
                )
                .security(List.of(new SecurityRequirement().addList("Bearer Authentication")));
    }

    @Bean
    public GroupedOpenApi publicApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("smartrent-api")
                .displayName("SmartRent API")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("Authentication APIs")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/auth/**", "/v1/verification/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("user-management")
                .displayName("User Management APIs")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("admin-management")
                .displayName("Admin Management APIs")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/admins/**", "/v1/auth/admin/**", "/v1/roles/**")
                .build();
    }
}
