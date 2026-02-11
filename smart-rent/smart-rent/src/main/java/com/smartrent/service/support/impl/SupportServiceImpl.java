package com.smartrent.service.support.impl;

import com.smartrent.config.AdminContactProperties;
import com.smartrent.dto.response.SupportContactResponse;
import com.smartrent.service.support.SupportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupportServiceImpl implements SupportService {

  AdminContactProperties adminContactProperties;

  @Override
  public SupportContactResponse getAdminContact() {
    String contactPhone = adminContactProperties.getContactPhoneNumber();

    log.info("Retrieving admin contact information");

    return SupportContactResponse.builder()
        .adminZaloPhoneNumber(contactPhone)
        .zaloLink("https://zalo.me/" + contactPhone)
        .build();
  }
}

