package com.smartrent.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.infra.exception.model.DomainCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    DomainCode domainCode = DomainCode.UNAUTHENTICATED;

    response.setStatus(domainCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
        .code(domainCode.getValue())
        .message(domainCode.getMessage())
        .build();

    ObjectMapper objectMapper = new ObjectMapper();

    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    response.flushBuffer();
  }
}
