package com.smartrent.service.otp.store;

import com.smartrent.enums.OtpChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryOtpStore
 */
class InMemoryOtpStoreTest {

    private InMemoryOtpStore otpStore;

    @BeforeEach
    void setUp() {
        otpStore = new InMemoryOtpStore();
    }

    @AfterEach
    void tearDown() {
        otpStore.shutdown();
    }

    @Test
    void testStore_shouldStoreOtpData() {
        OtpData otpData = createTestOtpData();
        
        boolean result = otpStore.store(otpData, 300);
        
        assertTrue(result);
    }

    @Test
    void testStore_shouldNotStoreDuplicateKey() {
        OtpData otpData = createTestOtpData();
        
        otpStore.store(otpData, 300);
        boolean result = otpStore.store(otpData, 300);
        
        assertFalse(result);
    }

    @Test
    void testRetrieve_shouldReturnStoredOtpData() {
        OtpData otpData = createTestOtpData();
        otpStore.store(otpData, 300);
        
        Optional<OtpData> retrieved = otpStore.retrieve(otpData.getPhone(), otpData.getRequestId());
        
        assertTrue(retrieved.isPresent());
        assertEquals(otpData.getPhone(), retrieved.get().getPhone());
        assertEquals(otpData.getRequestId(), retrieved.get().getRequestId());
    }

    @Test
    void testRetrieve_shouldReturnEmptyForNonExistentKey() {
        Optional<OtpData> retrieved = otpStore.retrieve("+84912345678", "non-existent");
        
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testRetrieve_shouldReturnEmptyForExpiredOtp() throws InterruptedException {
        OtpData otpData = createTestOtpData();
        otpData.setExpiresAt(Instant.now().plusSeconds(1));
        otpStore.store(otpData, 1);
        
        Thread.sleep(1100);
        
        Optional<OtpData> retrieved = otpStore.retrieve(otpData.getPhone(), otpData.getRequestId());
        
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testDelete_shouldDeleteOtpData() {
        OtpData otpData = createTestOtpData();
        otpStore.store(otpData, 300);
        
        boolean deleted = otpStore.delete(otpData.getPhone(), otpData.getRequestId());
        
        assertTrue(deleted);
        assertFalse(otpStore.exists(otpData.getPhone(), otpData.getRequestId()));
    }

    @Test
    void testDelete_shouldReturnFalseForNonExistentKey() {
        boolean deleted = otpStore.delete("+84912345678", "non-existent");
        
        assertFalse(deleted);
    }

    @Test
    void testUpdate_shouldUpdateOtpData() {
        OtpData otpData = createTestOtpData();
        otpStore.store(otpData, 300);
        
        otpData.setAttempts(3);
        boolean updated = otpStore.update(otpData, 300);
        
        assertTrue(updated);
        
        Optional<OtpData> retrieved = otpStore.retrieve(otpData.getPhone(), otpData.getRequestId());
        assertTrue(retrieved.isPresent());
        assertEquals(3, retrieved.get().getAttempts());
    }

    @Test
    void testExists_shouldReturnTrueForExistingKey() {
        OtpData otpData = createTestOtpData();
        otpStore.store(otpData, 300);
        
        boolean exists = otpStore.exists(otpData.getPhone(), otpData.getRequestId());
        
        assertTrue(exists);
    }

    @Test
    void testExists_shouldReturnFalseForNonExistentKey() {
        boolean exists = otpStore.exists("+84912345678", "non-existent");
        
        assertFalse(exists);
    }

    @Test
    void testExists_shouldReturnFalseForExpiredOtp() throws InterruptedException {
        OtpData otpData = createTestOtpData();
        otpData.setExpiresAt(Instant.now().plusSeconds(1));
        otpStore.store(otpData, 1);
        
        Thread.sleep(1100);
        
        boolean exists = otpStore.exists(otpData.getPhone(), otpData.getRequestId());
        
        assertFalse(exists);
    }

    private OtpData createTestOtpData() {
        return OtpData.builder()
            .hashedCode("hashed_code")
            .phone("+84912345678")
            .requestId("test-request-id")
            .channel(OtpChannel.ZALO)
            .attempts(0)
            .maxAttempts(5)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .verified(false)
            .build();
    }
}

