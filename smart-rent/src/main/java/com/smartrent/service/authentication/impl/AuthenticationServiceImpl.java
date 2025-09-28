package com.smartrent.service.authentication.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.AuthenticationRequest;
import com.smartrent.controller.dto.request.ChangePasswordRequest;
import com.smartrent.controller.dto.request.ForgotPasswordRequest;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.request.LogoutRequest;
import com.smartrent.controller.dto.request.RefreshTokenRequest;
import com.smartrent.controller.dto.request.ResetPasswordRequest;
import com.smartrent.controller.dto.request.VerifyCodeRequest;
import com.smartrent.controller.dto.response.AuthenticationResponse;
import com.smartrent.controller.dto.response.ForgotPasswordResponse;
import com.smartrent.controller.dto.response.IntrospectResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.IncorrectPasswordException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.exception.UserNotVerifiedException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.AuthenticationService;
import com.smartrent.service.authentication.TokenCacheService;
import com.smartrent.service.authentication.VerificationService;
import com.smartrent.service.authentication.domain.TokenType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service(Constants.AUTHENTICATION_SERVICE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {

  UserRepository userRepository;

  UserMapper userMapper;

  protected TokenCacheService tokenCacheService;

  protected PasswordEncoder passwordEncoder;

  protected VerificationService verificationService;

  @NonFinal
  @Value("${application.authentication.jwt.access-signer-key}")
  protected String SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.refresh-signer-key}")
  protected String REFRESH_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.reset-password-signer-key}")
  protected String RESET_PASSWORD_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.valid-duration}")
  protected long VALID_DURATION;

  @NonFinal
  @Value("${application.authentication.jwt.refreshable-duration}")
  protected long REFRESHABLE_DURATION;

  @NonFinal
  @Value("${application.authentication.jwt.reset_password_duration}")
  protected long RESET_PASSWORD_DURATION;

  @Override
  public IntrospectResponse introspect(IntrospectRequest request) {

    boolean isValid = true;

    try {
      verifyToken(request.getToken(), TokenType.ACCESS);
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

    if (!user.isVerified()) {
      throw new UserNotVerifiedException();
    }

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

      tokenCacheService.invalidateTokens(acId, rfId, expirationTime);
    } catch (ParseException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  @Override
  public AuthenticationResponse refresh(RefreshTokenRequest request) {
    SignedJWT signedJWT = verifyToken(request.getRefreshToken(), TokenType.REFRESH);

    try {
      String acId = signedJWT.getJWTClaimsSet().getClaim("acId").toString();
      String rfId = signedJWT.getJWTClaimsSet().getJWTID();

      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
      expirationTime = expirationTime.plusSeconds(REFRESHABLE_DURATION - VALID_DURATION);

      tokenCacheService.invalidateTokens(acId, rfId, expirationTime);

      User user = userRepository.findById(signedJWT.getJWTClaimsSet().getSubject())
          .orElseThrow(() -> new DomainException(DomainCode.USER_NOT_FOUND));

      return buildAuthenticationResponse(user);
    } catch (ParseException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  @Override
  public void changePassword(ChangePasswordRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User user = userRepository.findById(authentication.getName())
        .orElseThrow(UserNotFoundException::new);

    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
      throw new IncorrectPasswordException();
    }

    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      throw new DomainException(DomainCode.PASSWORD_SAME);
    }

    verificationService.verifyCode(VerifyCodeRequest.builder()
            .email(user.getEmail())
            .verificationCode(request.getVerificationCode())
        .build());

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  @Override
  public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
    User user = verificationService.verifyCode(request.getVerificationCode(), request.getEmail());
    return buildForgotPasswordResponse(user);
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

  @Override
  public void resetPassword(ResetPasswordRequest request) {
    try {

      SignedJWT signedJWT = verifyToken(request.getResetPasswordToken(), TokenType.RESET_PASSWORD);
      String rsId = signedJWT.getJWTClaimsSet().getJWTID();

      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());

      String userId = signedJWT.getJWTClaimsSet().getSubject();
      User user = userRepository.findById(userId)
          .orElseThrow(UserNotFoundException::new);

      if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
        throw new DomainException(DomainCode.PASSWORD_SAME);
      }

      user.setPassword(passwordEncoder.encode(request.getNewPassword()));
      userRepository.saveAndFlush(user);

      tokenCacheService.invalidateToken(rsId, expirationTime);
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private ForgotPasswordResponse buildForgotPasswordResponse(User user) {
    String rsId = UUID.randomUUID().toString();

    String resetPasswordToken = generateToken(user, RESET_PASSWORD_DURATION, rsId, null, RESET_PASSWORD_KEY);

    return ForgotPasswordResponse.builder()
        .resetPasswordToken(resetPasswordToken)
        .build();
  }

  protected SignedJWT verifyToken(String token, TokenType tokenType) {
    try {
      JWSVerifier verifier = new MACVerifier(SIGNER_KEY);

      if (TokenType.REFRESH.equals(tokenType)) {
        verifier = new MACVerifier(REFRESH_SIGNER_KEY);
      } else if (TokenType.RESET_PASSWORD.equals(tokenType)) {
        verifier = new MACVerifier(RESET_PASSWORD_KEY);
      }

      SignedJWT signedJWT = SignedJWT.parse(token);

      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

      String id = signedJWT.getJWTClaimsSet().getJWTID();
      boolean verified = signedJWT.verify(verifier);

      if (!verified) {
        log.error("JWT signature verification failed for token type: {}, token ID: {}", tokenType, id);
        throw new DomainException(DomainCode.INVALID_TOKEN);
      }

      if (expirationTime.before(new Date())) {
        log.error("JWT token expired for token type: {}, token ID: {}, expiration: {}", tokenType, id, expirationTime);
        throw new DomainException(DomainCode.INVALID_TOKEN);
      }

      if (tokenCacheService.isTokenInvalidated(id)) {
        log.error("JWT token has been invalidated for token type: {}, token ID: {}", tokenType, id);
        throw new DomainException(DomainCode.INVALID_TOKEN);
      }

      return signedJWT;
    } catch (ParseException e) {
      log.error("Failed to parse JWT token for token type: {}, error: {}", tokenType, e.getMessage());
      throw new DomainException(DomainCode.INVALID_TOKEN);
    } catch (JOSEException e) {
      log.error("JOSE exception during JWT verification for token type: {}, error: {}", tokenType, e.getMessage());
      throw new DomainException(DomainCode.INVALID_TOKEN);
    }
  }

  private String generateToken(User user, long duration, String id, String otherId, String signerKey) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claimsSet = null;

    if (signerKey.equals(SIGNER_KEY)) {
      claimsSet = buildAccessTokenClaims(user, duration, id, otherId);
    } else if (signerKey.equals(REFRESH_SIGNER_KEY)) {
      claimsSet = buildRefreshTokenClaims(user, duration, id, otherId);
    } else if (signerKey.equals(RESET_PASSWORD_KEY)) {
      claimsSet = buildResetPasswordTokenClaims(user, duration, id, otherId);
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
          .claim("user", userMapper.mapFromUserEntityToGetUserResponse(user))
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

  private JWTClaimsSet buildResetPasswordTokenClaims(User user, long duration, String id, String otherId) {
    return new JWTClaimsSet.Builder()
        .subject(user.getUserId())
        .jwtID(id)
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("scope", Constants.ROLE_USER)
        .build();
  }
}

