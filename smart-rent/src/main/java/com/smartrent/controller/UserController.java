package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
public class UserController {

  UserService userService;

  @PostMapping
  @Operation(
      summary = "Create a new user account",
      description = "Creates a new user account with the provided information. The user will need to verify their email before the account is fully activated.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "User creation details",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = UserCreationRequest.class),
              examples = @ExampleObject(
                  name = "User Creation Example",
                  value = """
                      {
                        "phoneCode": "+1",
                        "phoneNumber": "1234567890",
                        "email": "john.doe@example.com",
                        "password": "SecurePass123!",
                        "firstName": "John",
                        "lastName": "Doe",
                        "idDocument": "ID123456789",
                        "taxNumber": "TAX987654321"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "User created successfully",
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
                          "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                          "phoneCode": "+1",
                          "phoneNumber": "1234567890",
                          "email": "john.doe@example.com",
                          "firstName": "John",
                          "lastName": "Doe",
                          "idDocument": "ID123456789",
                          "taxNumber": "TAX987654321"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid input data",
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
          responseCode = "409",
          description = "User already exists",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Conflict Error",
                  value = """
                      {
                        "code": "409001",
                        "message": "USER_ALREADY_EXISTS",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<UserCreationResponse> createUser(@RequestBody @Valid UserCreationRequest userCreationRequest) {
    UserCreationResponse userCreationResponse = userService.createUser(userCreationRequest);
    return ApiResponse.<UserCreationResponse>builder()
        .data(userCreationResponse)
        .build();
  }

  @GetMapping
  @Operation(
      summary = "Get user profile",
      description = "Retrieves the profile information of the authenticated user. The user ID is automatically extracted from the JWT token.",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "User profile retrieved successfully",
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
                          "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                          "phoneCode": "+1",
                          "phoneNumber": "1234567890",
                          "email": "john.doe@example.com",
                          "firstName": "John",
                          "lastName": "Doe",
                          "idDocument": "ID123456789",
                          "taxNumber": "TAX987654321"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Unauthorized - Invalid or missing authentication token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Unauthorized Error",
                  value = """
                      {
                        "code": "401001",
                        "message": "UNAUTHORIZED",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "User not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "404001",
                        "message": "USER_NOT_FOUND",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetUserResponse> getUserById() {
    // Extract user ID from JWT token in SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();

    GetUserResponse getUserResponse = userService.getUserById(userId);
    return ApiResponse.<GetUserResponse>builder()
        .data(getUserResponse)
        .build();
  }
}
