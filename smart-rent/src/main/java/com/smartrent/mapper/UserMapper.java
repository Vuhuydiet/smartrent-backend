package com.smartrent.mapper;

import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.User;

public interface UserMapper {
  User mapFromUserCreationRequestToUserEntity(UserCreationRequest userCreationRequest);

  UserCreationResponse mapFromUserEntityToUserCreationResponse(User user);

  GetUserResponse mapFromUserEntityToGetUserResponse(User user);
}
