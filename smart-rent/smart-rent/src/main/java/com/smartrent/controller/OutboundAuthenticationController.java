package com.smartrent.controller;

import com.smartrent.dto.request.OutboundAuthenticationRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.AuthenticationResponse;
import com.smartrent.service.authentication.OutboundAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/outbound")
@RequiredArgsConstructor
@Tag(name = "Outbound Authentication", description = "APIs for OAuth authentication (Google login)")
public class OutboundAuthenticationController {

  @Qualifier("google-authentication-service")
  private final OutboundAuthenticationService googleAuthenticationService;

  @PostMapping("/google")
  @Operation(
      summary = "Authenticate with Google",
      description = "Authenticates a user using Google OAuth authorization code. Returns access and refresh tokens upon successful authentication. If the user doesn't exist, a new account will be created automatically.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Google OAuth authorization code",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = OutboundAuthenticationRequest.class),
              examples = @ExampleObject(
                  name = "Google Auth Example",
                  value = """
                      {
                        "code": "4/0AY0e-g7xxxxxxxxxxxxxxxxxxxxxxxxxxx"
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
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Invalid or expired authorization code",
          content = @Content(
              mediaType = "application/json",
              examples = {
                  @ExampleObject(
                      name = "Invalid OAuth Code",
                      value = """
                          {
                            "code": "5005",
                            "message": "Invalid or expired OAuth authorization code",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "OAuth Authentication Failed",
                      value = """
                          {
                            "code": "5006",
                            "message": "OAuth authentication failed",
                            "data": null
                          }
                          """
                  )
              }
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Internal server error or OAuth provider error",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "UNKNOWN_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<AuthenticationResponse> authenticateWithGoogle(
      @RequestBody @Valid OutboundAuthenticationRequest request) {
    return ApiResponse.<AuthenticationResponse>builder()
        .data(googleAuthenticationService.authenticate(request.getCode()))
        .build();
  }
}

