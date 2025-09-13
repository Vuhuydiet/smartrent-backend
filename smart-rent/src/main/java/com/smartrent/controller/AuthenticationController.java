package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.AuthenticationRequest;
import com.smartrent.controller.dto.request.ChangePasswordRequest;
import com.smartrent.controller.dto.request.ForgotPasswordRequest;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.request.LogoutRequest;
import com.smartrent.controller.dto.request.RefreshTokenRequest;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.AuthenticationResponse;
import com.smartrent.controller.dto.response.IntrospectResponse;
import com.smartrent.service.authentication.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthenticationController {

  AuthenticationService authenticationService;

  @Autowired
  public AuthenticationController(@Qualifier(Constants.AUTHENTICATION_SERVICE) AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/introspect")
  public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
    return ApiResponse.<IntrospectResponse>builder()
        .data(authenticationService.introspect(request))
        .build();
  }

  @PostMapping
  public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.authenticate(request))
        .build();
  }

  @PostMapping("/refresh")
  public ApiResponse<AuthenticationResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.refresh(request))
        .build();
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
    authenticationService.logout(request);
    return ApiResponse.<Void>builder().build();
  }

  @PatchMapping("/change-password")
  public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
    authenticationService.changePassword(request);
    return ApiResponse.<Void>builder().build();
  }

  @PatchMapping("/forgot-password")
  public ApiResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
    authenticationService.forgotPassword(request);
    return ApiResponse.<Void>builder().build();
  }

}

