package com.smartrent.service.authentication;

import com.smartrent.controller.dto.response.AuthenticationResponse;

public interface OutboundAuthenticationService {
  AuthenticationResponse authenticate(String authenticationCode);
}
