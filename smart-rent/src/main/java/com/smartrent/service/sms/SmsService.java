package com.smartrent.service.sms;

import com.smartrent.dto.request.MockSmsRequest;
import com.smartrent.dto.response.MockSmsResponse;

public interface SmsService {
    MockSmsResponse sendMessage(MockSmsRequest smsRequest);
}