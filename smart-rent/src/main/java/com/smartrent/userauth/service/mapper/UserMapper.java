package com.smartrent.userauth.service.mapper;

import com.smartrent.userauth.controller.dto.request.UserCreationRequest;
import com.smartrent.userauth.controller.dto.response.GetUserDetailResponse;
import com.smartrent.userauth.infra.repository.entity.UserEntity;

public interface UserMapper {

    UserEntity mapFromUserCreationRequestToEntity(UserCreationRequest userCreationRequest);

    GetUserDetailResponse mapFromEntityToGetUserDetailResponse(UserEntity userEntity);

}
