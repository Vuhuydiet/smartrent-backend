# Google OAuth Login Troubleshooting Guide

## Overview

This guide helps you troubleshoot common issues with Google OAuth authentication in SmartRent.

## Error: "invalid_grant" - Bad Request

### What This Error Means

The `invalid_grant` error from Google OAuth typically indicates one of these issues:

1. **Authorization code already used** (most common)
2. **Authorization code expired** (codes expire in ~10 minutes)
3. **Redirect URI mismatch**
4. **Client ID/Secret mismatch**

### Common Causes & Solutions

#### 1. Authorization Code Already Used ⚠️

**Problem**: OAuth authorization codes are **single-use only**. Once exchanged for tokens, they cannot be reused.

**Solution**:
- Get a fresh authorization code from Google
- Don't retry the same request with the same code
- Implement proper error handling on the frontend to request a new code

**Frontend Flow**:
```javascript
// ❌ WRONG - Retrying with same code
try {
  await authenticateWithGoogle(code);
} catch (error) {
  await authenticateWithGoogle(code); // This will fail!
}

// ✅ CORRECT - Request new code on failure
try {
  await authenticateWithGoogle(code);
} catch (error) {
  // Redirect user back to Google OAuth to get new code
  window.location.href = googleOAuthUrl;
}
```

#### 2. Authorization Code Expired

**Problem**: Google authorization codes expire quickly (typically 10 minutes).

**Solution**:
- Exchange the code for tokens immediately after receiving it
- Don't store codes for later use
- If code expires, redirect user to Google OAuth again

#### 3. Redirect URI Mismatch

**Problem**: The `redirect_uri` in your backend must **exactly match** what's configured in Google Cloud Console.

**Check Your Configuration**:

1. **Google Cloud Console**:
   - Go to: https://console.cloud.google.com/
   - Navigate to: APIs & Services > Credentials
   - Click your OAuth 2.0 Client ID
   - Check "Authorized redirect URIs"

2. **Backend Configuration** (`.env` or environment variables):
   ```bash
   GOOGLE_AUTH_CLIENT_REDIRECT_URI=http://localhost:3000/auth/google/callback
   ```

3. **Frontend Configuration**:
   ```javascript
   const googleOAuthUrl = `https://accounts.google.com/o/oauth2/v2/auth?` +
     `client_id=${CLIENT_ID}&` +
     `redirect_uri=${encodeURIComponent('http://localhost:3000/auth/google/callback')}&` +
     `response_type=code&` +
     `scope=openid email profile`;
   ```

**Important**: All three must match **exactly**, including:
- Protocol (`http` vs `https`)
- Domain/Port (`localhost:3000` vs `127.0.0.1:3000`)
- Path (`/auth/google/callback` vs `/auth/callback`)
- Trailing slashes

#### 4. Client ID/Secret Mismatch

**Problem**: The credentials in your backend don't match your Google Cloud project.

**Verify**:
```bash
# Check your .env file
CLIENT_ID=your-client-id.apps.googleusercontent.com
CLIENT_SECRET=your-client-secret
```

**Get Correct Values**:
1. Go to Google Cloud Console
2. APIs & Services > Credentials
3. Click your OAuth 2.0 Client ID
4. Copy the Client ID and Client Secret

## Testing the OAuth Flow

### Step 1: Verify Google Cloud Console Setup

1. **Enable Google+ API** (if not already enabled):
   - Go to: APIs & Services > Library
   - Search for "Google+ API"
   - Click "Enable"

2. **Configure OAuth Consent Screen**:
   - Go to: APIs & Services > OAuth consent screen
   - Add test users if in "Testing" mode
   - Verify scopes include: `email`, `profile`, `openid`

3. **Verify Authorized Redirect URIs**:
   - Must include your frontend callback URL
   - Example: `http://localhost:3000/auth/google/callback`

### Step 2: Test the Complete Flow

1. **Frontend initiates OAuth**:
   ```javascript
   const CLIENT_ID = 'your-client-id.apps.googleusercontent.com';
   const REDIRECT_URI = 'http://localhost:3000/auth/google/callback';
   
   const googleOAuthUrl = 
     `https://accounts.google.com/o/oauth2/v2/auth?` +
     `client_id=${CLIENT_ID}&` +
     `redirect_uri=${encodeURIComponent(REDIRECT_URI)}&` +
     `response_type=code&` +
     `scope=openid email profile&` +
     `access_type=offline`;
   
   window.location.href = googleOAuthUrl;
   ```

2. **User authorizes on Google**

3. **Google redirects to your callback with code**:
   ```
   http://localhost:3000/auth/google/callback?code=4/0AY0e-g7...
   ```

4. **Frontend sends code to backend**:
   ```javascript
   const response = await fetch('http://localhost:8080/v1/auth/outbound/google', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({ code: authorizationCode })
   });
   ```

5. **Backend exchanges code for tokens and returns JWT**

## API Endpoint

### POST `/v1/auth/outbound/google`

**Request**:
```json
{
  "code": "4/0AY0e-g7xxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

**Success Response** (200):
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Error Responses**:

**Invalid/Expired Code** (401):
```json
{
  "code": "5005",
  "message": "Invalid or expired OAuth authorization code",
  "data": null
}
```

**OAuth Failed** (401):
```json
{
  "code": "5006",
  "message": "OAuth authentication failed",
  "data": null
}
```

## Environment Variables Checklist

Make sure these are set in your `.env` file or environment:

```bash
# Google OAuth Configuration
CLIENT_ID=your-client-id.apps.googleusercontent.com
CLIENT_SECRET=your-client-secret
GOOGLE_AUTH_CLIENT_REDIRECT_URI=http://localhost:3000/auth/google/callback

# JWT Configuration (should already exist)
ACCESS_SIGNER_KEY=your-access-signer-key
REFRESH_SIGNER_KEY=your-refresh-signer-key
VALID_DURATION=3600
REFRESHABLE_DURATION=86400
```

## Debugging Tips

### 1. Enable Debug Logging

Add to `application.yml`:
```yaml
logging:
  level:
    com.smartrent.service.authentication.impl.GoogleAuthenticationServiceImpl: DEBUG
    com.smartrent.infra.connector: DEBUG
```

### 2. Check Backend Logs

Look for these log messages:
```
DEBUG - Attempting to exchange Google authorization code for access token
DEBUG - Successfully exchanged authorization code, fetching user details from Google
DEBUG - Retrieved user info from Google: user@example.com
INFO  - Google OAuth authentication successful for user: user@example.com
```

### 3. Test with cURL

```bash
curl -X POST http://localhost:8080/v1/auth/outbound/google \
  -H "Content-Type: application/json" \
  -d '{"code":"YOUR_FRESH_AUTHORIZATION_CODE"}'
```

**Important**: Get a fresh code each time you test!

## Common Mistakes

1. ❌ Reusing the same authorization code
2. ❌ Redirect URI doesn't match exactly
3. ❌ Using `http` in production (should be `https`)
4. ❌ Wrong Client ID/Secret
5. ❌ Not enabling required Google APIs
6. ❌ Test user not added in OAuth consent screen (when in Testing mode)

## Production Checklist

Before deploying to production:

- [ ] Use HTTPS for all redirect URIs
- [ ] Update redirect URIs in Google Cloud Console for production domain
- [ ] Update `GOOGLE_AUTH_CLIENT_REDIRECT_URI` environment variable
- [ ] Verify OAuth consent screen is published (not in Testing mode)
- [ ] Test the complete flow in production environment
- [ ] Monitor error logs for OAuth failures

## Need More Help?

1. Check Google OAuth 2.0 documentation: https://developers.google.com/identity/protocols/oauth2
2. Review backend logs for detailed error messages
3. Verify all configuration matches between frontend, backend, and Google Cloud Console
4. Test with a fresh authorization code (never reuse codes)

## Auto-Registration Feature

When a user successfully authenticates with Google:
- If the user doesn't exist, a new account is automatically created
- User's email, first name, and last name are populated from Google
- No email verification is required (pre-verified by Google)
- User can immediately access the application

