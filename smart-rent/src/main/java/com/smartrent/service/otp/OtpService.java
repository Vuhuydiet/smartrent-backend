package com.smartrent.service.otp;

import com.smartrent.config.otp.OtpProperties;
import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.request.OtpVerifyRequest;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.dto.response.OtpVerifyResponse;
import com.smartrent.enums.OtpChannel;
import com.smartrent.infra.exception.OtpException;
import com.smartrent.infra.exception.OtpNotFoundException;
import com.smartrent.infra.exception.OtpVerificationException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.otp.provider.OtpProvider;
import com.smartrent.service.otp.provider.OtpProviderResult;
import com.smartrent.service.otp.store.OtpData;
import com.smartrent.service.otp.store.OtpStore;
import com.smartrent.service.otp.util.OtpUtil;
import com.smartrent.service.otp.util.PhoneNumberUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Main OTP service handling send and verify operations
 * Implements channel fallback logic: Zalo -> SMS
 */
@Slf4j
@Service
public class OtpService {

    private final OtpProperties otpProperties;
    private final OtpStore otpStore;
    private final OtpUtil otpUtil;
    private final PhoneNumberUtil phoneNumberUtil;
    private final RateLimitService rateLimitService;
    private final List<OtpProvider> providers;
    private final Map<OtpChannel, OtpProvider> providerMap;
    
    // Metrics
    private final Counter otpSendSuccessCounter;
    private final Counter otpSendFailureCounter;
    private final Counter otpVerifySuccessCounter;
    private final Counter otpVerifyFailureCounter;
    private final Counter otpFallbackCounter;

    public OtpService(OtpProperties otpProperties,
                     OtpStore otpStore,
                     OtpUtil otpUtil,
                     PhoneNumberUtil phoneNumberUtil,
                     RateLimitService rateLimitService,
                     List<OtpProvider> providers,
                     MeterRegistry meterRegistry) {
        this.otpProperties = otpProperties;
        this.otpStore = otpStore;
        this.otpUtil = otpUtil;
        this.phoneNumberUtil = phoneNumberUtil;
        this.rateLimitService = rateLimitService;
        this.providers = providers;
        this.providerMap = buildProviderMap(providers);
        
        // Initialize metrics
        this.otpSendSuccessCounter = Counter.builder("otp.send.success")
            .description("Number of successful OTP sends")
            .register(meterRegistry);
        this.otpSendFailureCounter = Counter.builder("otp.send.failure")
            .description("Number of failed OTP sends")
            .register(meterRegistry);
        this.otpVerifySuccessCounter = Counter.builder("otp.verify.success")
            .description("Number of successful OTP verifications")
            .register(meterRegistry);
        this.otpVerifyFailureCounter = Counter.builder("otp.verify.failure")
            .description("Number of failed OTP verifications")
            .register(meterRegistry);
        this.otpFallbackCounter = Counter.builder("otp.fallback")
            .description("Number of OTP fallbacks to secondary channel")
            .register(meterRegistry);
    }

    /**
     * Send OTP to phone number
     * Implements fallback logic: Zalo -> SMS
     *
     * @param request OTP send request
     * @param ipAddress Client IP address for rate limiting
     * @return OTP send response
     */
    public OtpSendResponse sendOtp(OtpSendRequest request, String ipAddress) {
        String requestId = UUID.randomUUID().toString();
        
        log.info("OTP send request: requestId={}, phone={}", requestId, 
            phoneNumberUtil.maskPhoneNumber(request.getPhone()));

        try {
            // 1. Normalize and validate phone number
            String normalizedPhone = phoneNumberUtil.normalizeAndValidate(request.getPhone());

            // 2. Check rate limits
            rateLimitService.checkPhoneRateLimit(normalizedPhone);
            if (ipAddress != null) {
                rateLimitService.checkIpRateLimit(ipAddress);
            }

            // 3. Generate OTP code
            String otpCode = otpUtil.generateOtpCode();
            String hashedCode = otpUtil.hashOtpCode(otpCode);

            // 4. Determine channel order
            List<OtpChannel> channelOrder = determineChannelOrder(request.getPreferredChannels());

            // 5. Try sending via channels with fallback
            OtpProviderResult result = null;
            OtpChannel usedChannel = null;
            
            for (int i = 0; i < channelOrder.size(); i++) {
                OtpChannel channel = channelOrder.get(i);
                OtpProvider provider = providerMap.get(channel);
                
                if (provider == null || !provider.isAvailable()) {
                    log.warn("Provider not available for channel: {}", channel);
                    continue;
                }

                log.info("Attempting to send OTP via {}: requestId={}", channel, requestId);
                
                Map<String, Object> context = new HashMap<>();
                context.put("requestId", requestId);
                context.put("expiryMinutes", otpProperties.getTtlSeconds() / 60);
                
                result = provider.sendOtp(normalizedPhone, otpCode, context);
                
                if (result.isSuccess()) {
                    usedChannel = channel;
                    log.info("OTP sent successfully via {}: requestId={}, messageId={}", 
                        channel, requestId, result.getMessageId());
                    break;
                } else {
                    log.warn("Failed to send OTP via {}: requestId={}, error={}", 
                        channel, requestId, result.getErrorMessage());
                    
                    // If this is not the last channel and error is not retryable, try fallback
                    if (i < channelOrder.size() - 1) {
                        otpFallbackCounter.increment();
                        log.info("Falling back to next channel: {}", channelOrder.get(i + 1));
                    }
                }
            }

            // 6. Check if any channel succeeded
            if (result == null || !result.isSuccess() || usedChannel == null) {
                log.error("Failed to send OTP via all channels: requestId={}", requestId);
                otpSendFailureCounter.increment();
                throw new OtpException(DomainCode.OTP_SEND_FAILED);
            }

            // 7. Store OTP data
            OtpData otpData = OtpData.builder()
                .hashedCode(hashedCode)
                .phone(normalizedPhone)
                .requestId(requestId)
                .channel(usedChannel)
                .attempts(0)
                .maxAttempts(otpProperties.getMaxVerificationAttempts())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(otpProperties.getTtlSeconds()))
                .verified(false)
                .build();

            boolean stored = otpStore.store(otpData, otpProperties.getTtlSeconds());
            if (!stored) {
                log.error("Failed to store OTP: requestId={}", requestId);
                throw new OtpException(DomainCode.OTP_GENERATION_FAILED);
            }

            // 8. Build response
            otpSendSuccessCounter.increment();
            
            return OtpSendResponse.builder()
                .channel(usedChannel.getValue())
                .requestId(requestId)
                .ttlSeconds(otpProperties.getTtlSeconds())
                .maskedPhone(phoneNumberUtil.maskPhoneNumber(normalizedPhone))
                .build();

        } catch (OtpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending OTP: requestId={}", requestId, e);
            otpSendFailureCounter.increment();
            throw new OtpException(DomainCode.OTP_SEND_FAILED);
        }
    }

    /**
     * Verify OTP code
     *
     * @param request OTP verify request
     * @return OTP verify response
     */
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        log.info("OTP verify request: requestId={}, phone={}", 
            request.getRequestId(), phoneNumberUtil.maskPhoneNumber(request.getPhone()));

        try {
            // 1. Normalize phone number
            String normalizedPhone = phoneNumberUtil.normalizeAndValidate(request.getPhone());

            // 2. Retrieve OTP data
            Optional<OtpData> otpDataOpt = otpStore.retrieve(normalizedPhone, request.getRequestId());
            if (otpDataOpt.isEmpty()) {
                log.warn("OTP not found: requestId={}, phone={}", 
                    request.getRequestId(), phoneNumberUtil.maskPhoneNumber(normalizedPhone));
                otpVerifyFailureCounter.increment();
                throw new OtpNotFoundException();
            }

            OtpData otpData = otpDataOpt.get();

            // 3. Check if already verified
            if (Boolean.TRUE.equals(otpData.getVerified())) {
                log.warn("OTP already verified: requestId={}", request.getRequestId());
                throw new OtpVerificationException(DomainCode.OTP_ALREADY_VERIFIED);
            }

            // 4. Check if expired
            if (otpData.getExpiresAt().isBefore(Instant.now())) {
                log.warn("OTP expired: requestId={}", request.getRequestId());
                otpStore.delete(normalizedPhone, request.getRequestId());
                throw new OtpNotFoundException();
            }

            // 5. Check verification attempts
            if (otpData.getAttempts() >= otpData.getMaxAttempts()) {
                log.warn("Max verification attempts exceeded: requestId={}", request.getRequestId());
                otpStore.delete(normalizedPhone, request.getRequestId());
                throw new OtpVerificationException(DomainCode.OTP_VERIFICATION_ATTEMPTS_EXCEEDED);
            }

            // 6. Verify OTP code
            boolean codeMatches = otpUtil.verifyOtpCode(request.getCode(), otpData.getHashedCode());

            if (codeMatches) {
                // Success: mark as verified and delete
                otpData.setVerified(true);
                otpStore.delete(normalizedPhone, request.getRequestId());
                
                log.info("OTP verified successfully: requestId={}", request.getRequestId());
                otpVerifySuccessCounter.increment();
                
                return OtpVerifyResponse.builder()
                    .verified(true)
                    .message("OTP verified successfully")
                    .build();
            } else {
                // Failed: increment attempts
                otpData.setAttempts(otpData.getAttempts() + 1);
                int remainingSeconds = (int) (otpData.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
                otpStore.update(otpData, Math.max(0, remainingSeconds));
                
                int remainingAttempts = otpData.getMaxAttempts() - otpData.getAttempts();
                log.warn("OTP verification failed: requestId={}, remainingAttempts={}", 
                    request.getRequestId(), remainingAttempts);
                otpVerifyFailureCounter.increment();
                
                return OtpVerifyResponse.builder()
                    .verified(false)
                    .message("Invalid OTP code")
                    .remainingAttempts(remainingAttempts)
                    .build();
            }

        } catch (OtpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error verifying OTP: requestId={}", request.getRequestId(), e);
            otpVerifyFailureCounter.increment();
            throw new OtpException(DomainCode.OTP_INVALID_CODE);
        }
    }

    /**
     * Determine channel order based on preferred channels
     * Default: [ZALO, SMS]
     */
    private List<OtpChannel> determineChannelOrder(List<String> preferredChannels) {
        List<OtpChannel> channelOrder = new ArrayList<>();
        
        if (preferredChannels != null && !preferredChannels.isEmpty()) {
            for (String channelStr : preferredChannels) {
                try {
                    OtpChannel channel = OtpChannel.fromValue(channelStr);
                    if (!channelOrder.contains(channel)) {
                        channelOrder.add(channel);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown channel: {}", channelStr);
                }
            }
        }
        
        // Add default channels if not specified
        if (!channelOrder.contains(OtpChannel.ZALO)) {
            channelOrder.add(OtpChannel.ZALO);
        }
        if (!channelOrder.contains(OtpChannel.SMS)) {
            channelOrder.add(OtpChannel.SMS);
        }
        
        return channelOrder;
    }

    /**
     * Build provider map from provider list
     */
    private Map<OtpChannel, OtpProvider> buildProviderMap(List<OtpProvider> providers) {
        Map<OtpChannel, OtpProvider> map = new HashMap<>();
        for (OtpProvider provider : providers) {
            map.put(provider.getChannel(), provider);
            log.info("Registered OTP provider: {} for channel: {}", 
                provider.getProviderName(), provider.getChannel());
        }
        return map;
    }
}

