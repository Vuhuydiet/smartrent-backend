package com.smartrent.service.authentication.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartrent.config.Constants;
import com.smartrent.dto.request.AuthenticationRequest;
import com.smartrent.dto.request.RefreshTokenRequest;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.mapper.AdminMapper;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.TokenCacheService;
import com.smartrent.service.authentication.domain.TokenType;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import com.smartrent.service.authentication.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(Constants.ADMIN_AUTHENTICATION_SERVICE)
public class AdminAuthenticationServiceImpl extends AuthenticationServiceImpl {

  AdminRepository adminRepository;

  AdminMapper adminMapper;

  @Autowired
  public AdminAuthenticationServiceImpl(
      TokenCacheService tokenCacheService,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      VerificationService verificationService,
      AdminRepository adminRepository,
      UserMapper userMapper,
      AdminMapper adminMapper) {
    super(userRepository, userMapper, tokenCacheService, passwordEncoder, verificationService);
    this.adminRepository = adminRepository;
    this.adminMapper = adminMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public AuthenticationResponse authenticate(AuthenticationRequest request) {

    Admin admin = adminRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new DomainException(DomainCode.INVALID_EMAIL_PASSWORD));

    if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
      throw new DomainException(DomainCode.INVALID_EMAIL_PASSWORD);
    }

    return buildAuthenticationResponse(admin);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthenticationResponse refresh(RefreshTokenRequest request) {
    SignedJWT signedJWT = verifyToken(request.getRefreshToken(), TokenType.REFRESH);

    try {
      String acId = signedJWT.getJWTClaimsSet().getClaim("acId").toString();
      String rfId = signedJWT.getJWTClaimsSet().getJWTID();

      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
      expirationTime = expirationTime.plusSeconds(REFRESHABLE_DURATION - VALID_DURATION);

      tokenCacheService.invalidateTokens(acId, rfId, expirationTime);

      Admin admin = adminRepository.findById(signedJWT.getJWTClaimsSet().getSubject())
          .orElseThrow(() -> new DomainException(DomainCode.USER_NOT_FOUND));

      return buildAuthenticationResponse(admin);
    } catch (ParseException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private AuthenticationResponse buildAuthenticationResponse(Admin admin) {
    String acId = UUID.randomUUID().toString();
    String rfId = UUID.randomUUID().toString();

    String accessToken = generateToken(admin, VALID_DURATION, acId, rfId, SIGNER_KEY);
    String refreshToken = generateToken(admin, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY);

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }

  private String generateToken(Admin admin, long duration, String id, String otherId, String signerKey) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claimsSet = null;

    if (signerKey.equals(SIGNER_KEY)) {
      claimsSet = buildAccessTokenClaims(admin, duration, id, otherId);
    } else if (signerKey.equals(REFRESH_SIGNER_KEY)) {
      claimsSet = buildRefreshTokenClaims(admin, duration, id, otherId);
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

  private JWTClaimsSet buildAccessTokenClaims(Admin admin, long duration, String id, String otherId) {
    try {
      return new JWTClaimsSet.Builder()
          .subject(admin.getAdminId())
          .jwtID(id)
          .issuer("Smart-Rent-Team")
          .issueTime(new Date())
          .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
          .claim("rfId", otherId)
          .claim("scope", buildScope(admin))
          .build();
    } catch (Exception e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private JWTClaimsSet buildRefreshTokenClaims(Admin admin, long duration, String id, String otherId) {
    return new JWTClaimsSet.Builder()
        .subject(admin.getAdminId())
        .jwtID(id)
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("acId", otherId)
        .claim("scope", buildScope(admin))
        .claim("user", adminMapper.mapFromAdminEntityToGetAdminResponse(admin))
        .build();
  }

  private String buildScope(Admin admin) {
    StringJoiner joiner = new StringJoiner(" ");
    admin.getRoles().forEach(role -> {
      joiner.add("ROLE_" + role.getRoleId());  // Use roleId (SA, UA, SPA) instead of roleName (Super Admin)
    });

    return joiner.toString();
  }
}
