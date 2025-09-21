package com.smartrent.config.security;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.response.IntrospectResponse;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.authentication.AuthenticationService;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {

  AuthenticationService authenticationService;

  @Autowired
  public CustomJwtDecoder(@Qualifier(Constants.AUTHENTICATION_SERVICE) AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @NonFinal
  @Value("${application.authentication.jwt.access-signer-key}")
  String ACCESS_SIGNER_KEY;

  @NonFinal
  NimbusJwtDecoder nimbusJwtDecoder = null;

  @Override
  public Jwt decode(String token) throws JwtException {
    IntrospectResponse introspectResponse = authenticationService.introspect(
        IntrospectRequest.builder()
            .token(token)
            .build());

    if (!introspectResponse.isValid()) {
      throw new JwtException(DomainCode.INVALID_TOKEN.getMessage());
    }

    if (Objects.isNull(nimbusJwtDecoder)) {
      SecretKeySpec secretKeySpec = new SecretKeySpec(ACCESS_SIGNER_KEY.getBytes(), "HmacSHA512");

      nimbusJwtDecoder = NimbusJwtDecoder
          .withSecretKey(secretKeySpec)
          .macAlgorithm(MacAlgorithm.HS512)
          .build();
    }

    return nimbusJwtDecoder.decode(token);
  }
}

