package com.smartrent.service.authentication;

import com.smartrent.dto.request.AuthenticationRequest;
import com.smartrent.dto.request.ChangePasswordRequest;
import com.smartrent.dto.request.ForgotPasswordRequest;
import com.smartrent.dto.request.IntrospectRequest;
import com.smartrent.dto.request.LogoutRequest;
import com.smartrent.dto.request.RefreshTokenRequest;
import com.smartrent.dto.request.ResetPasswordRequest;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.dto.response.ForgotPasswordResponse;
import com.smartrent.dto.response.IntrospectResponse;

public interface AuthenticationService {
  IntrospectResponse introspect(IntrospectRequest request);

  AuthenticationResponse authenticate(AuthenticationRequest request);

  void logout(LogoutRequest request);

  AuthenticationResponse refresh(RefreshTokenRequest request);

  void changePassword(ChangePasswordRequest request);

  ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);
}
