package com.smartrent.service.authentication.impl;

import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.infra.connector.GoogleAuthConnector;
import com.smartrent.infra.connector.GoogleConnector;
import com.smartrent.infra.connector.model.GoogleExchangeTokenRequest;
import com.smartrent.infra.connector.model.GoogleExchangeTokenResponse;
import com.smartrent.infra.connector.model.GoogleUserDetailResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.OutboundAuthenticationService;
import com.smartrent.service.authentication.domain.TokenType;
import com.smartrent.service.user.UserService;
import com.smartrent.utility.TokenGenerator;
import feign.FeignException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service("google-authentication-service")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class GoogleAuthenticationServiceImpl implements OutboundAuthenticationService {

  final static String DEFAULT_FIRST_NAME = "Anonymous";
  final static String DEFAULT_LAST_NAME = "User";

  @NonFinal
  @Value("${feign.client.config.google.auth.client_id}")
  String clientId;

  @NonFinal
  @Value("${feign.client.config.google.auth.client_secret}")
  String clientSecret;

  @NonFinal
  @Value("${feign.client.config.google.auth.redirect_uri}")
  String redirectUri;

  @NonFinal
  @Value("${feign.client.config.google.auth.grant_type}")
  String grantType;

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

  GoogleAuthConnector googleAuthConnector;

  GoogleConnector googleConnector;

  UserRepository userRepository;

  UserService userService;

  UserMapper userMapper;

  @Override
  public AuthenticationResponse authenticate(String authenticationCode) {
    try {
      log.debug("Attempting to exchange Google authorization code for access token");

      GoogleExchangeTokenResponse exchangeTokenResponse =
          googleAuthConnector.exchangeToken(GoogleExchangeTokenRequest.builder()
              .clientId(clientId)
              .clientSecret(clientSecret)
              .redirectUri(redirectUri)
              .grantType(grantType)
              .code(authenticationCode)
              .build());

      log.debug("Successfully exchanged authorization code, fetching user details from Google");

      GoogleUserDetailResponse userInfo = googleConnector.getUserDetail("json", exchangeTokenResponse.getAccessToken());

      log.debug("Retrieved user info from Google: {}", userInfo.getEmail());

      User user = userRepository
          .findByEmail(userInfo.getEmail())
          .orElseGet(() -> {
            log.info("Creating new user account for Google OAuth user: {}", userInfo.getEmail());
            return userService.internalCreateUser(InternalUserCreationRequest.builder()
                .email(userInfo.getEmail())
                .firstName(StringUtils.isEmpty(userInfo.getFamilyName()) ? DEFAULT_FIRST_NAME : userInfo.getFamilyName())
                .lastName(StringUtils.isEmpty(userInfo.getGivenName()) ? DEFAULT_LAST_NAME : userInfo.getGivenName())
                .avatarUrl(userInfo.getPicture())
                .build());
          });

      // Update avatar URL if user exists but avatar has changed (e.g., user updated their Google profile picture)
      if (user.getAvatarUrl() == null && StringUtils.isNotEmpty(userInfo.getPicture())) {
        user.setAvatarUrl(userInfo.getPicture());
        user = userRepository.save(user);
        log.info("Updated avatar URL for existing Google OAuth user: {}", user.getEmail());
      }

      GetUserResponse userResponse = userMapper.mapFromUserEntityToGetUserResponse(user);

      log.info("Google OAuth authentication successful for user: {}", user.getEmail());
      return buildAuthenticationResponse(user, userResponse);

    } catch (FeignException.BadRequest e) {
      log.error("Invalid Google authorization code: {}", e.getMessage());
      // Check if it's an invalid_grant error (expired or already used code)
      if (e.contentUTF8().contains("invalid_grant")) {
        throw new DomainException(DomainCode.INVALID_OAUTH_CODE);
      }
      throw new DomainException(DomainCode.OAUTH_AUTHENTICATION_FAILED);

    } catch (FeignException.Unauthorized e) {
      log.error("Google OAuth authentication failed - unauthorized: {}", e.getMessage());
      throw new DomainException(DomainCode.OAUTH_AUTHENTICATION_FAILED);

    } catch (FeignException e) {
      log.error("Google OAuth communication error: Status={}, Message={}", e.status(), e.getMessage());
      throw new DomainException(DomainCode.OAUTH_AUTHENTICATION_FAILED);

    } catch (Exception e) {
      log.error("Unexpected error during Google OAuth authentication", e);
      throw new DomainException(DomainCode.UNKNOWN_ERROR);
    }
  }

  protected AuthenticationResponse buildAuthenticationResponse(User user, GetUserResponse getUserResponse) {
    String acId = UUID.randomUUID().toString();
    String rfId = UUID.randomUUID().toString();

    String accessToken = TokenGenerator.generateToken(user, getUserResponse, VALID_DURATION, acId, rfId, SIGNER_KEY, TokenType.ACCESS);
    String refreshToken = TokenGenerator.generateToken(user, getUserResponse, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY, TokenType.REFRESH);

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .build();
  }
}
