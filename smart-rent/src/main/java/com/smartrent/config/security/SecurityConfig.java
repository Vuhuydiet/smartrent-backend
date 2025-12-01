package com.smartrent.config.security;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableMethodSecurity
public class SecurityConfig {

  SecurityProperties securityProperties;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      AfterBearerTokenExceptionHandler exceptionHandler,
      CustomJwtDecoder decoder) throws Exception {

    http.addFilterBefore(exceptionHandler, LogoutFilter.class);

    // Get patterns from configuration
    String[] getPatterns = securityProperties.getMethods().getGet() != null
        ? securityProperties.getMethods().getGet().toArray(new String[0])
        : new String[0];
    String[] postPatterns = securityProperties.getMethods().getPost() != null
        ? securityProperties.getMethods().getPost().toArray(new String[0])
        : new String[0];

    http.authorizeHttpRequests(configurer -> {
      configurer
          // Configure public POST endpoints from YAML
          .requestMatchers(HttpMethod.POST, postPatterns)
          .permitAll()
          // Configure public GET endpoints from YAML
          .requestMatchers(HttpMethod.GET, getPatterns)
          .permitAll()
          // Allow all OPTIONS requests (for CORS preflight)
          .requestMatchers(HttpMethod.OPTIONS, "/**")
          .permitAll()
          // All other requests require authentication
          .anyRequest()
          .authenticated();
    });

    http.csrf(AbstractHttpConfigurer::disable);

    // Configure stateless session management for JWT
    http.sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    );

    http.oauth2ResourceServer(configurer -> configurer
        .jwt(jwtConfigurer -> jwtConfigurer
            .decoder(decoder)
            .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
    );

    return http.build();
  }

  @Bean
  public CorsFilter corsFilter(
      @Value("${application.client-url}") String clientUrl,
      @Value("${application.cors.allowed-origins:}") String additionalOrigins) {
    CorsConfiguration config = new CorsConfiguration();

    // Add additional origins if specified (comma-separated)
//    if (additionalOrigins != null && !additionalOrigins.isBlank()) {
//      if (additionalOrigins.equals("*")) {
//        config.setAllowedOriginPatterns(List.of("*"));
//        config.setAllowCredentials(false);
//      } else {
//        List<String> allowedOrigins = new ArrayList<>();
//        allowedOrigins.add(clientUrl);
//        allowedOrigins.addAll(Arrays.asList(additionalOrigins.split(",")));
//        config.setAllowedOrigins(allowedOrigins);
//        config.setAllowCredentials(true);
//      }
//    } else {
//      config.setAllowedOrigins(List.of(clientUrl));
//      config.setAllowCredentials(true);
//    }

    config.setAllowedOrigins(List.of("*"));
    config.setAllowCredentials(false);

    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
    config.setExposedHeaders(List.of(
        "Content-Disposition",
        "X-Suggested-Filename",
        "Content-Length",
        "Content-Type"
    ));

    // Cache preflight for 2 hours to reduce OPTIONS requests
    config.setMaxAge(7200L);

    UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
    urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", config);

    return new CorsFilter(urlBasedCorsConfigurationSource);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);

    return jwtAuthenticationConverter;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

