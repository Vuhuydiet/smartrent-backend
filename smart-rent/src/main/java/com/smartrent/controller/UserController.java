package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.UserCreationRequest;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.GetUserResponse;
import com.smartrent.controller.dto.response.UserCreationResponse;
import com.smartrent.service.user.UserService;
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
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

  UserService userService;

  @PostMapping
  ApiResponse<UserCreationResponse> createUser(@RequestBody @Valid UserCreationRequest userCreationRequest) {
    UserCreationResponse userCreationResponse = userService.createUser(userCreationRequest);
    return ApiResponse.<UserCreationResponse>builder()
        .data(userCreationResponse)
        .build();
  }

  @GetMapping
  ApiResponse<GetUserResponse> getUserById(@RequestHeader(Constants.USER_ID) String id) {
    GetUserResponse getUserResponse = userService.getUserById(id);
    return ApiResponse.<GetUserResponse>builder()
        .data(getUserResponse)
        .build();
  }
}
