package com.smartrent.controller;

import com.smartrent.dto.request.UpdateContactPhoneRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/list")
  @Operation(
      summary = "Get paginated list of users",
      description = "Retrieves a paginated list of all users in the system. This endpoint requires authentication and is typically used for administrative purposes.",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Users retrieved successfully",
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
                          "page": 0,
                          "size": 10,
                          "totalElements": 25,
                          "totalPages": 3,
                          "data": [
                            {
                              "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                              "phoneCode": "+1",
                              "phoneNumber": "1234567890",
                              "email": "john.doe@example.com",
                              "firstName": "John",
                              "lastName": "Doe",
                              "idDocument": "ID123456789",
                              "taxNumber": "TAX987654321"
                            },
                            {
                              "userId": "user-223e4567-e89b-12d3-a456-426614174001",
                              "phoneCode": "+1",
                              "phoneNumber": "0987654321",
                              "email": "jane.smith@example.com",
                              "firstName": "Jane",
                              "lastName": "Smith",
                              "idDocument": "ID987654321",
                              "taxNumber": "TAX123456789"
                            }
                          ]
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
          responseCode = "403",
          description = "Forbidden - Insufficient permissions",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Forbidden Error",
                  value = """
                      {
                        "code": "403001",
                        "message": "FORBIDDEN",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<PageResponse<GetUserResponse>> getUsers(
      @io.swagger.v3.oas.annotations.Parameter(
          description = "Page number (0-indexed)",
          example = "0",
          required = true
      )
      @RequestParam("page") int page,
      @io.swagger.v3.oas.annotations.Parameter(
          description = "Number of items per page",
          example = "10",
          required = true
      )
      @RequestParam("size") int size) {
    PageResponse<GetUserResponse> pageResponse = userService.getUsers(page, size);
    return ApiResponse.<PageResponse<GetUserResponse>>builder()
        .data(pageResponse)
        .build();
  }

  @PatchMapping("/contact-phone")
  @Operation(
      summary = "Update contact phone number",
      description = """
          Updates the authenticated user's contact phone number.

          **Use Case:**
          - User clicks on phone number in listing detail but hasn't provided contact phone
          - Frontend prompts user to input their contact phone
          - This endpoint updates the user's contact phone

          **Behavior:**
          - Requires authentication (user must be logged in)
          - Validates Vietnam phone number format
          - Resets phone verification status to false
          - Clears user cache

          **Returns:**
          - Updated user profile with new contact phone
          """,
      security = @SecurityRequirement(name = "Bearer Authentication"),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Contact phone update request",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = UpdateContactPhoneRequest.class),
              examples = @ExampleObject(
                  name = "Update Contact Phone",
                  value = """
                      {
                        "contactPhoneNumber": "0912345678"
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Contact phone updated successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class)
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid phone number format"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Unauthorized - User not authenticated"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "User not found"
      )
  })
  public ApiResponse<GetUserResponse> updateContactPhone(
      @Valid @RequestBody UpdateContactPhoneRequest request) {
    // Extract user ID from JWT token
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();

    GetUserResponse response = userService.updateContactPhone(userId, request);
    return ApiResponse.<GetUserResponse>builder()
        .data(response)
        .build();
  }
}
