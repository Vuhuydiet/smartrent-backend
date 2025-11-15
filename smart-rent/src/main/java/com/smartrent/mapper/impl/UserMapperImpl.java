package com.smartrent.mapper.impl;

import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.UserCreationResponse;
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
        .contactPhoneNumber(userCreationRequest.getContactPhoneNumber())
        .contactPhoneVerified(false) // New users start with unverified contact phone
        .build();
  }

  @Override
  public User mapFromInternalUserCreationRequestToUserEntity(
      InternalUserCreationRequest userCreationRequest) {
    return User.builder()
        .email(userCreationRequest.getEmail())
        .firstName(userCreationRequest.getFirstName())
        .lastName(userCreationRequest.getLastName())
        .idDocument(userCreationRequest.getIdDocument())
        .taxNumber(userCreationRequest.getTaxNumber())
        .phoneCode(userCreationRequest.getPhoneCode())
        .phoneNumber(userCreationRequest.getPhoneNumber())
        .contactPhoneNumber(userCreationRequest.getContactPhoneNumber())
        .contactPhoneVerified(false) // New users start with unverified contact phone
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
        .contactPhoneNumber(user.getContactPhoneNumber())
        .contactPhoneVerified(user.getContactPhoneVerified())
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
        .contactPhoneNumber(user.getContactPhoneNumber())
        .contactPhoneVerified(user.getContactPhoneVerified())
        .build();
  }
}
