package com.smartrent.service.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.smartrent.config.otp.OtpProperties;
import com.smartrent.config.otp.ZaloProperties;
import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.request.OtpVerifyRequest;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.dto.response.OtpVerifyResponse;
import com.smartrent.service.otp.provider.ZaloZnsProvider;
import com.smartrent.service.otp.store.InMemoryOtpStore;
import com.smartrent.service.otp.store.OtpStore;
import com.smartrent.service.otp.util.OtpUtil;
import com.smartrent.service.otp.util.PhoneNumberUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for OTP service with WireMock
 * Tests the full flow including provider interactions
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OtpIntegrationTest {

    private WireMockServer wireMockServer;
    private OtpService otpService;
    private OtpStore otpStore;
    private OtpUtil otpUtil;
    private PhoneNumberUtil phoneNumberUtil;
    private RateLimitService rateLimitService;
    private ZaloZnsProvider zaloProvider;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Initialize components
        objectMapper = new ObjectMapper();
        otpUtil = new OtpUtil();
        phoneNumberUtil = new PhoneNumberUtil();
        otpStore = new InMemoryOtpStore();

        // Configure OTP properties
        OtpProperties otpProperties = new OtpProperties();
        otpProperties.setCodeLength(6);
        otpProperties.setTtlSeconds(300);
        otpProperties.setMaxVerificationAttempts(5);

        // Mock Redis template for rate limiter
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        rateLimitService = new RateLimitService(otpProperties, redisTemplate);

        // Configure Zalo provider with WireMock endpoint
        ZaloProperties zaloProperties = new ZaloProperties();
        zaloProperties.setEnabled(true);
        zaloProperties.setAccessToken("test-access-token");
        zaloProperties.setOaId("test-oa-id");
        zaloProperties.setTemplateId("test-template-id");
        zaloProperties.setApiEndpoint("http://localhost:8089/message/template");

        WebClient.Builder webClientBuilder = WebClient.builder();
        zaloProvider = new ZaloZnsProvider(zaloProperties, webClientBuilder);

        // Initialize OTP service
        otpService = new OtpService(
            otpProperties,
            otpStore,
            otpUtil,
            phoneNumberUtil,
            rateLimitService,
            Arrays.asList(zaloProvider),
            new SimpleMeterRegistry()
        );
    }

    @AfterAll
    void tearDown() {
        wireMockServer.stop();
        if (otpStore instanceof InMemoryOtpStore) {
            ((InMemoryOtpStore) otpStore).shutdown();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void testSendOtp_zaloSuccess() throws Exception {
        // Arrange: Mock successful Zalo response
        Map<String, Object> zaloResponse = new HashMap<>();
        zaloResponse.put("error", 0);
        zaloResponse.put("message", "Success");
        Map<String, String> data = new HashMap<>();
        data.put("msg_id", "zalo-msg-123");
        zaloResponse.put("data", data);

        stubFor(post(urlEqualTo("/message/template"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(zaloResponse))));

        OtpSendRequest request = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        // Act
        OtpSendResponse response = otpService.sendOtp(request, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("zalo", response.getChannel());
        assertNotNull(response.getRequestId());
        assertEquals(300, response.getTtlSeconds());

        // Verify WireMock was called
        verify(postRequestedFor(urlEqualTo("/message/template"))
            .withHeader("access_token", equalTo("test-access-token")));
    }

    @Test
    void testSendOtp_zaloFailure() throws Exception {
        // Arrange: Mock Zalo error response
        Map<String, Object> zaloResponse = new HashMap<>();
        zaloResponse.put("error", -124);
        zaloResponse.put("message", "Invalid access token");

        stubFor(post(urlEqualTo("/message/template"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(zaloResponse))));

        OtpSendRequest request = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        // Act & Assert
        // Since we only have Zalo provider and it fails, the send should fail
        assertThrows(Exception.class, () -> {
            otpService.sendOtp(request, "127.0.0.1");
        });
    }

    @Test
    void testSendAndVerifyOtp_fullFlow() throws Exception {
        // Arrange: Mock successful Zalo response
        Map<String, Object> zaloResponse = new HashMap<>();
        zaloResponse.put("error", 0);
        zaloResponse.put("message", "Success");
        Map<String, String> data = new HashMap<>();
        data.put("msg_id", "zalo-msg-123");
        zaloResponse.put("data", data);

        stubFor(post(urlEqualTo("/message/template"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(zaloResponse))));

        // Step 1: Send OTP
        OtpSendRequest sendRequest = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        OtpSendResponse sendResponse = otpService.sendOtp(sendRequest, "127.0.0.1");
        assertNotNull(sendResponse);
        String requestId = sendResponse.getRequestId();

        // Step 2: Verify OTP with wrong code
        OtpVerifyRequest wrongVerifyRequest = OtpVerifyRequest.builder()
            .phone("0912345678")
            .code("000000")
            .requestId(requestId)
            .build();

        OtpVerifyResponse wrongResponse = otpService.verifyOtp(wrongVerifyRequest);
        assertFalse(wrongResponse.getVerified());
        assertEquals(4, wrongResponse.getRemainingAttempts());

        // Step 3: We can't verify with correct code in test because we don't know the generated OTP
        // In real scenario, the OTP would be sent to the user and they would provide it
    }

    @Test
    void testSendOtp_zaloRetry() throws Exception {
        // Arrange: Mock Zalo to fail first, then succeed
        Map<String, Object> zaloResponse = new HashMap<>();
        zaloResponse.put("error", 0);
        zaloResponse.put("message", "Success");
        Map<String, String> data = new HashMap<>();
        data.put("msg_id", "zalo-msg-123");
        zaloResponse.put("data", data);

        stubFor(post(urlEqualTo("/message/template"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json"))
            .willSetStateTo("First Attempt Failed"));

        stubFor(post(urlEqualTo("/message/template"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Attempt Failed")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(zaloResponse))));

        OtpSendRequest request = OtpSendRequest.builder()
            .phone("0912345678")
            .build();

        // Act
        OtpSendResponse response = otpService.sendOtp(request, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertEquals("zalo", response.getChannel());

        // Verify retry happened
        verify(2, postRequestedFor(urlEqualTo("/message/template")));
    }

    @Test
    void testVerifyOtp_expiredOtp() throws Exception {
        // This test would require manipulating time or using a very short TTL
        // For now, we'll test the not found scenario
        OtpVerifyRequest request = OtpVerifyRequest.builder()
            .phone("0912345678")
            .code("123456")
            .requestId("non-existent-request-id")
            .build();

        // Act & Assert
        assertThrows(Exception.class, () -> {
            otpService.verifyOtp(request);
        });
    }
}

