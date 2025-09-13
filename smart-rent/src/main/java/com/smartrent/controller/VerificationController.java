package com.smartrent.controller;

import com.smartrent.controller.dto.request.VerifyCodeRequest;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.service.authentication.VerificationService;
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
public class VerificationController {

  VerificationService verificationService;

  @PostMapping
  public ApiResponse<Void> verify(@RequestBody @Valid VerifyCodeRequest verifyCodeRequest) {
    verificationService.verifyCode(verifyCodeRequest);

    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/code")
  public ApiResponse<Void> sendCode(@RequestParam("email") String email) {
    verificationService.verifyCode(email);
    return ApiResponse.<Void>builder().build();
  }
}
