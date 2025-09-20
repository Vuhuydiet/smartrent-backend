package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.AdminCreationRequest;
import com.smartrent.controller.dto.response.AdminCreationResponse;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.GetAdminResponse;
import com.smartrent.service.admin.AdminService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Management", description = "APIs for managing administrator accounts and profiles")
public class AdminController {

  AdminService adminService;

  @PostMapping
  @Operation(
      summary = "Create a new admin account",
      description = "Creates a new administrator account with the provided information and assigned roles. Only existing admins with appropriate permissions can create new admin accounts.",
      security = @SecurityRequirement(name = "Bearer Authentication"),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Admin creation details including roles",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AdminCreationRequest.class),
              examples = @ExampleObject(
                  name = "Admin Creation Example",
                  value = """
                      {
                        "phoneCode": "+1",
                        "phoneNumber": "9876543210",
                        "email": "admin@smartrent.com",
                        "password": "AdminPass123!",
                        "firstName": "Jane",
                        "lastName": "Smith",
                        "roles": ["ADMIN", "SUPER_ADMIN"]
                      }
                      """
              )
          )
      )
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admin created successfully",
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
                          "adminId": "admin-123e4567-e89b-12d3-a456-426614174000",
                          "phoneCode": "+1",
                          "phoneNumber": "9876543210",
                          "email": "admin@smartrent.com",
                          "password": "AdminPass123!",
                          "firstName": "Jane",
                          "lastName": "Smith",
                          "roles": ["ADMIN", "SUPER_ADMIN"]
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
                        "message": "INVALID_ROLE",
                        "data": null
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
          description = "Forbidden - Insufficient permissions to create admin",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Forbidden Error",
                  value = """
                      {
                        "code": "403001",
                        "message": "INSUFFICIENT_PERMISSIONS",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "409",
          description = "Admin already exists",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Conflict Error",
                  value = """
                      {
                        "code": "409001",
                        "message": "ADMIN_ALREADY_EXISTS",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<AdminCreationResponse> createAdmin(@RequestBody @Valid AdminCreationRequest adminCreationRequest) {
    AdminCreationResponse adminCreationResponse = adminService.createAdmin(adminCreationRequest);
    return ApiResponse.<AdminCreationResponse>builder()
        .data(adminCreationResponse)
        .build();
  }

  @GetMapping
  @Operation(
      summary = "Get admin profile by ID",
      description = "Retrieves the profile information of the authenticated administrator. The admin ID is extracted from the request header.",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admin profile retrieved successfully",
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
                          "adminId": "admin-123e4567-e89b-12d3-a456-426614174000",
                          "phoneCode": "+1",
                          "phoneNumber": "9876543210",
                          "email": "admin@smartrent.com",
                          "firstName": "Jane",
                          "lastName": "Smith",
                          "idDocument": null,
                          "taxNumber": null,
                          "roles": ["ADMIN", "SUPER_ADMIN"]
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
          description = "Admin not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "404001",
                        "message": "ADMIN_NOT_FOUND",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetAdminResponse> getAdminById(
      @Parameter(
          name = "X-Admin-Id",
          description = "The unique identifier of the administrator",
          required = true,
          example = "admin-123e4567-e89b-12d3-a456-426614174000"
      )
      @RequestHeader(Constants.ADMIN_ID) String id) {
    GetAdminResponse getAdminResponse = adminService.getAdminById(id);
    return ApiResponse.<GetAdminResponse>builder()
        .data(getAdminResponse)
        .build();
  }
}
