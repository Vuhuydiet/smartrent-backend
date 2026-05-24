package com.smartrent.service.authentication.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartrent.config.Constants;
import com.smartrent.dto.request.MagicLinkRequest;
import com.smartrent.dto.request.MagicLinkVerifyRequest;
import com.smartrent.dto.response.JwtUserClaimsDto;
import com.smartrent.dto.response.MagicLinkResponse;
import com.smartrent.dto.response.MagicLinkVerifyResponse;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.authentication.MagicLinkService;
import com.smartrent.service.authentication.TokenCacheService;
import com.smartrent.service.email.EmailService;
import com.smartrent.utility.EmailBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MagicLinkServiceImpl implements MagicLinkService {

  EmailService emailService;
  TokenCacheService tokenCacheService;

  @NonFinal
  @Value("${application.authentication.jwt.access-signer-key}")
  String ACCESS_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.magic-link-signer-key}")
  String MAGIC_LINK_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.valid-duration}")
  long ACCESS_DURATION;

  @NonFinal
  @Value("${application.authentication.jwt.magic-link-duration:600}")
  long MAGIC_LINK_DURATION;

  @NonFinal
  @Value("${application.client-url}")
  String CLIENT_URL;

  @NonFinal
  @Value("${application.authentication.magic-link.callback-path:/auth/magic-link}")
  String MAGIC_LINK_CALLBACK_PATH;

  @NonFinal
  @Value("${application.email.sender.email}")
  String senderEmail;

  @NonFinal
  @Value("${application.email.sender.name}")
  String senderName;

  @NonFinal
  @Value("${application.authentication.magic-link.email-subject:SmartRent - Đăng nhập với liên kết}")
  String magicLinkSubject;

  @Override
  public MagicLinkResponse requestLink(MagicLinkRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    String guestId = "guest:" + UUID.randomUUID();
    String jti = UUID.randomUUID().toString();

    String token = generateMagicLinkToken(guestId, email, jti);
    String url = buildMagicLinkUrl(token);

    sendMagicLinkEmail(email, url);

    log.info("Magic link dispatched for email={} jti={}", email, jti);
    return MagicLinkResponse.builder()
        .email(email)
        .expiresInSeconds(MAGIC_LINK_DURATION)
        .build();
  }

  @Override
  public MagicLinkVerifyResponse verifyLink(MagicLinkVerifyRequest request) {
    SignedJWT signedJWT = parseAndVerifyMagicLink(request.getToken());

    String jti;
    String guestId;
    String email;
    LocalDateTime tokenExpiry;
    try {
      jti = signedJWT.getJWTClaimsSet().getJWTID();
      guestId = signedJWT.getJWTClaimsSet().getSubject();
      email = signedJWT.getJWTClaimsSet().getStringClaim("email");
      Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
      tokenExpiry = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
    } catch (ParseException e) {
      throw new DomainException(DomainCode.MAGIC_LINK_INVALID);
    }

    if (tokenCacheService.isTokenInvalidated(jti)) {
      throw new DomainException(DomainCode.MAGIC_LINK_ALREADY_USED);
    }

    // Burn the link immediately so it cannot be replayed.
    tokenCacheService.invalidateToken(jti, tokenExpiry);

    String accessToken = generateGuestAccessToken(guestId, email);

    return MagicLinkVerifyResponse.builder()
        .accessToken(accessToken)
        .expiresInSeconds(ACCESS_DURATION)
        .email(email)
        .guest(true)
        .build();
  }

  private SignedJWT parseAndVerifyMagicLink(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWSVerifier verifier = new MACVerifier(MAGIC_LINK_SIGNER_KEY);

      if (!signedJWT.verify(verifier)) {
        throw new DomainException(DomainCode.MAGIC_LINK_INVALID);
      }
      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime == null || expirationTime.before(new Date())) {
        throw new DomainException(DomainCode.MAGIC_LINK_INVALID);
      }
      return signedJWT;
    } catch (ParseException | JOSEException e) {
      log.warn("Magic link token rejected: {}", e.getMessage());
      throw new DomainException(DomainCode.MAGIC_LINK_INVALID);
    }
  }

  private String generateMagicLinkToken(String guestId, String email, String jti) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(guestId)
        .jwtID(jti)
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(MAGIC_LINK_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("email", email)
        .claim("type", "MAGIC_LINK")
        .build();
    return sign(header, claims, MAGIC_LINK_SIGNER_KEY);
  }

  private String generateGuestAccessToken(String guestId, String email) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JwtUserClaimsDto userClaims = JwtUserClaimsDto.builder()
        .userId(guestId)
        .email(email)
        .isVerified(false)
        .isBroker(false)
        .build();
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(guestId)
        .jwtID(UUID.randomUUID().toString())
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(ACCESS_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("scope", Constants.ROLE_USER)
        .claim("guest", true)
        .claim("email", email)
        .claim("user", userClaims)
        .build();
    return sign(header, claims, ACCESS_SIGNER_KEY);
  }

  private String sign(JWSHeader header, JWTClaimsSet claims, String signerKey) {
    JWSObject jwsObject = new JWSObject(header, claims.toPayload());
    try {
      jwsObject.sign(new MACSigner(signerKey));
      return jwsObject.serialize();
    } catch (JOSEException e) {
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private String buildMagicLinkUrl(String token) {
    String base = CLIENT_URL.endsWith("/") ? CLIENT_URL.substring(0, CLIENT_URL.length() - 1) : CLIENT_URL;
    String path = MAGIC_LINK_CALLBACK_PATH.startsWith("/") ? MAGIC_LINK_CALLBACK_PATH : "/" + MAGIC_LINK_CALLBACK_PATH;
    String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
    return base + path + "?token=" + encoded;
  }

  private void sendMagicLinkEmail(String email, String url) {
    long expiryMinutes = Math.max(1, MAGIC_LINK_DURATION / 60);
    String html = EmailBuilder.buildMagicLinkHtmlContent(senderName, email, url, (int) expiryMinutes);

    EmailRequest emailRequest = EmailRequest.builder()
        .sender(EmailInfo.builder().email(senderEmail).name(senderName).build())
        .to(List.of(EmailInfo.builder().email(email).name(email).build()))
        .subject(magicLinkSubject)
        .htmlContent(html)
        .build();
    emailService.sendEmail(emailRequest);
  }
}
