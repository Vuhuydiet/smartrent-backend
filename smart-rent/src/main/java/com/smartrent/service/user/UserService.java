package com.smartrent.service.user;

import com.smartrent.controller.dto.request.UserCreationRequest;
import com.smartrent.controller.dto.response.GetUserResponse;
import com.smartrent.controller.dto.response.UserCreationResponse;

public interface UserService {
  UserCreationResponse createUser(UserCreationRequest request);

  GetUserResponse getUserById(String id);
}
