package com.smartrent.service.authentication.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.AuthenticationRequest;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.request.LogoutRequest;
import com.smartrent.controller.dto.request.RefreshTokenRequest;
import com.smartrent.controller.dto.response.AuthenticationResponse;
import com.smartrent.controller.dto.response.IntrospectResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.InvalidatedTokenRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.InvalidatedToken;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.authentication.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service(Constants.AUTHENTICATION_SERVICE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

  UserRepository userRepository;

  protected InvalidatedTokenRepository invalidatedTokenRepository;

  protected PasswordEncoder passwordEncoder;

  @NonFinal
  @Value("${application.authentication.jwt.access-signer-key}")
  protected String SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.refresh-signer-key}")
  protected String REFRESH_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.valid-duration}")
  protected long VALID_DURATION;

  @NonFinal
  @Value("${application.authentication.jwt.refreshable-duration}")
  protected long REFRESHABLE_DURATION;

  @Override
  public IntrospectResponse introspect(IntrospectRequest request) {

    boolean isValid = true;

    try {
      verifyToken(request.getToken(), false);
    } catch (DomainException e) {
      isValid = false;
    }

    return IntrospectResponse.builder()
        .valid(isValid)
        .build();
  }

  @Override
  public AuthenticationResponse authenticate(AuthenticationRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new DomainException(DomainCode.INVALID_EMAIL_PASSWORD));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new DomainException(DomainCode.INVALID_EMAIL_PASSWORD);
    }

    return buildAuthenticationResponse(user);
  }

  @Override
  public void logout(LogoutRequest request) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(request.getToken());
      String acId = signedJWT.getJWTClaimsSet().getJWTID();
      String rfId = signedJWT.getJWTClaimsSet().getClaim("rfId").toString();

      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
      expirationTime = expirationTime.plusSeconds(REFRESHABLE_DURATION - VALID_DURATION);

      invalidatedTokenRepository.save(InvalidatedToken.builder()
          .accessId(acId)
          .refreshId(rfId)
          .expirationTime(expirationTime)
          .build());
    } catch (ParseException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }


  @Override
  public AuthenticationResponse refresh(RefreshTokenRequest request) {
    SignedJWT signedJWT = verifyToken(request.getRefreshToken(), true);

    try {
      String acId = signedJWT.getJWTClaimsSet().getClaim("acId").toString();
      String rfId = signedJWT.getJWTClaimsSet().getJWTID();

      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
      expirationTime = expirationTime.plusSeconds(REFRESHABLE_DURATION - VALID_DURATION);

      invalidatedTokenRepository.save(InvalidatedToken.builder()
          .accessId(acId)
          .refreshId(rfId)
          .expirationTime(expirationTime)
          .build());

      User user = userRepository.findById(signedJWT.getJWTClaimsSet().getSubject())
          .orElseThrow(() -> new DomainException(DomainCode.USER_NOT_FOUND));

      return buildAuthenticationResponse(user);
    } catch (ParseException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private AuthenticationResponse buildAuthenticationResponse(User user) {
    String acId = UUID.randomUUID().toString();
    String rfId = UUID.randomUUID().toString();

    String accessToken = generateToken(user, VALID_DURATION, acId, rfId, SIGNER_KEY);
    String refreshToken = generateToken(user, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY);

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  protected SignedJWT verifyToken(String token, boolean isRefresh) {
    JWSVerifier verifier;

    try {
      if (isRefresh) {
        verifier = new MACVerifier(REFRESH_SIGNER_KEY);
      } else {
        verifier = new MACVerifier(SIGNER_KEY);
      }

      SignedJWT signedJWT = SignedJWT.parse(token);

      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

      String id = signedJWT.getJWTClaimsSet().getJWTID();
      boolean verified = signedJWT.verify(verifier);

      if (!verified || expirationTime.before(new Date())) {
        throw new DomainException(DomainCode.INVALID_TOKEN);
      }

      if (invalidatedTokenRepository.existsByAccessIdOrRefreshId(id)) {
        throw new DomainException(DomainCode.INVALID_TOKEN);
      }

      return signedJWT;
    } catch (ParseException | JOSEException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private String generateToken(User user, long duration, String id, String otherId, String signerKey) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claimsSet = null;

    if (signerKey.equals(SIGNER_KEY)) {
      claimsSet = buildAccessTokenClaims(user, duration, id, otherId);
    } else if (signerKey.equals(REFRESH_SIGNER_KEY)) {
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

  private JWTClaimsSet buildAccessTokenClaims(User user, long duration, String id, String otherId) {
    try {
      return new JWTClaimsSet.Builder()
          .subject(user.getUserId())
          .jwtID(id)
          .issuer("Smart-Rent-Team")
          .issueTime(new Date())
          .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
          .claim("rfId", otherId)
          .claim("scope",Constants.ROLE_USER)
          .build();
    } catch (Exception e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private JWTClaimsSet buildRefreshTokenClaims(User user, long duration, String id, String otherId) {
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

