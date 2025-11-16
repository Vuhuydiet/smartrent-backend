package com.smartrent.controller;

import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.request.AdminUpdateRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.PageResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


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
  ApiResponse<GetAdminResponse> getAdminById() {
    // Extract admin ID from JWT token
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String id = authentication.getName();

    GetAdminResponse getAdminResponse = adminService.getAdminById(id);
    return ApiResponse.<GetAdminResponse>builder()
        .data(getAdminResponse)
        .build();
  }

  @GetMapping("/list")
  @Operation(
      summary = "Get all admins",
      description = "Retrieves a paginated list of all administrators in the system",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admins retrieved successfully",
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
                          "currentPage": 1,
                          "pageSize": 10,
                          "totalPages": 1,
                          "totalElements": 2,
                          "data": [
                            {
                              "adminId": "admin-123",
                              "phoneCode": "+1",
                              "phoneNumber": "9876543210",
                              "email": "admin1@smartrent.com",
                              "firstName": "Jane",
                              "lastName": "Smith",
                              "roles": ["ADMIN"]
                            },
                            {
                              "adminId": "admin-456",
                              "phoneCode": "+1",
                              "phoneNumber": "9876543211",
                              "email": "admin2@smartrent.com",
                              "firstName": "John",
                              "lastName": "Doe",
                              "roles": ["SUPER_ADMIN"]
                            }
                          ]
                        }
                      }
                      """
              )
          )
      )
  })
  ApiResponse<PageResponse<GetAdminResponse>> getAllAdmins(
      @Parameter(description = "Page number (1-indexed)", example = "1")
      @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page", example = "10")
      @RequestParam(defaultValue = "10") int size
  ) {
    PageResponse<GetAdminResponse> admins = adminService.getAllAdmins(page, size);
    return ApiResponse.<PageResponse<GetAdminResponse>>builder()
        .data(admins)
        .build();
  }

  @PutMapping("/{adminId}")
  @Operation(
      summary = "Update admin",
      description = "Updates an existing administrator's information",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admin updated successfully",
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
                          "adminId": "admin-123",
                          "phoneCode": "+1",
                          "phoneNumber": "9876543210",
                          "email": "updated@smartrent.com",
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
          responseCode = "404",
          description = "Admin not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "4014",
                        "message": "Admin not found",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetAdminResponse> updateAdmin(
      @Parameter(description = "Admin ID", required = true)
      @PathVariable String adminId,
      @Valid @RequestBody AdminUpdateRequest request
  ) {
    GetAdminResponse admin = adminService.updateAdmin(adminId, request);
    return ApiResponse.<GetAdminResponse>builder()
        .data(admin)
        .build();
  }

  @DeleteMapping("/{adminId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete admin",
      description = "Deletes an administrator from the system",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "204",
          description = "Admin deleted successfully"
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
                        "code": "4014",
                        "message": "Admin not found",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  void deleteAdmin(
      @Parameter(description = "Admin ID", required = true)
      @PathVariable String adminId
  ) {
    adminService.deleteAdmin(adminId);
  }
}
