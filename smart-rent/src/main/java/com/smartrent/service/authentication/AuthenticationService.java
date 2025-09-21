package com.smartrent.service.authentication;

import com.smartrent.controller.dto.request.AuthenticationRequest;
import com.smartrent.controller.dto.request.ChangePasswordRequest;
import com.smartrent.controller.dto.request.ForgotPasswordRequest;
import com.smartrent.controller.dto.request.IntrospectRequest;
import com.smartrent.controller.dto.request.LogoutRequest;
import com.smartrent.controller.dto.request.RefreshTokenRequest;
import com.smartrent.controller.dto.request.ResetPasswordRequest;
import com.smartrent.controller.dto.response.AuthenticationResponse;
import com.smartrent.controller.dto.response.ForgotPasswordResponse;
import com.smartrent.controller.dto.response.IntrospectResponse;

public interface AuthenticationService {
  IntrospectResponse introspect(IntrospectRequest request);

  AuthenticationResponse authenticate(AuthenticationRequest request);

  void logout(LogoutRequest request);

  AuthenticationResponse refresh(RefreshTokenRequest request);

  void changePassword(ChangePasswordRequest request);

  ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);
}
