package com.smartrent.utility;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.smartrent.config.Constants;
import com.smartrent.controller.dto.response.GetUserResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.authentication.domain.TokenType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TokenGenerator {

  public static String generateToken(User user, GetUserResponse userResponse, long duration, String id, String otherId, String signerKey, TokenType tokenType) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claimsSet = null;

    if (TokenType.ACCESS.equals(tokenType)) {
      claimsSet = buildAccessTokenClaims(user, duration, id, otherId, userResponse);
    } else if (TokenType.REFRESH.equals(tokenType)) {
      claimsSet = buildRefreshTokenClaims(user, duration, id, otherId);
    }

    Payload payload = claimsSet.toPayload();

    JWSObject jwsObject = new JWSObject(header, payload);

    try {
      jwsObject.sign(new MACSigner(signerKey));

      return jwsObject.serialize();
    } catch (JOSEException e) {
      throw  new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private static JWTClaimsSet buildAccessTokenClaims(User user, long duration, String id,
      String otherId, GetUserResponse userResponse) {
    try {
      return new JWTClaimsSet.Builder()
          .subject(user.getUserId())
          .jwtID(id)
          .issuer("Smart-Rent-Team")
          .issueTime(new Date())
          .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
          .claim("rfId", otherId)
          .claim("scope", Constants.ROLE_USER)
          .claim("user", userResponse)
          .build();
    } catch (Exception e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private static JWTClaimsSet buildRefreshTokenClaims(User user, long duration, String id,
      String otherId) {
    return new JWTClaimsSet.Builder()
        .subject(user.getUserId())
        .jwtID(id)
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("acId", otherId)
        .claim("scope", Constants.ROLE_USER)
        .build();
  }
}
