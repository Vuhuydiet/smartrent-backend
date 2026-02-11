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

    log.debug("Loaded GET patterns: {}", java.util.Arrays.toString(getPatterns));
    log.debug("Loaded POST patterns: {}", java.util.Arrays.toString(postPatterns));

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
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    // Configure OAuth2 resource server with custom bearer token resolver
    // that skips JWT processing for public endpoints
    http.oauth2ResourceServer(configurer -> configurer
        .bearerTokenResolver(request -> {
          // Skip JWT processing for public endpoints - return null to skip authentication
          String path = request.getRequestURI();
          String method = request.getMethod();

          // Check if this is a public GET endpoint
          if ("GET".equalsIgnoreCase(method)) {
            // Exclude endpoints that always require authentication
            if (path.contains("/my-") || path.contains("/draft/") ||
                path.contains("/admin") || path.contains("/quota-check")) {
              log.debug("Requiring JWT for protected endpoint: {}", path);
              // Continue to extract bearer token below
            } else {
              for (String pattern : getPatterns) {
                if (pathMatches(path, pattern)) {
                  log.debug("Skipping JWT for public GET endpoint: {}", path);
                  return null;
                }
              }
            }
          }

          // Check if this is a public POST endpoint
          if ("POST".equalsIgnoreCase(method)) {
            for (String pattern : postPatterns) {
              if (pathMatches(path, pattern)) {
                log.debug("Skipping JWT for public POST endpoint: {}", path);
                return null;
              }
            }
          }

          // For other requests, extract the bearer token normally
          String authorization = request.getHeader("Authorization");
          if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
          }
          return null;
        })
        .jwt(jwtConfigurer -> jwtConfigurer
            .decoder(decoder)
            .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

    return http.build();
  }

  /**
   * Simple path matching that supports ** and * wildcards
   */
  private boolean pathMatches(String path, String pattern) {
    // Convert pattern to regex
    String regex = pattern
        .replace("**", "@@DOUBLE_STAR@@")
        .replace("*", "[^/]*")
        .replace("@@DOUBLE_STAR@@", ".*");
    return path.matches(regex);
  }

  @Bean
  public CorsFilter corsFilter(
      @Value("${application.client-url}") String clientUrl,
      @Value("${application.admin-url}") String adminUrl,
      @Value("${application.cors.allowed-origins:}") String additionalOrigins) {
    CorsConfiguration config = new CorsConfiguration();

    // Allow credentials for OAuth flows (Google login, etc.)
    config.setAllowCredentials(true);

    // Set specific allowed origins (required when allowCredentials is true)
    config.setAllowedOrigins(List.of(
        clientUrl,
        adminUrl,
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:8080"));

    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
    config.setExposedHeaders(List.of(
        "Content-Disposition",
        "X-Suggested-Filename",
        "Content-Length",
        "Content-Type",
        "Access-Control-Allow-Credentials"));

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
