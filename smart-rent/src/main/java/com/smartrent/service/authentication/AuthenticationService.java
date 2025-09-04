package com.smartrent.service.authentication;

import com.smartrent.controller.dto.request.AuthenticationRequest;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.request.LogoutRequest;
import com.smartrent.controller.dto.request.RefreshTokenRequest;
import com.smartrent.controller.dto.response.AuthenticationResponse;
import com.smartrent.controller.dto.response.IntrospectResponse;

public interface AuthenticationService {
  IntrospectResponse introspect(IntrospectRequest request);

  AuthenticationResponse authenticate(AuthenticationRequest request);

  void logout(LogoutRequest request);

  AuthenticationResponse refresh(RefreshTokenRequest request);
}
