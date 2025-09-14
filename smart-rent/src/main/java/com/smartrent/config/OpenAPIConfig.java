package com.smartrent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                        .description(appDescription))
                .servers(List.of(new Server().url(appServerUrl).description(appServerDescription)))
                .components(
                        new Components()
                                .addSecuritySchemes("Bearer Authentication",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .name("Authorization")
                                )
                )
                .security(List.of(new SecurityRequirement().addList("Bearer Authentication")));
    }

    @Bean
    public GroupedOpenApi groupedOpenApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("api-service")
                .packagesToScan(packageToScan)
                .build();
    }
}
