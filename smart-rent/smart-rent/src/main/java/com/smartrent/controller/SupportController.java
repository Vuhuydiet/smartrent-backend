package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.SupportContactResponse;
import com.smartrent.service.support.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/support")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Support", description = "APIs for customer support and contact information")
public class SupportController {

  SupportService supportService;

  @GetMapping("/contact")
  @Operation(
      summary = "Get admin contact information",
      description = "Retrieves the admin's Zalo contact information for customer support. This endpoint is publicly accessible."
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Admin contact information retrieved successfully",
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
                          "adminZaloPhoneNumber": "0123456789",
                          "zaloLink": "https://zalo.me/0123456789"
                        }
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<SupportContactResponse> getAdminContact() {
    SupportContactResponse response = supportService.getAdminContact();
    return ApiResponse.<SupportContactResponse>builder()
        .data(response)
        .build();
  }
}

