package com.smartrent.userauth.service;

import com.smartrent.userauth.controller.dto.request.UserCreationRequest;
import com.smartrent.userauth.controller.dto.response.GetUserDetailResponse;

public interface UserService {

    void createUser(UserCreationRequest userCreationRequest);

    GetUserDetailResponse getUserDetail(String userId);

}
