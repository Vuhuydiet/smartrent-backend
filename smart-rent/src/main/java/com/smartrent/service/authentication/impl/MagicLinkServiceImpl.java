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
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.MagicLinkService;
import com.smartrent.service.authentication.TokenCacheService;
import com.smartrent.service.authentication.domain.TokenType;
import com.smartrent.service.email.EmailService;
import com.smartrent.utility.EmailBuilder;
import com.smartrent.utility.TokenGenerator;
import com.smartrent.utility.Utils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
  UserRepository userRepository;
  UserMapper userMapper;

  @NonFinal
  @Value("${application.authentication.jwt.access-signer-key}")
  String ACCESS_SIGNER_KEY;

  // Read both keys raw and fall back in Java. The previous YAML form
  // ${MAGIC_LINK_SIGNER_KEY:${RESET_PASSWORD_SIGNER_KEY}} is not reliably
  // resolved across Spring versions — when the outer var is unset the inner
  // placeholder can land as a literal string ("${RESET_PASSWORD_SIGNER_KEY}"),
  // which is far below the 64-byte (512-bit) minimum HS512 needs and makes
  // MACSigner throw KeyLengthException at sign time.
  @NonFinal
  @Value("${application.authentication.jwt.refresh-signer-key}")
  String REFRESH_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.magic-link-signer-key:}")
  String MAGIC_LINK_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.reset-password-signer-key:}")
  String RESET_PASSWORD_SIGNER_KEY;

  @NonFinal
  @Value("${application.authentication.jwt.valid-duration}")
  long ACCESS_DURATION;

  @NonFinal
  @Value("${application.authentication.jwt.refreshable-duration}")
  long REFRESHABLE_DURATION;

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

    // If a User row exists for this email, the link will log them in as that
    // user (with full access + refresh). Otherwise, the link issues a guest
    // session. Either way the API response is the same — we never reveal
    // whether the account exists.
    Optional<User> existing = userRepository.findByEmail(email);
    String subject = existing
        .map(User::getUserId)
        .orElseGet(() -> "guest:" + UUID.randomUUID());
    boolean isGuest = existing.isEmpty();
    String jti = UUID.randomUUID().toString();

    String token = generateMagicLinkToken(subject, email, jti, isGuest);
    String url = buildMagicLinkUrl(token);

    String recipientName = existing
        .map(u -> Utils.buildName(u.getFirstName(), u.getLastName()))
        .orElse(email);
    sendMagicLinkEmail(email, recipientName, url);

    log.info("Magic link dispatched email={} guest={} jti={}", email, isGuest, jti);
    return MagicLinkResponse.builder()
        .email(email)
        .expiresInSeconds(MAGIC_LINK_DURATION)
        .build();
  }

  @Override
  public MagicLinkVerifyResponse verifyLink(MagicLinkVerifyRequest request) {
    SignedJWT signedJWT = parseAndVerifyMagicLink(request.getToken());

    String jti;
    String subject;
    String email;
    boolean isGuest;
    LocalDateTime tokenExpiry;
    try {
      JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
      jti = claims.getJWTID();
      subject = claims.getSubject();
      email = claims.getStringClaim("email");
      Boolean guestClaim = claims.getBooleanClaim("guest");
      isGuest = guestClaim != null && guestClaim;
      Instant expirationInstant = claims.getExpirationTime().toInstant();
      tokenExpiry = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
    } catch (ParseException e) {
      throw new DomainException(DomainCode.MAGIC_LINK_INVALID);
    }

    if (tokenCacheService.isTokenInvalidated(jti)) {
      throw new DomainException(DomainCode.MAGIC_LINK_ALREADY_USED);
    }

    // Burn the link immediately so it cannot be replayed.
    tokenCacheService.invalidateToken(jti, tokenExpiry);

    if (isGuest) {
      return issueGuestSession(subject, email);
    }
    return issueUserSession(subject, email);
  }

  private MagicLinkVerifyResponse issueUserSession(String userId, String email) {
    // The token's subject was the userId at request time; we look the user up
    // again at verify time in case the row was deleted in between.
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DomainException(DomainCode.MAGIC_LINK_INVALID));

    // Clicking the magic link proves email ownership — same guarantee OTP
    // gives — so we upgrade unverified accounts on first successful verify.
    if (!user.isVerified()) {
      user.setVerified(true);
      user = userRepository.save(user);
      log.info("Marked user verified via magic-link login userId={}", userId);
    }

    String acId = UUID.randomUUID().toString();
    String rfId = UUID.randomUUID().toString();
    JwtUserClaimsDto userClaims = userMapper.mapFromUserEntityToJwtUserClaimsDto(user);
    String accessToken = TokenGenerator.generateToken(
        user, userClaims, ACCESS_DURATION, acId, rfId, ACCESS_SIGNER_KEY, TokenType.ACCESS);
    String refreshToken = TokenGenerator.generateToken(
        user, userClaims, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY, TokenType.REFRESH);

    return MagicLinkVerifyResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresInSeconds(ACCESS_DURATION)
        .email(user.getEmail())
        .userId(user.getUserId())
        .guest(false)
        .build();
  }

  private MagicLinkVerifyResponse issueGuestSession(String guestId, String email) {
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
    String accessToken = sign(header, claims, ACCESS_SIGNER_KEY);
    return MagicLinkVerifyResponse.builder()
        .accessToken(accessToken)
        .expiresInSeconds(ACCESS_DURATION)
        .email(email)
        .guest(true)
        .build();
  }

  private String effectiveMagicLinkSignerKey() {
    if (MAGIC_LINK_SIGNER_KEY != null
        && !MAGIC_LINK_SIGNER_KEY.isBlank()
        && !MAGIC_LINK_SIGNER_KEY.startsWith("${")) {
      return MAGIC_LINK_SIGNER_KEY;
    }
    if (RESET_PASSWORD_SIGNER_KEY == null || RESET_PASSWORD_SIGNER_KEY.isBlank()) {
      log.error("Magic-link signer key is not configured: both MAGIC_LINK_SIGNER_KEY"
          + " and RESET_PASSWORD_SIGNER_KEY are unset/blank");
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
    return RESET_PASSWORD_SIGNER_KEY;
  }

  private SignedJWT parseAndVerifyMagicLink(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      JWSVerifier verifier = new MACVerifier(effectiveMagicLinkSignerKey());

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

  private String generateMagicLinkToken(String subject, String email, String jti, boolean isGuest) {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(subject)
        .jwtID(jti)
        .issuer("Smart-Rent-Team")
        .issueTime(new Date())
        .expirationTime(new Date(Instant.now().plus(MAGIC_LINK_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
        .claim("email", email)
        .claim("type", "MAGIC_LINK")
        .claim("guest", isGuest)
        .build();
    return sign(header, claims, effectiveMagicLinkSignerKey());
  }

  private String sign(JWSHeader header, JWTClaimsSet claims, String signerKey) {
    JWSObject jwsObject = new JWSObject(header, claims.toPayload());
    try {
      jwsObject.sign(new MACSigner(signerKey));
      return jwsObject.serialize();
    } catch (JOSEException e) {
      // Most common cause: signer key shorter than 64 bytes (HS512 minimum).
      // Logging the algorithm + key byte length without leaking the key itself.
      int keyBytes = signerKey == null ? 0 : signerKey.getBytes(StandardCharsets.UTF_8).length;
      log.error("Failed to sign magic-link JWT (alg={}, keyBytes={}): {}",
          header.getAlgorithm(), keyBytes, e.getMessage(), e);
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  private String buildMagicLinkUrl(String token) {
    String base = CLIENT_URL.endsWith("/") ? CLIENT_URL.substring(0, CLIENT_URL.length() - 1) : CLIENT_URL;
    String path = MAGIC_LINK_CALLBACK_PATH.startsWith("/") ? MAGIC_LINK_CALLBACK_PATH : "/" + MAGIC_LINK_CALLBACK_PATH;
    String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
    return base + path + "?token=" + encoded;
  }

  private void sendMagicLinkEmail(String email, String recipientName, String url) {
    long expiryMinutes = Math.max(1, MAGIC_LINK_DURATION / 60);
    String html = EmailBuilder.buildMagicLinkHtmlContent(senderName, email, url, (int) expiryMinutes);

    EmailRequest emailRequest = EmailRequest.builder()
        .sender(EmailInfo.builder().email(senderEmail).name(senderName).build())
        .to(List.of(EmailInfo.builder().email(email).name(recipientName).build()))
        .subject(magicLinkSubject)
        .htmlContent(html)
        .build();
    emailService.sendEmail(emailRequest);
  }
}
