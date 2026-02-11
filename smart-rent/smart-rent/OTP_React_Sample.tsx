/**
 * React Sample Component for OTP Phone Verification
 * 
 * This is a complete example showing how to integrate the OTP API
 * into a React application with TypeScript.
 * 
 * Features:
 * - Phone number input with validation
 * - Send OTP with loading state
 * - OTP code input (6 digits)
 * - Verify OTP with error handling
 * - Rate limit display
 * - Resend OTP functionality
 * - Countdown timer
 */

import React, { useState, useEffect } from 'react';
import axios from 'axios';

// API Configuration
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Types
interface OtpSendResponse {
  code: string;
  message: string;
  data: {
    channel: string;
    requestId: string;
    ttlSeconds: number;
    maskedPhone: string;
  };
}

interface OtpVerifyResponse {
  code: string;
  message: string;
  data: {
    verified: boolean;
    message: string;
    remainingAttempts?: number;
  };
}

interface ErrorResponse {
  code: string;
  message: string;
}

const OtpVerification: React.FC = () => {
  // State
  const [phone, setPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [requestId, setRequestId] = useState('');
  const [step, setStep] = useState<'phone' | 'verify'>('phone');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [remainingAttempts, setRemainingAttempts] = useState<number | null>(null);
  const [rateLimitPhone, setRateLimitPhone] = useState<number | null>(null);
  const [rateLimitIp, setRateLimitIp] = useState<number | null>(null);

  // Countdown timer
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  // Format phone number for display
  const formatPhone = (value: string): string => {
    return value.replace(/\D/g, '').slice(0, 10);
  };

  // Validate Vietnam phone number
  const isValidPhone = (value: string): boolean => {
    const phoneRegex = /^(0[3|5|7|8|9])[0-9]{8}$/;
    return phoneRegex.test(value);
  };

  // Send OTP
  const handleSendOtp = async () => {
    setError('');
    setSuccess('');

    if (!isValidPhone(phone)) {
      setError('Please enter a valid Vietnam phone number (e.g., 0912345678)');
      return;
    }

    setLoading(true);

    try {
      const response = await axios.post<OtpSendResponse>(
        `${API_BASE_URL}/otp/send`,
        { phone },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      // Extract rate limit headers
      const headers = response.headers;
      if (headers['x-ratelimit-remaining-phone']) {
        setRateLimitPhone(parseInt(headers['x-ratelimit-remaining-phone']));
      }
      if (headers['x-ratelimit-remaining-ip']) {
        setRateLimitIp(parseInt(headers['x-ratelimit-remaining-ip']));
      }

      // Save request ID and move to verification step
      setRequestId(response.data.data.requestId);
      setCountdown(response.data.data.ttlSeconds);
      setStep('verify');
      setSuccess(
        `OTP sent via ${response.data.data.channel.toUpperCase()} to ${response.data.data.maskedPhone}`
      );
    } catch (err: any) {
      if (axios.isAxiosError(err) && err.response) {
        const errorData = err.response.data as ErrorResponse;
        setError(errorData.message || 'Failed to send OTP');
      } else {
        setError('Network error. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Verify OTP
  const handleVerifyOtp = async () => {
    setError('');
    setSuccess('');

    if (otpCode.length !== 6) {
      setError('Please enter a 6-digit OTP code');
      return;
    }

    setLoading(true);

    try {
      const response = await axios.post<OtpVerifyResponse>(
        `${API_BASE_URL}/otp/verify`,
        {
          phone,
          code: otpCode,
          requestId,
        },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.data.verified) {
        setSuccess('Phone number verified successfully! ✓');
        // Handle successful verification (e.g., redirect, update user state)
        setTimeout(() => {
          // Navigate to next step or close modal
          console.log('Verification successful');
        }, 2000);
      } else {
        setError(response.data.data.message);
        setRemainingAttempts(response.data.data.remainingAttempts || null);
      }
    } catch (err: any) {
      if (axios.isAxiosError(err) && err.response) {
        const errorData = err.response.data as ErrorResponse;
        setError(errorData.message || 'Failed to verify OTP');
        
        // Handle specific error codes
        if (errorData.code === '10006') {
          setError('OTP expired. Please request a new one.');
          setStep('phone');
        } else if (errorData.code === '10008') {
          setError('Too many failed attempts. Please request a new OTP.');
          setStep('phone');
        }
      } else {
        setError('Network error. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Resend OTP
  const handleResendOtp = () => {
    setOtpCode('');
    setError('');
    setSuccess('');
    handleSendOtp();
  };

  // Reset form
  const handleReset = () => {
    setPhone('');
    setOtpCode('');
    setRequestId('');
    setStep('phone');
    setError('');
    setSuccess('');
    setCountdown(0);
    setRemainingAttempts(null);
  };

  return (
    <div className="otp-verification-container" style={{ maxWidth: '400px', margin: '0 auto', padding: '20px' }}>
      <h2>Phone Verification</h2>

      {/* Error Message */}
      {error && (
        <div style={{ padding: '10px', marginBottom: '10px', backgroundColor: '#fee', color: '#c00', borderRadius: '4px' }}>
          {error}
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div style={{ padding: '10px', marginBottom: '10px', backgroundColor: '#efe', color: '#0a0', borderRadius: '4px' }}>
          {success}
        </div>
      )}

      {/* Rate Limit Info */}
      {(rateLimitPhone !== null || rateLimitIp !== null) && (
        <div style={{ padding: '10px', marginBottom: '10px', backgroundColor: '#fef9e7', borderRadius: '4px', fontSize: '12px' }}>
          {rateLimitPhone !== null && <div>Remaining attempts (phone): {rateLimitPhone}</div>}
          {rateLimitIp !== null && <div>Remaining attempts (IP): {rateLimitIp}</div>}
        </div>
      )}

      {/* Step 1: Phone Input */}
      {step === 'phone' && (
        <div>
          <div style={{ marginBottom: '15px' }}>
            <label htmlFor="phone" style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
              Phone Number
            </label>
            <input
              id="phone"
              type="tel"
              value={phone}
              onChange={(e) => setPhone(formatPhone(e.target.value))}
              placeholder="0912345678"
              style={{ width: '100%', padding: '10px', fontSize: '16px', borderRadius: '4px', border: '1px solid #ccc' }}
              disabled={loading}
            />
            <small style={{ color: '#666' }}>Enter your Vietnam mobile number</small>
          </div>

          <button
            onClick={handleSendOtp}
            disabled={loading || !phone}
            style={{
              width: '100%',
              padding: '12px',
              fontSize: '16px',
              fontWeight: 'bold',
              color: '#fff',
              backgroundColor: loading ? '#ccc' : '#007bff',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? 'Sending...' : 'Send OTP'}
          </button>
        </div>
      )}

      {/* Step 2: OTP Verification */}
      {step === 'verify' && (
        <div>
          <div style={{ marginBottom: '15px' }}>
            <label htmlFor="otp" style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
              Enter OTP Code
            </label>
            <input
              id="otp"
              type="text"
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="123456"
              style={{
                width: '100%',
                padding: '10px',
                fontSize: '24px',
                textAlign: 'center',
                letterSpacing: '8px',
                borderRadius: '4px',
                border: '1px solid #ccc',
              }}
              disabled={loading}
              autoFocus
            />
            <small style={{ color: '#666' }}>
              {countdown > 0 ? `Code expires in ${countdown}s` : 'Code expired'}
              {remainingAttempts !== null && ` • ${remainingAttempts} attempts remaining`}
            </small>
          </div>

          <button
            onClick={handleVerifyOtp}
            disabled={loading || otpCode.length !== 6}
            style={{
              width: '100%',
              padding: '12px',
              fontSize: '16px',
              fontWeight: 'bold',
              color: '#fff',
              backgroundColor: loading || otpCode.length !== 6 ? '#ccc' : '#28a745',
              border: 'none',
              borderRadius: '4px',
              cursor: loading || otpCode.length !== 6 ? 'not-allowed' : 'pointer',
              marginBottom: '10px',
            }}
          >
            {loading ? 'Verifying...' : 'Verify OTP'}
          </button>

          <button
            onClick={handleResendOtp}
            disabled={loading || countdown > 240}
            style={{
              width: '100%',
              padding: '10px',
              fontSize: '14px',
              color: '#007bff',
              backgroundColor: 'transparent',
              border: '1px solid #007bff',
              borderRadius: '4px',
              cursor: loading || countdown > 240 ? 'not-allowed' : 'pointer',
              marginBottom: '10px',
            }}
          >
            {countdown > 240 ? `Resend in ${countdown - 240}s` : 'Resend OTP'}
          </button>

          <button
            onClick={handleReset}
            disabled={loading}
            style={{
              width: '100%',
              padding: '10px',
              fontSize: '14px',
              color: '#666',
              backgroundColor: 'transparent',
              border: 'none',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            Change Phone Number
          </button>
        </div>
      )}
    </div>
  );
};

export default OtpVerification;

