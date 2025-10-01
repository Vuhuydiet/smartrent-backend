package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.AuthenticationRequest;
import com.smartrent.dto.request.ChangePasswordRequest;
import com.smartrent.dto.request.ForgotPasswordRequest;
import com.smartrent.dto.request.IntrospectRequest;
import com.smartrent.dto.request.LogoutRequest;
import com.smartrent.dto.request.RefreshTokenRequest;
import com.smartrent.dto.request.ResetPasswordRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.dto.response.ForgotPasswordResponse;
import com.smartrent.dto.response.IntrospectResponse;
import com.smartrent.service.authentication.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "User Authentication", description = "APIs for user authentication, token management, and password operations")
public class AuthenticationController {

  AuthenticationService authenticationService;

  @Autowired
  public AuthenticationController(@Qualifier(Constants.AUTHENTICATION_SERVICE) AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/introspect")
  @Operation(
      summary = "Validate access token",
      description = "Validates the provided access token and returns whether it is valid and active.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Token introspection request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IntrospectRequest.class),
              examples = @ExampleObject(
                  name = "Token Introspection Example",
                  value = """
                      {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Token validation completed",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = {
                  @ExampleObject(
                      name = "Valid Token",
                      value = """
                          {
                            "code": "999999",
                            "message": null,
                            "data": {
                              "valid": true
                            }
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "Invalid Token",
                      value = """
                          {
                            "code": "999999",
                            "message": null,
                            "data": {
                              "valid": false
                            }
                          }
                          """
                  )
              }
          )
      )
  })
  public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
    return ApiResponse.<IntrospectResponse>builder()
        .data(authenticationService.introspect(request))
        .build();
  }

  @PostMapping
  @Operation(
      summary = "Authenticate user",
      description = "Authenticates a user with email and password, returning access and refresh tokens upon successful authentication.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "User authentication credentials",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AuthenticationRequest.class),
              examples = @ExampleObject(
                  name = "Authentication Example",
                  value = """
                      {
                        "email": "john.doe@example.com",
                        "password": "SecurePass123!"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Authentication successful",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid credentials format",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "code": "400001",
                        "message": "INVALID_EMAIL",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Authentication failed - Invalid credentials",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Authentication Failed",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_CREDENTIALS",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "403",
          description = "Account not verified",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Account Not Verified",
                  value = """
                      {
                        "code": "403001",
                        "message": "ACCOUNT_NOT_VERIFIED",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.authenticate(request))
        .build();
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description = "Generates a new access token using a valid refresh token. The old access token will be invalidated.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Refresh token request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RefreshTokenRequest.class),
              examples = @ExampleObject(
                  name = "Refresh Token Example",
                  value = """
                      {
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Token refreshed successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid refresh token format",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "code": "400001",
                        "message": "EMPTY_INPUT",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Invalid or expired refresh token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Token",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_REFRESH_TOKEN",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<AuthenticationResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.refresh(request))
        .build();
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Logout user",
      description = "Logs out the user by invalidating the provided access token. The token will no longer be valid for authentication.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Logout request with token to invalidate",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LogoutRequest.class),
              examples = @ExampleObject(
                  name = "Logout Example",
                  value = """
                      {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Logout successful",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid token format",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Token",
                  value = """
                      {
                        "code": "400001",
                        "message": "INVALID_TOKEN",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
    authenticationService.logout(request);
    return ApiResponse.<Void>builder().build();
  }

  @PatchMapping("/change-password")
  @Operation(
      summary = "Change user password",
      description = "Changes the user's password. Requires the old password and a verification code sent to the user's email.",
      security = @SecurityRequirement(name = "Bearer Authentication"),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Password change request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ChangePasswordRequest.class),
              examples = @ExampleObject(
                  name = "Change Password Example",
                  value = """
                      {
                        "oldPassword": "OldPass123!",
                        "newPassword": "NewSecurePass123!",
                        "verificationCode": "123456"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Password changed successfully",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid password format or verification code",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "code": "400001",
                        "message": "INVALID_PASSWORD",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Invalid old password or verification code",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Authentication Failed",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_OLD_PASSWORD",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
    authenticationService.changePassword(request);
    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Verify OTP with email to get token to reset password",
      description = "Verifies the OTP code sent to the user's email and returns a reset password token if valid. The email and OTP combination ensures better security by preventing OTP collision attacks.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Forgot password request containing email and verification code",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ForgotPasswordRequest.class),
              examples = @ExampleObject(
                  name = "Forgot Password Example",
                  value = """
                      {
                        "email": "john.doe@example.com",
                        "verificationCode": "123456"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Password reset successfully",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "data": {
                            "resetPasswordToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkNjE3ODI4Mi0zOTQ4LTQ0MTItYjYwYi1lZDc1Mz..."
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Invalid or expired verification code",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Code",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_VERIFICATION_CODE",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<ForgotPasswordResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
    ForgotPasswordResponse forgotPasswordResponse = authenticationService.forgotPassword(request);
    return ApiResponse.<ForgotPasswordResponse>builder().data(forgotPasswordResponse).build();
  }

  @PostMapping("/reset-password")
  @Operation(
      summary = "Reset password with token",
      description = "Resets the user's password using a reset password token obtained from the forgot-password endpoint. This completes the password reset flow.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Password reset request with token and new password",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResetPasswordRequest.class),
              examples = @ExampleObject(
                  name = "Reset Password Example",
                  value = """
                      {
                        "resetPasswordToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "newPassword": "NewSecurePass123!"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Password reset successfully",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid password format or token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "code": "400001",
                        "message": "INVALID_PASSWORD",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Invalid or expired reset token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Token",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_TOKEN",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
    authenticationService.resetPassword(request);
    return ApiResponse.<Void>builder().build();
  }

}

