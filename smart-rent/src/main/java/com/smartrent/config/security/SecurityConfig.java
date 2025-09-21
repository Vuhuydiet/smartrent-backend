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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

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

    http.authorizeHttpRequests(configurer -> {
      configurer
          .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs", "/swagger-resources/**", "/webjars/**")
          .permitAll()
      .requestMatchers(HttpMethod.GET, "/v1/listings/**")
      .permitAll()
          .requestMatchers(HttpMethod.GET, "/v1/addresses/**")
          .permitAll()
          .requestMatchers(HttpMethod.POST, securityProperties.getMethods().getPost().toArray(new String[0]))
          .permitAll()
          .requestMatchers(HttpMethod.GET, securityProperties.getMethods().getGet().toArray(new String[0]))
          .permitAll()
          .requestMatchers(HttpMethod.OPTIONS, "/**")
          .permitAll()
          .anyRequest()
          .authenticated();
    });

    http.csrf(AbstractHttpConfigurer::disable);

    http.oauth2ResourceServer(configurer -> configurer.jwt(jwtConfigurer -> jwtConfigurer
            .decoder(decoder)
            .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
    );

    return http.build();
  }

  @Bean
  public CorsFilter corsFilter(@Value("${application.client-url}") String clientUrl) {
    CorsConfiguration config = new CorsConfiguration();

    config.addAllowedOrigin(clientUrl);
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.setAllowCredentials(true);

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

