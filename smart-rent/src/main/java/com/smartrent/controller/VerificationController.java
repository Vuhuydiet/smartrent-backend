package com.smartrent.controller;

import com.smartrent.dto.request.VerifyCodeRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.service.authentication.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/verification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Email Verification", description = "APIs for email verification and code management")
public class VerificationController {

  VerificationService verificationService;

  @PostMapping
  @Operation(
      summary = "Verify email with code",
      description = "Verifies a user's email address using the verification code sent to their email. Once verified, the user account will be activated.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Email verification request containing email and verification code",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = VerifyCodeRequest.class),
              examples = @ExampleObject(
                  name = "Email Verification Example",
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
          description = "Email verified successfully",
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
          description = "Invalid verification code or email format",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Validation Error",
                  value = """
                      {
                        "code": "400001",
                        "message": "INVALID_VERIFICATION_CODE",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "Verification code not found or user not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "404001",
                        "message": "VERIFICATION_CODE_NOT_FOUND",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "410",
          description = "Verification code has expired",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Expired Code Error",
                  value = """
                      {
                        "code": "410001",
                        "message": "VERIFICATION_CODE_EXPIRED",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<Void> verify(@RequestBody @Valid VerifyCodeRequest verifyCodeRequest) {
    verificationService.verifyCode(verifyCodeRequest);

    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/code")
  @Operation(
      summary = "Send verification code to email",
      description = "Sends a new verification code to the specified email address. This can be used for initial account verification or to resend an expired code.",
      parameters = @Parameter(
          name = "email",
          description = "The email address to send the verification code to",
          required = true,
          example = "john.doe@example.com",
          schema = @Schema(type = "string", format = "email")
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Verification code sent successfully",
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
          description = "Invalid email format",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Invalid Email Error",
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
          responseCode = "404",
          description = "User with the specified email not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "User Not Found Error",
                  value = """
                      {
                        "code": "404001",
                        "message": "USER_NOT_FOUND",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "429",
          description = "Too many requests - Rate limit exceeded",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Rate Limit Error",
                  value = """
                      {
                        "code": "429001",
                        "message": "RATE_LIMIT_EXCEEDED",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Failed to send email",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Email Service Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "EMAIL_SERVICE_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<Void> sendCode(@RequestParam("email") String email) {
    verificationService.sendVerifyCode(email);
    return ApiResponse.<Void>builder().build();
  }
}
