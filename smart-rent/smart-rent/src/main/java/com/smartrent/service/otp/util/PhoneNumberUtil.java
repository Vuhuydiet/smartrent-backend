package com.smartrent.service.otp.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;
import com.smartrent.infra.exception.OtpException;
import com.smartrent.infra.exception.model.DomainCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for phone number validation and normalization
 * Uses Google's libphonenumber library
 */
@Slf4j
@Component
public class PhoneNumberUtil {

    private static final String VIETNAM_COUNTRY_CODE = "VN";
    private static final int VIETNAM_COUNTRY_CALLING_CODE = 84;
    
    private final com.google.i18n.phonenumbers.PhoneNumberUtil phoneNumberUtil;

    public PhoneNumberUtil() {
        this.phoneNumberUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();
    }

    /**
     * Normalize phone number to E.164 format and validate it's a Vietnam number
     *
     * @param phoneNumber Phone number in any format
     * @return Normalized phone number in E.164 format (e.g., +84912345678)
     * @throws OtpException if phone number is invalid or not a Vietnam number
     */
    public String normalizeAndValidate(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new OtpException(DomainCode.OTP_INVALID_PHONE);
        }

        try {
            // Parse the phone number
            // Try parsing with VN region first, then without region hint
            Phonenumber.PhoneNumber parsedNumber;
            try {
                parsedNumber = phoneNumberUtil.parse(phoneNumber, VIETNAM_COUNTRY_CODE);
            } catch (NumberParseException e) {
                // Try parsing without region hint (for numbers with country code)
                parsedNumber = phoneNumberUtil.parse(phoneNumber, null);
            }

            // Validate the phone number
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                log.warn("Invalid phone number: {}", phoneNumber);
                throw new OtpException(DomainCode.OTP_INVALID_PHONE);
            }

            // Check if it's a Vietnam number
            if (parsedNumber.getCountryCode() != VIETNAM_COUNTRY_CALLING_CODE) {
                log.warn("Non-Vietnam phone number: {}", phoneNumber);
                throw new OtpException(DomainCode.OTP_NON_VIETNAM_PHONE);
            }

            // Return in E.164 format
            String e164Number = phoneNumberUtil.format(parsedNumber, 
                com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164);
            
            log.debug("Normalized phone number: {} -> {}", phoneNumber, e164Number);
            return e164Number;

        } catch (NumberParseException e) {
            log.warn("Failed to parse phone number: {}", phoneNumber, e);
            throw new OtpException(DomainCode.OTP_INVALID_PHONE);
        } catch (OtpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error normalizing phone number: {}", phoneNumber, e);
            throw new OtpException(DomainCode.OTP_INVALID_PHONE);
        }
    }

    /**
     * Mask phone number for display purposes
     * Example: +84912345678 -> +8491***5678
     *
     * @param phoneNumber Phone number in E.164 format
     * @return Masked phone number
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }

        int length = phoneNumber.length();
        int visibleStart = Math.min(5, length - 4);
        int visibleEnd = length - 4;

        StringBuilder masked = new StringBuilder();
        masked.append(phoneNumber, 0, visibleStart);
        masked.append("***");
        masked.append(phoneNumber.substring(visibleEnd));

        return masked.toString();
    }

    /**
     * Check if phone number is a Vietnam mobile number
     *
     * @param phoneNumber Phone number in E.164 format
     * @return true if it's a Vietnam mobile number
     */
    public boolean isVietnamMobileNumber(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, null);
            return parsedNumber.getCountryCode() == VIETNAM_COUNTRY_CALLING_CODE &&
                   phoneNumberUtil.getNumberType(parsedNumber) == 
                   com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType.MOBILE;
        } catch (Exception e) {
            return false;
        }
    }
}

