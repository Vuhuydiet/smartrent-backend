package com.smartrent.service.sms.impl;

import com.smartrent.controller.dto.request.MockSmsRequest;
import com.smartrent.controller.dto.response.MockSmsResponse;
import com.smartrent.service.sms.SmsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockSmsServiceImpl implements SmsService {

    @Override
    public MockSmsResponse sendMessage(MockSmsRequest smsRequest) {
        log.info("Mock SMS Service: Sending message to {} with content: {}", 
                smsRequest.getPhoneNumber(), smsRequest.getMessage());
        
        // Mock successful response
        return MockSmsResponse.builder()
                .success(true)
                .messageId(UUID.randomUUID().toString())
                .errorMessage(null)
                .errorCode(null)
                .build();
    }
}