package com.smartrent.service.user;

import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UpdateContactPhoneRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.request.UserUpdateRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.User;

public interface UserService {
  UserCreationResponse createUser(UserCreationRequest request);

  GetUserResponse getUserById(String id);

  PageResponse<GetUserResponse> getUsers(int page, int size);

  User internalCreateUser(InternalUserCreationRequest request);

  GetUserResponse updateContactPhone(String userId, UpdateContactPhoneRequest request);

  GetUserResponse updateUser(String userId, UserUpdateRequest request);

  void deleteUser(String userId);
}
