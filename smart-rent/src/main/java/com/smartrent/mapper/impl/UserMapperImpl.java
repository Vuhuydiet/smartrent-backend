package com.smartrent.mapper.impl;

import com.smartrent.controller.dto.request.UserCreationRequest;
import com.smartrent.controller.dto.response.GetUserResponse;
import com.smartrent.controller.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

  @Override
  public User mapFromUserCreationRequestToUserEntity(UserCreationRequest userCreationRequest) {
    return User.builder()
        .email(userCreationRequest.getEmail())
        .password(userCreationRequest.getPassword())
        .firstName(userCreationRequest.getFirstName())
        .lastName(userCreationRequest.getLastName())
        .idDocument(userCreationRequest.getIdDocument())
        .taxNumber(userCreationRequest.getTaxNumber())
        .phoneCode(userCreationRequest.getPhoneCode())
        .phoneNumber(userCreationRequest.getPhoneNumber())
        .build();
  }

  @Override
  public UserCreationResponse mapFromUserEntityToUserCreationResponse(User user) {
    return UserCreationResponse.builder()
        .userId(user.getUserId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .idDocument(user.getIdDocument())
        .taxNumber(user.getTaxNumber())
        .phoneCode(user.getPhoneCode())
        .phoneNumber(user.getPhoneNumber())
        .build();
  }

  @Override
  public GetUserResponse mapFromUserEntityToGetUserResponse(User user) {
    return GetUserResponse.builder()
        .userId(user.getUserId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .idDocument(user.getIdDocument())
        .taxNumber(user.getTaxNumber())
        .phoneCode(user.getPhoneCode())
        .phoneNumber(user.getPhoneNumber())
        .build();
  }
}
