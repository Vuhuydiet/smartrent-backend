package com.smartrent.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.infra.exception.model.DomainCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class AfterBearerTokenExceptionHandler extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      DomainCode domainCode = DomainCode.UNAUTHENTICATED;
      response.setStatus(domainCode.getStatus().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      ApiResponse<?> apiResponse = ApiResponse.builder()
          .code(domainCode.getValue())
          .message(domainCode.getMessage())
          .build();

      ObjectMapper objectMapper = new ObjectMapper();

      response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

      response.getWriter().flush();
    }
  }
}
