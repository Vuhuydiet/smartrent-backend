# Login with Link (Guest Magic-Link) – Frontend Integration Guide

## Overview

This guide explains how to integrate the **passwordless guest login** feature into the frontend.

Customers type only their email, receive a one-time login link, click it, and are signed in as a **guest** — no account creation, no password, no profile data stored on our side. The email lives inside the signed link itself.

> **Mental model:** think Slack/Notion "Sign in with email". The backend never inserts a row in `users`. The session is just a short-lived JWT scoped to the email address.

---

## When to use this flow

Use it for visitors who want a *lightweight* identity — enough to save listings, follow brokers, or chat — but who don't want to register a full account yet.

**Do not** use it for:

- Owners / brokers / admins. They must register normally.
- Anything that requires loading the user's record from the database (e.g. profile edit, broker dashboard, KYC). Guest tokens carry only an email — the backend won't find a `User` row for `sub = "guest:<uuid>"` and the call will return `4001 USER_NOT_FOUND`.

If you need to gate the UI, read the `guest` boolean from the verify response (or decode the JWT and look for `"guest": true` / `"scope": "ROLE_USER"` together with `sub` starting with `guest:`).

---

## High-level flow

```
┌──────────────────┐   1. POST /magic-link/request   ┌──────────────────────────┐
│  FE: email form  │ ──────────────────────────────▶ │  BE: sign JWT, send mail │
└──────────────────┘                                  └────────────┬─────────────┘
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
                                                        ┌─────────────────────┐
                                                        │  BE: invalidate jti │
                                                        │  return accessToken │
                                                        └──────────┬──────────┘
                                                                   ▼
                                                        ┌─────────────────────┐
                                                        │  FE stores token,   │
                                                        │  routes to /home    │
                                                        └─────────────────────┘
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
{ "email": "guest@example.com" }
```

| Field   | Type   | Required | Notes                                                  |
|---------|--------|----------|--------------------------------------------------------|
| `email` | string | ✅       | Standard email format. Normalized to lowercase server-side. |

**Success — `200`**

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "email": "guest@example.com",
    "expiresInSeconds": 600
  }
}
```

`expiresInSeconds` is the TTL of the **link**, not the access token. Use it to drive the "resend link" countdown on the FE.

> The response is intentionally the same regardless of whether the email exists in our database. Do **not** show the user "no account found" — that would leak account existence.

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

**Success — `200`**

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresInSeconds": 3600,
    "email": "guest@example.com",
    "guest": true
  }
}
```

| Field              | Type    | Notes |
|--------------------|---------|-------|
| `accessToken`      | string  | Send as `Authorization: Bearer <accessToken>` on subsequent calls. |
| `expiresInSeconds` | number  | TTL of the access token. When it expires the user must request a new link — **there is no refresh token**. |
| `email`            | string  | Same email the user typed. Use it to render "Logged in as guest@…". |
| `guest`            | boolean | Always `true` here. Use it to render a guest badge / upsell the user to register. |

**Errors**

| HTTP | code     | message                                  | When                                                  |
|------|----------|------------------------------------------|-------------------------------------------------------|
| 400  | `2001`   | `EMPTY_INPUT`                            | Token missing                                         |
| 401  | `20001`  | Magic link is invalid or has expired     | Bad signature, expired, malformed, wrong signer key  |
| 401  | `20002`  | Magic link has already been used         | jti is in the invalidation cache (single-use enforced) |

Show a friendly "This link no longer works — request a new one" message for both `20001` and `20002`. Don't differentiate; both mean "request a fresh link".

---

## Frontend implementation

### Routes you need

| Route                       | What it does                                                      |
|-----------------------------|-------------------------------------------------------------------|
| `/login` (or wherever)      | Hosts the email form that calls `/magic-link/request`.            |
| `/auth/magic-link`          | Reads `?token=…`, calls `/magic-link/verify`, stores access token, redirects to `/`. **This path must match `application.authentication.magic-link.callback-path` in the backend config** (default `/auth/magic-link`). |

If you want a different callback path, ask backend to update `MAGIC_LINK_CALLBACK_PATH` env var — don't hard-code an alternative in FE only, since the link inside the email comes from the backend.

### Storing the token

Because there is no refresh token, treat the guest access token as ephemeral:

- **Storage:** `sessionStorage` is the safest default (cleared on tab close). `localStorage` is fine if you want the session to survive a refresh, but be explicit in the UI.
- **Header:** add `Authorization: Bearer <accessToken>` exactly the way you do for regular logged-in users.
- **Expiry:** schedule a timer for `expiresInSeconds`. When it fires, drop the token and bounce the user back to `/login` (or pop a "Session expired — get a new link" modal).
- **Logout:** clear the storage entry client-side. **Do not** call `POST /v1/auth/logout` — that endpoint expects a refresh token claim (`rfId`) that guest tokens don't have, and it will 500.

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
          sessionStorage.setItem("accessToken", json.data.accessToken);
          sessionStorage.setItem("guestEmail", json.data.email);
          // schedule auto-clear
          setTimeout(() => {
            sessionStorage.removeItem("accessToken");
            sessionStorage.removeItem("guestEmail");
          }, json.data.expiresInSeconds * 1000);
          router.replace("/");
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

Use it to hide or stub out features that need a real `User` row (profile edit, broker registration, payments, etc.) and show an "Upgrade to a full account" CTA instead.

---

## Edge cases to handle in UI

| Scenario                                          | Suggested UX |
|---------------------------------------------------|--------------|
| User clicks the link in a different browser/device | It still works — the JWT is self-contained, no cookies/sessions tied to the originating browser. |
| User clicks the link twice                        | First click logs in. Second returns `20002` — show "already used, request a new link". |
| User waits >10 minutes before clicking            | Returns `20001` — show "link expired". |
| User wants to switch identities                   | Clear `sessionStorage`, return them to the email form. |
| User accidentally requests two links              | The older one still works until it expires; once verified it burns immediately. There is no "only-latest-wins" rule. |
| Access token expires while user is active         | Show a non-blocking toast "Session expired" and route to `/login`. |

---

## Security notes (for awareness, not action)

- The link is signed with `HS512` using `MAGIC_LINK_SIGNER_KEY` and carries `email`, `sub` (`guest:<uuid>`), `jti`, `exp` (10 min default). Tampering breaks the signature → `20001`.
- After verify, the `jti` is added to the `auth.invalidatedTokens` Redis cache (24h TTL) — that's how we enforce single-use.
- Issued guest access token is signed with the same `ACCESS_SIGNER_KEY` regular users use, so it flows through the existing `CustomJwtDecoder` / Spring Security pipeline transparently.
- We deliberately do **not** reveal whether the email is registered when `/request` is called — same response either way. Don't change that on the FE either.

---

## Backend configuration (for reference / ops)

These are set in `application.yml`; the FE doesn't read them but it helps to know what's tunable:

| Property | Default | Notes |
|---|---|---|
| `application.authentication.jwt.magic-link-signer-key` | falls back to `RESET_PASSWORD_SIGNER_KEY` | Set `MAGIC_LINK_SIGNER_KEY` in prod. |
| `application.authentication.jwt.magic-link-duration` | `600` (seconds, 10 min) | TTL of the link. |
| `application.authentication.jwt.valid-duration` | env `VALID_DURATION` | TTL of the **guest access token** (same as regular access token). |
| `application.authentication.magic-link.callback-path` | `/auth/magic-link` | Path appended to `client-url` when building the email link. Must match the FE route. |
| `application.authentication.magic-link.email-subject` | `SmartRent - Đăng nhập với liên kết` | Email subject line. |
| `application.client-url` | env `CLIENT_URL` | Base URL of the FE app. |

---

## FAQ

**Q: Can I make guest sessions persist across browser tabs?**
Yes — use `localStorage` instead of `sessionStorage`. Just be sure to honor the expiry timer; the backend won't refresh the token for you.

**Q: Can a guest become a registered user later?**
Yes — when they click "Create account" route them to the normal `/v1/users` registration flow, prefilling `email` from `sessionStorage.getItem("guestEmail")`. The two identities are completely independent on the backend.

**Q: What's the rate-limit on `/magic-link/request`?**
There isn't one yet — it goes through Brevo's rate limits only. If you see abuse during rollout, ping backend to add a per-email/per-IP throttle.

**Q: Can I use a deep link (iOS/Android) instead of a web URL?**
Yes — change `MAGIC_LINK_CALLBACK_PATH` to your app's deep-link path (e.g. `smartrent://auth/magic-link`) and `CLIENT_URL` to the scheme prefix. Both endpoints are scheme-agnostic.
