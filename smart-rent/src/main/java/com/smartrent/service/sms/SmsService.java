package com.smartrent.service.sms;

import com.smartrent.dto.request.MockSmsRequest;
import com.smartrent.dto.response.SmsResponse;

public interface SmsService {
    SmsResponse sendMessage(MockSmsRequest smsRequest);
}