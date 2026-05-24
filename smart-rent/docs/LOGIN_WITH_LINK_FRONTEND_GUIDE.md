# Login with Link (Magic-Link) – Frontend Integration Guide

## Overview

This guide explains how to integrate the **passwordless magic-link login** feature into the frontend.

Customers type only their email, receive a one-time login link, and click it. What happens next depends on whether the email is already in our database:

| Caller's email                  | Outcome of verify                                                  |
|---------------------------------|--------------------------------------------------------------------|
| Matches a registered user       | **Full session**: access + refresh tokens for that user. Same as a normal login. If they were unverified, we also flip `isVerified=true`. |
| Doesn't match any user          | **Guest session**: short-lived access token only, no DB row, no refresh. |

The FE flow is identical in both cases — the only difference is in the verify response (`guest: true/false` and the presence/absence of `refreshToken`).

> **Important:** the `/magic-link/request` endpoint **never** reveals which branch you'll get. The response is the same regardless of whether the email exists. Do not show "no account found" — that would let attackers enumerate accounts.

---

## High-level flow

```
┌──────────────────┐   1. POST /magic-link/request   ┌──────────────────────────────────┐
│  FE: email form  │ ──────────────────────────────▶ │  BE: lookup user by email,       │
└──────────────────┘                                  │       sign JWT (carries userId   │
                                                      │       or guest:<uuid>), send mail│
                                                      └────────────┬─────────────────────┘
                                                                   │
                                                                   ▼
                                                        ┌─────────────────────┐
                                                        │  Email inbox        │
                                                        │  "Đăng nhập ngay"   │
                                                        └──────────┬──────────┘
                                                                   │ user clicks
                                                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  Browser opens https://<CLIENT_URL>/auth/magic-link?token=<JWT>              │
│  FE route reads `token` from URL, calls POST /magic-link/verify              │
└──────────────────────────────────────────────────────────────────────────────┘
                                                                   │
                                  2. POST /magic-link/verify       │
                                                                   ▼
                                                ┌─────────────────────────────────┐
                                                │  BE: invalidate jti             │
                                                │  if userId: load user, mint     │
                                                │     access+refresh; mark        │
                                                │     verified if needed          │
                                                │  else: mint guest access only   │
                                                └────────────┬────────────────────┘
                                                             ▼
                                                ┌─────────────────────────────────┐
                                                │  FE stores token(s),            │
                                                │  branches on `guest`            │
                                                └─────────────────────────────────┘
```

The token in the URL is **single-use**. Calling `/verify` a second time with the same token returns `20002 MAGIC_LINK_ALREADY_USED`.

---

## API reference

Base URL: same as the rest of the API (`https://dev.api.smartrent.io.vn`, `https://api.smartrent.io.vn`, …).

Both endpoints are public — **do not** send an `Authorization` header.

### 1. Request a magic link

`POST /v1/auth/magic-link/request`

**Request**

```json
{ "email": "user@example.com" }
```

| Field   | Type   | Required | Notes                                                  |
|---------|--------|----------|--------------------------------------------------------|
| `email` | string | ✅       | Standard email format. Normalized to lowercase server-side. |

**Success — `200`** (identical whether or not the email is registered):

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "email": "user@example.com",
    "expiresInSeconds": 600
  }
}
```

`expiresInSeconds` is the TTL of the **link**, not the access token. Use it to drive the "resend link" countdown on the FE.

**Errors**

| HTTP | code     | message         | When                              |
|------|----------|-----------------|-----------------------------------|
| 400  | `2004`   | `INVALID_EMAIL` | Empty or malformed email          |
| 503  | `1002`   | Cannot send email | Brevo / SMTP downstream failure |

---

### 2. Verify the magic link

`POST /v1/auth/magic-link/verify`

Call this from the FE route that handles the `?token=…` URL — **not** from a backend redirect. Keeping verification client-side means the token never appears in our server access logs.

**Request**

```json
{ "token": "eyJhbGciOiJIUzUxMiJ9..." }
```

| Field   | Type   | Required | Notes                                                  |
|---------|--------|----------|--------------------------------------------------------|
| `token` | string | ✅       | Exactly the value of the `token` query param from the email URL. URL-decode if your router didn't do it for you. |

**Success — `200` (registered user)**

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresInSeconds": 3600,
    "email": "user@example.com",
    "guest": false,
    "userId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Success — `200` (guest, no account)**

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresInSeconds": 3600,
    "email": "stranger@example.com",
    "guest": true
  }
}
```

| Field              | Type    | Notes |
|--------------------|---------|-------|
| `accessToken`      | string  | Send as `Authorization: Bearer <accessToken>` on subsequent calls. Same shape and signer as a normal login token. |
| `refreshToken`     | string  | **Present only when `guest=false`.** Use it against `POST /v1/auth/refresh` exactly like after a normal login. |
| `expiresInSeconds` | number  | TTL of the access token. |
| `email`            | string  | Same email the user typed. |
| `guest`            | boolean | `true` → no DB row, no refresh token, limited capabilities. `false` → real user, full access. |
| `userId`           | string  | UUID of the registered user. Present only when `guest=false`. |

**Errors**

| HTTP | code     | message                                  | When                                                  |
|------|----------|------------------------------------------|-------------------------------------------------------|
| 400  | `2001`   | `EMPTY_INPUT`                            | Token missing                                         |
| 401  | `20001`  | Magic link is invalid or has expired     | Bad signature, expired, malformed, user row gone     |
| 401  | `20002`  | Magic link has already been used         | jti is in the invalidation cache (single-use enforced) |

Show a friendly "This link no longer works — request a new one" message for both `20001` and `20002`.

---

## What "guest" mode can and can't do

A guest token is signed with the same `ACCESS_SIGNER_KEY` as a regular user token, so it passes Spring Security's `authenticated()` check. **But** the `sub` claim looks like `"guest:<uuid>"` and points to no DB row.

| Backend code path                                      | Works for guest? |
|--------------------------------------------------------|------------------|
| Endpoints that check only `Authentication != null`     | ✅               |
| Endpoints that read `Authentication.getName()` (user-id) and use it as an opaque key | ⚠️ Works syntactically — the key is `guest:<uuid>` — but cross-session continuity is impossible. |
| Endpoints that do `userRepository.findById(authName)`  | ❌ Returns `4001 USER_NOT_FOUND` |
| Anything tied to `User` data: profile, packages, broker, payments, push, follow | ❌               |

Use the `guest` flag on the verify response to gate UI accordingly — show registered features as locked behind an "Upgrade to a full account" CTA.

---

## Frontend implementation

### Routes you need

| Route                       | What it does                                                      |
|-----------------------------|-------------------------------------------------------------------|
| `/login` (or wherever)      | Hosts the email form that calls `/magic-link/request`.            |
| `/auth/magic-link`          | Reads `?token=…`, calls `/magic-link/verify`, stores tokens, branches on `guest`. **This path must match `application.authentication.magic-link.callback-path` in the backend config** (default `/auth/magic-link`). |

If you want a different callback path, ask backend to update `MAGIC_LINK_CALLBACK_PATH` env var — don't hard-code an alternative on the FE only, since the link inside the email is built server-side.

### Storing the tokens

```ts
function persistSession(data: {
  accessToken: string;
  refreshToken?: string;
  expiresInSeconds: number;
  email: string;
  guest: boolean;
  userId?: string;
}) {
  // Registered users: behave exactly like normal login — use the same
  // storage/refresh logic you already have for /v1/auth POST /login.
  if (!data.guest) {
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken!);
    localStorage.setItem("userId", data.userId!);
    return;
  }
  // Guests: ephemeral. No refresh token to spend, so a tab-scoped store
  // is the safest default.
  sessionStorage.setItem("accessToken", data.accessToken);
  sessionStorage.setItem("guestEmail", data.email);
  setTimeout(() => {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("guestEmail");
  }, data.expiresInSeconds * 1000);
}
```

### Header usage

Add `Authorization: Bearer <accessToken>` on every authenticated call, regardless of guest/user. Your existing interceptor should not need changes.

### Logout

| Session | How to log out |
|---|---|
| Registered user (`guest=false`) | Call `POST /v1/auth/logout` with the access token — same as normal logout. Clear storage. |
| Guest (`guest=true`)            | **Do not** call `/v1/auth/logout` — guest tokens have no `rfId` claim and the endpoint will fail. Just clear `sessionStorage`. |

### React example — request form

```tsx
import { useState } from "react";

export function MagicLinkRequestForm() {
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState<"idle" | "sending" | "sent" | "error">("idle");
  const [cooldown, setCooldown] = useState(0);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setStatus("sending");
    try {
      const res = await fetch("/v1/auth/magic-link/request", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      const json = await res.json();
      if (json.code === "999999") {
        setStatus("sent");
        setCooldown(json.data.expiresInSeconds);
      } else {
        setStatus("error");
      }
    } catch {
      setStatus("error");
    }
  }

  if (status === "sent") {
    return (
      <p>
        Đã gửi liên kết đăng nhập tới <b>{email}</b>. Vui lòng kiểm tra hộp thư
        trong vòng {Math.floor(cooldown / 60)} phút.
      </p>
    );
  }

  return (
    <form onSubmit={onSubmit}>
      <input
        type="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
      />
      <button disabled={status === "sending"}>Gửi liên kết</button>
      {status === "error" && <p>Đã có lỗi xảy ra. Vui lòng thử lại.</p>}
    </form>
  );
}
```

### React example — verify route

`/auth/magic-link` page (Next.js App Router shown; adapt to your router):

```tsx
"use client";
import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function MagicLinkCallback() {
  const router = useRouter();
  const params = useSearchParams();
  const token = params.get("token");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setError("Liên kết không hợp lệ.");
      return;
    }

    (async () => {
      try {
        const res = await fetch("/v1/auth/magic-link/verify", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ token }),
        });
        const json = await res.json();

        if (json.code === "999999") {
          const { accessToken, refreshToken, email, guest, userId, expiresInSeconds } = json.data;

          if (guest) {
            sessionStorage.setItem("accessToken", accessToken);
            sessionStorage.setItem("guestEmail", email);
            setTimeout(() => {
              sessionStorage.removeItem("accessToken");
              sessionStorage.removeItem("guestEmail");
            }, expiresInSeconds * 1000);
            router.replace("/?welcome=guest");
          } else {
            localStorage.setItem("accessToken", accessToken);
            localStorage.setItem("refreshToken", refreshToken);
            localStorage.setItem("userId", userId);
            router.replace("/");
          }
        } else if (json.code === "20001" || json.code === "20002") {
          setError("Liên kết đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu liên kết mới.");
        } else {
          setError("Đăng nhập thất bại. Vui lòng thử lại.");
        }
      } catch {
        setError("Không thể kết nối tới máy chủ.");
      }
    })();
  }, [token, router]);

  if (error) {
    return (
      <div>
        <p>{error}</p>
        <a href="/login">Yêu cầu liên kết mới</a>
      </div>
    );
  }
  return <p>Đang đăng nhập…</p>;
}
```

### Detecting a guest in your auth state

If you decode the JWT, look at the payload:

```json
// Registered user
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "scope": "ROLE_USER",
  "rfId": "…",
  "user": { "userId": "550e…", "email": "user@example.com", "isVerified": true, "isBroker": false }
}

// Guest
{
  "sub": "guest:1f3c…",
  "scope": "ROLE_USER",
  "guest": true,
  "email": "guest@example.com",
  "user": { "userId": "guest:1f3c…", "email": "guest@example.com", "isVerified": false }
}
```

A simple guard:

```ts
function isGuest(claims: { sub?: string; guest?: boolean }) {
  return claims.guest === true || claims.sub?.startsWith("guest:");
}
```

The cleanest source of truth, however, is the `guest` flag returned by `/magic-link/verify` — persist it next to the token so you don't need to decode the JWT later.

---

## Edge cases to handle in UI

| Scenario                                          | Suggested UX |
|---------------------------------------------------|--------------|
| User has an account but uses magic-link instead of password | Works — they get the same access+refresh pair as a normal login. Treat as a logged-in user; the `guest` flag is `false`. |
| User is registered but never verified their email | Still works — clicking the link counts as email verification. The backend flips `isVerified=true` and returns full tokens. |
| User has no account                               | Gets a guest session. Show guest UI and offer registration. |
| User clicks the link twice                        | First click logs in. Second returns `20002` — show "already used, request a new link". |
| User waits >10 minutes before clicking            | Returns `20001` — show "link expired". |
| User account was deleted between request and click | Returns `20001` (the userId no longer resolves). Show the same "link expired" message. |
| User clicks the link in a different browser/device | Works — the JWT is self-contained. |
| User wants to switch identities                   | Clear stored tokens, return them to the email form. |
| Access token expires while user is active         | Guest: bounce to `/login`. Registered: use the refresh token like normal. |

---

## Security notes (for awareness, not action)

- The magic-link JWT is signed with `HS512` using `MAGIC_LINK_SIGNER_KEY` and carries `sub` (`userId` for registered users or `guest:<uuid>` for non-users), `email`, `jti`, `guest` (boolean), and `exp` (10 min default).
- After verify, the `jti` is added to the `auth.invalidatedTokens` Redis cache (24h TTL) — that's how we enforce single-use.
- The session token issued on verify uses the same signer keys as normal login (`ACCESS_SIGNER_KEY`, `REFRESH_SIGNER_KEY`), so it flows through the existing `CustomJwtDecoder` / Spring Security pipeline transparently.
- We deliberately do **not** reveal whether the email is registered when `/request` is called — same response either way. Don't change that on the FE either.
- Clicking the link is treated as proof of email ownership; we use that to auto-verify previously unverified accounts (same security guarantee as the OTP flow).

---

## Backend configuration (for reference / ops)

These are set in `application.yml`; the FE doesn't read them but it helps to know what's tunable:

| Property | Default | Notes |
|---|---|---|
| `application.authentication.jwt.magic-link-signer-key` | falls back to `RESET_PASSWORD_SIGNER_KEY` | Set `MAGIC_LINK_SIGNER_KEY` env var in prod. |
| `application.authentication.jwt.magic-link-duration` | `600` (seconds, 10 min) | TTL of the link. |
| `application.authentication.jwt.valid-duration` | env `VALID_DURATION` | TTL of the access token (guest and user). |
| `application.authentication.jwt.refreshable-duration` | env `REFRESHABLE_DURATION` | TTL of the refresh token (registered users only). |
| `application.authentication.magic-link.callback-path` | `/auth/magic-link` | Path appended to `client-url` when building the email link. Must match the FE route. |
| `application.authentication.magic-link.email-subject` | `SmartRent - Đăng nhập với liên kết` | Email subject line. |
| `application.client-url` | env `CLIENT_URL` | Base URL of the FE app. |

---

## FAQ

**Q: Can I make guest sessions persist across browser tabs?**
Yes — use `localStorage` instead of `sessionStorage`. Just honor the expiry timer; the backend won't refresh a guest token.

**Q: Can a guest later become a registered user?**
Yes — route them to the normal `/v1/users` registration flow, prefilling the email from `sessionStorage.getItem("guestEmail")`. The two identities are independent on the backend until they finish registration, at which point future magic-link requests will resolve to the new `User` row.

**Q: Does the magic-link work for admins?**
No. The lookup is against the `users` table only. Admins use `POST /v1/admins/auth`.

**Q: Does it work for brokers?**
Yes — a broker is just a `User` with `isBroker=true`. The magic-link returns their normal session and the FE can show broker features as usual.

**Q: What's the rate-limit on `/magic-link/request`?**
There isn't one yet. If you see abuse during rollout, ping backend to add a per-email/per-IP throttle.

**Q: Can I use a deep link (iOS/Android) instead of a web URL?**
Yes — change `MAGIC_LINK_CALLBACK_PATH` to your app's deep-link path (e.g. `smartrent://auth/magic-link`) and `CLIENT_URL` to the scheme prefix.
