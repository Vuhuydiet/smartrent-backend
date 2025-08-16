package com.smartrent.userauth.service.impl;

import com.smartrent.userauth.controller.dto.request.UserCreationRequest;
import com.smartrent.userauth.controller.dto.response.GetUserDetailResponse;
import com.smartrent.userauth.infra.repository.UserRepository;
import com.smartrent.userauth.infra.repository.entity.UserEntity;
import com.smartrent.userauth.service.UserService;
import com.smartrent.userauth.service.mapper.UserMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    UserMapper userMapper;

    @Override
    public void createUser(UserCreationRequest userCreationRequest) {
        String id = UUID.randomUUID().toString();

        UserEntity userEntity = userMapper.mapFromUserCreationRequestToEntity(userCreationRequest);
        userEntity.setId(id);

        userRepository.save(userEntity);
    }

    @Override
    public GetUserDetailResponse getUserDetail(String userId) {
        UserEntity userEntity = userRepository.findById(userId).orElse(null);

        return userMapper.mapFromEntityToGetUserDetailResponse(userEntity);
    }
}
