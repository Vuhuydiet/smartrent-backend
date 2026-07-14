package com.smartrent.service.ai;

/**
 * Admin-configurable on/off flag for AI listing verification, persisted in
 * the {@code system_settings} table so it survives server restarts.
 *
 * <p>Currently gates whether the admin post-review dialog auto-runs AI
 * analysis when opened (instead of requiring a manual "Verify" click).
 */
public interface AiVerificationSettingService {

    boolean isAutoVerifyEnabled();

    void setAutoVerifyEnabled(boolean enabled, String updatedBy);
}
