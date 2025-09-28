package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.AuthenticationRequest;
import com.smartrent.dto.request.IntrospectRequest;
import com.smartrent.dto.request.LogoutRequest;
import com.smartrent.dto.request.RefreshTokenRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.dto.response.IntrospectResponse;
import com.smartrent.service.authentication.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/admin")
@Tag(name = "Admin Authentication", description = "APIs for administrator authentication and token management")
public class AdminAuthenticationController {

  AuthenticationService authenticationService;

  @Autowired
  public AdminAuthenticationController(@Qualifier(Constants.ADMIN_AUTHENTICATION_SERVICE) AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/introspect")
  @Operation(
      summary = "Validate admin access token",
      description = "Validates the provided admin access token and returns whether it is valid and active.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Admin token introspection request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IntrospectRequest.class),
              examples = @ExampleObject(
                  name = "Admin Token Introspection Example",
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
          description = "Admin token validation completed",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = {
                  @ExampleObject(
                      name = "Valid Admin Token",
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
                      name = "Invalid Admin Token",
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
      summary = "Authenticate administrator",
      description = "Authenticates an administrator with email and password, returning access and refresh tokens upon successful authentication.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Administrator authentication credentials",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AuthenticationRequest.class),
              examples = @ExampleObject(
                  name = "Admin Authentication Example",
                  value = """
                      {
                        "email": "admin@smartrent.com",
                        "password": "AdminPass123!"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admin authentication successful",
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
          description = "Admin authentication failed - Invalid credentials",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Authentication Failed",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_ADMIN_CREDENTIALS",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "403",
          description = "Admin account disabled or insufficient permissions",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Account Disabled",
                  value = """
                      {
                        "code": "403001",
                        "message": "ADMIN_ACCOUNT_DISABLED",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.authenticate(request))
        .build();
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh admin access token",
      description = "Generates a new admin access token using a valid refresh token. The old access token will be invalidated.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Admin refresh token request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RefreshTokenRequest.class),
              examples = @ExampleObject(
                  name = "Admin Refresh Token Example",
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
          description = "Admin token refreshed successfully",
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
          description = "Invalid or expired admin refresh token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Token",
                  value = """
                      {
                        "code": "401001",
                        "message": "INVALID_ADMIN_REFRESH_TOKEN",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(authenticationService.refresh(request))
        .build();
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Logout administrator",
      description = "Logs out the administrator by invalidating the provided access token. The token will no longer be valid for authentication.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Admin logout request with token to invalidate",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LogoutRequest.class),
              examples = @ExampleObject(
                  name = "Admin Logout Example",
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
          description = "Admin logout successful",
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

}

