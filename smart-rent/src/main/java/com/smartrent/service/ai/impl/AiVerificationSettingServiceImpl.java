package com.smartrent.service.ai.impl;

import com.smartrent.infra.repository.SystemSettingRepository;
import com.smartrent.infra.repository.entity.SystemSetting;
import com.smartrent.service.ai.AiVerificationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiVerificationSettingServiceImpl implements AiVerificationSettingService {

    private static final String AUTO_VERIFY_KEY = "ai_auto_verify_enabled";

    private final SystemSettingRepository systemSettingRepository;

    @Override
    public boolean isAutoVerifyEnabled() {
        return systemSettingRepository.findById(AUTO_VERIFY_KEY)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(true);
    }

    @Override
    @Transactional
    public void setAutoVerifyEnabled(boolean enabled, String updatedBy) {
        SystemSetting setting = systemSettingRepository.findById(AUTO_VERIFY_KEY)
                .orElse(SystemSetting.builder().settingKey(AUTO_VERIFY_KEY).build());
        setting.setSettingValue(String.valueOf(enabled));
        setting.setUpdatedBy(updatedBy);
        systemSettingRepository.save(setting);
        log.info("AI auto-verify setting set to {} by {}", enabled, updatedBy);
    }
}
