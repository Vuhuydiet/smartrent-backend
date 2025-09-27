package com.smartrent.service.user;

import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.UserCreationResponse;

public interface UserService {
  UserCreationResponse createUser(UserCreationRequest request);

  GetUserResponse getUserById(String id);
}
