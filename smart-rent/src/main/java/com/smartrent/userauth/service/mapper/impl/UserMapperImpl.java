package com.smartrent.userauth.service.mapper.impl;

import com.smartrent.userauth.controller.dto.request.UserCreationRequest;
import com.smartrent.userauth.controller.dto.response.GetUserDetailResponse;
import com.smartrent.userauth.infra.repository.entity.UserEntity;
import com.smartrent.userauth.service.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserEntity mapFromUserCreationRequestToEntity(UserCreationRequest userCreationRequest) {
        return UserEntity.builder()
                .password(userCreationRequest.getPassword())
                .username(userCreationRequest.getUsername())
                .build();
    }

    @Override
    public GetUserDetailResponse mapFromEntityToGetUserDetailResponse(UserEntity userEntity) {
        return GetUserDetailResponse.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .build();
    }
}
