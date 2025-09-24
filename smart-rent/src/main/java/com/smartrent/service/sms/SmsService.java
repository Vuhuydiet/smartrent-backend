package com.smartrent.service.sms;

import com.smartrent.controller.dto.request.MockSmsRequest;
import com.smartrent.controller.dto.response.MockSmsResponse;

public interface SmsService {
    MockSmsResponse sendMessage(MockSmsRequest smsRequest);
}