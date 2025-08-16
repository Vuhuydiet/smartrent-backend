package com.smartrent.userauth.controller.dto;

import com.smartrent.userauth.controller.dto.request.UserCreationRequest;
import com.smartrent.userauth.controller.dto.response.GetUserDetailResponse;
import com.smartrent.userauth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.smartrent.common.config.Constants.USER_ID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    @PostMapping
    public void createUser(@RequestBody @Valid UserCreationRequest userCreationRequest) {
        userService.createUser(userCreationRequest);
    }

    @GetMapping
    public GetUserDetailResponse getUserDetail(@RequestHeader(USER_ID) @NotBlank String userId) {
        log.debug("getUserDetail {}", userId);
        return userService.getUserDetail(userId);
    }

}
