package com.smartrent.service.authentication.impl;

import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.infra.connector.GoogleAuthConnector;
import com.smartrent.infra.connector.GoogleConnector;
import com.smartrent.infra.connector.model.GoogleExchangeTokenRequest;
import com.smartrent.infra.connector.model.GoogleExchangeTokenResponse;
import com.smartrent.infra.connector.model.GoogleUserDetailResponse;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.OutboundAuthenticationService;
import com.smartrent.service.authentication.domain.TokenType;
import com.smartrent.service.user.UserService;
import com.smartrent.utility.TokenGenerator;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("google-authentication-service")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class GoogleAuthenticationServiceImpl implements OutboundAuthenticationService {

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
    GoogleExchangeTokenResponse exchangeTokenResponse =
        googleAuthConnector.exchangeToken(GoogleExchangeTokenRequest.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .grantType(grantType)
            .code(authenticationCode)
            .build());

    GoogleUserDetailResponse userInfo = googleConnector.getUserDetail("json", exchangeTokenResponse.getAccessToken());

    User user = userRepository
        .findByEmail(userInfo.getEmail())
        .orElseGet(() -> userService.internalCreateUser(UserCreationRequest.builder()
            .email(userInfo.getEmail())
            .firstName(userInfo.getFamilyName())
            .lastName(userInfo.getGivenName())
            .build()));

    GetUserResponse userResponse = userMapper.mapFromUserEntityToGetUserResponse(user);

    return buildAuthenticationResponse(user, userResponse);
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
