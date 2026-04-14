# Broker Feature – User-Facing Frontend Integration Guide

> **Audience:** Frontend developer building the user-facing (customer) application.
> Admin-facing guide is in `BROKER_ADMIN_FRONTEND_GUIDE.md`.

---

## 1. Business Overview

### User Journey
1. **Normal user** opens their profile page and clicks **"Register as Broker"**.
2. **Upload documents** — user uploads 4 identity images using the presigned URL flow.
3. **Submit registration** — `POST /v1/users/broker/register` with the 4 media IDs.
4. Status becomes **`PENDING`** → show "Verification in Progress" badge.
5. **Admin manually verifies** at [nangluchdxd.gov.vn](https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20).
6. **Approved** → `isBroker=true` → "✓ Verified Broker" badge everywhere.
7. **Rejected** → rejection reason shown → "Re-apply" CTA (must re-upload docs).

### Required Documents
| Slot | Field | Description |
|---|---|---|
| 1 | `cccdFrontMediaId` | CCCD (National ID) — front side |
| 2 | `cccdBackMediaId` | CCCD — back side |
| 3 | `certFrontMediaId` | Practising certificate — front side |
| 4 | `certBackMediaId` | Practising certificate — back side |

---

## 2. State Machine

```
NONE ──(upload docs + register)──▶ PENDING ──(admin approves)──▶ APPROVED
                                       │
                                  (admin rejects)
                                       │
                                       ▼
                                   REJECTED ──(re-upload + re-register)──▶ PENDING
```

---

## 3. API Contracts

### Step 1 — Upload a Document (repeat ×4)

**`POST /v1/media/upload-url`**  
Auth: Bearer token

Request:
```json
{
  "purpose": "BROKER_DOCUMENT",
  "mediaType": "IMAGE",
  "filename": "cccd-front.jpg",
  "contentType": "image/jpeg",
  "fileSize": 204800
}
```

Response:
```json
{
  "code": "999999",
  "data": {
    "mediaId": 101,
    "uploadUrl": "https://r2.example.com/bucket/users/.../broker/...jpg?X-Amz-Signature=...",
    "expiresIn": 600,
    "storageKey": "users/user-abc/broker/uuid.jpg"
  }
}
```

### Step 2 — Upload to R2 (repeat ×4)
```
PUT <uploadUrl>
Content-Type: image/jpeg
Content-Length: 204800
[binary file body]
```
> No auth header needed — the presigned URL is self-authenticating.

### Step 3 — Confirm Upload (repeat ×4)

**`POST /v1/media/{mediaId}/confirm`**  
Auth: Bearer token, Body: `{}`

Response: `{ "data": { "mediaId": 101, "status": "ACTIVE", "url": "...", ... } }`

### Step 4 — Submit Registration

**`POST /v1/users/broker/register`**  
Auth: Bearer token

Request:
```json
{
  "cccdFrontMediaId": 101,
  "cccdBackMediaId": 102,
  "certFrontMediaId": 103,
  "certBackMediaId": 104
}
```

Response 200:
```json
{
  "code": "999999",
  "data": {
    "userId": "user-abc-123",
    "isBroker": false,
    "brokerVerificationStatus": "PENDING",
    "brokerRegisteredAt": "2024-01-15T10:30:00",
    "brokerVerifiedAt": null,
    "brokerRejectionReason": null,
    "brokerVerificationSource": null,
    "cccdFrontUrl": "https://r2.example.com/...?sig=...",
    "cccdBackUrl": "https://r2.example.com/...?sig=...",
    "certFrontUrl": "https://r2.example.com/...?sig=...",
    "certBackUrl": "https://r2.example.com/...?sig=..."
  }
}
```

**Document validation errors:**
| Code | Meaning | When |
|---|---|---|
| `17003` | Document field missing | `null` mediaId sent |
| `17004` | Document media not found | Wrong mediaId or not owned by user |
| `17005` | Document not confirmed yet | Upload not confirmed before registering |
| `17006` | Document not an image | Wrong file type |

**Idempotent behavior:** PENDING or APPROVED → returns current state unchanged (no document re-processing).

---

### 3.5 GET /v1/users/broker/status

Auth: Bearer token, No body.

Response: Same shape as register response (includes document URLs).

---

### 3.6 POST /v1/listings/search — broker filter
```json
{ "isBroker": true, "page": 1, "size": 20 }
```

---

### 3.7 GET /v1/users (profile endpoint)
```json
{
  "userId": "user-abc-123",
  "isBroker": true,
  "brokerVerificationStatus": "APPROVED"
}
```

---

## 4. TypeScript Data Model

```typescript
export type BrokerVerificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface BrokerStatusResponse {
  userId: string;
  isBroker: boolean;
  brokerVerificationStatus: BrokerVerificationStatus;
  brokerRegisteredAt?: string | null;
  brokerVerifiedAt?: string | null;
  brokerRejectionReason?: string | null;
  brokerVerificationSource?: string | null;
  // Presigned document URLs (present only when docs were submitted)
  cccdFrontUrl?: string | null;
  cccdBackUrl?: string | null;
  certFrontUrl?: string | null;
  certBackUrl?: string | null;
}

export interface BrokerRegisterRequest {
  cccdFrontMediaId: number;
  cccdBackMediaId: number;
  certFrontMediaId: number;
  certBackMediaId: number;
}

export interface UploadUrlRequest {
  purpose: 'BROKER_DOCUMENT';
  mediaType: 'IMAGE';
  filename: string;
  contentType: string;
  fileSize: number;
}

export interface UploadUrlResponse {
  mediaId: number;
  uploadUrl: string;
  expiresIn: number;
  storageKey: string;
}

export interface User {
  userId: string;
  firstName: string;
  lastName: string;
  email?: string;
  avatarUrl?: string;
  isBroker?: boolean | null;
  brokerVerificationStatus?: BrokerVerificationStatus | null;
}

export interface ListingFilterPayload {
  isBroker?: boolean | null;
  page?: number;
  size?: number;
  [key: string]: unknown;
}
```

---

## 5. Full Registration Flow (React)

```typescript
// hooks/useBrokerDocumentUpload.ts
export async function uploadBrokerDocument(file: File): Promise<number> {
  // Step 1: Get presigned URL
  const urlRes = await apiClient.post<ApiResponse<UploadUrlResponse>>('/v1/media/upload-url', {
    purpose: 'BROKER_DOCUMENT',
    mediaType: 'IMAGE',
    filename: file.name,
    contentType: file.type,
    fileSize: file.size,
  });
  const { mediaId, uploadUrl } = urlRes.data.data;

  // Step 2: Upload directly to R2
  await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file,
  });

  // Step 3: Confirm upload
  await apiClient.post(`/v1/media/${mediaId}/confirm`, {});

  return mediaId;
}

// hooks/useBrokerRegistration.ts
export function useBrokerRegistration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (files: {
      cccdFront: File; cccdBack: File;
      certFront: File; certBack: File;
    }) => {
      // Upload all 4 documents in parallel
      const [cccdFrontMediaId, cccdBackMediaId, certFrontMediaId, certBackMediaId] =
        await Promise.all([
          uploadBrokerDocument(files.cccdFront),
          uploadBrokerDocument(files.cccdBack),
          uploadBrokerDocument(files.certFront),
          uploadBrokerDocument(files.certBack),
        ]);

      // Submit registration
      return apiClient
        .post<ApiResponse<BrokerStatusResponse>>('/v1/users/broker/register', {
          cccdFrontMediaId, cccdBackMediaId, certFrontMediaId, certBackMediaId,
        })
        .then(r => r.data.data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['broker-status'] });
    },
  });
}

// hooks/useBrokerStatus.ts
export function useBrokerStatus() {
  return useQuery({
    queryKey: ['broker-status'],
    queryFn: () =>
      apiClient.get<ApiResponse<BrokerStatusResponse>>('/v1/users/broker/status')
        .then(r => r.data.data),
    refetchInterval: (query) =>
      query.state.data?.brokerVerificationStatus === 'PENDING' ? 30_000 : false,
  });
}
```

---

## 6. UI Requirements

### Registration Form

| Step | What to show |
|---|---|
| Before upload | 4 file inputs labelled CCCD Front, CCCD Back, Cert Front, Cert Back |
| Uploading | Per-file progress bar / spinner |
| Upload error | Per-file error message with retry |
| All uploaded | "Submit Registration" button becomes enabled |
| Submitting | Full-form loading overlay |
| Success | "Under Review" badge; hide form |

### Document Preview (after submission)

Show thumbnail/link for each document on the profile page. URLs come from `cccdFrontUrl`, `cccdBackUrl`, `certFrontUrl`, `certBackUrl` in the status response. These are presigned and short-lived (regenerated each time status is fetched).

### Profile Status Badges

| Status | What to show |
|---|---|
| `NONE` | "Register as Broker" CTA |
| `PENDING` | "🕐 Verification in Progress"; disable re-submit |
| `APPROVED` | "✓ Verified Broker" green badge |
| `REJECTED` | Warning + `brokerRejectionReason`; "Re-apply" CTA (user must re-upload docs) |

---

## 7. UX Rules & Edge Cases

| Scenario | Behavior |
|---|---|
| Image too large | Validate client-side before upload (`fileSize` > limit); show error |
| Non-image file | Show "Only image files are accepted" before upload |
| Upload fails mid-flow | Per-document error; allow individual retry; do not disable other docs |
| Confirm fails (not uploaded) | API 400 code `17005`; show "Upload failed, please re-select the file" |
| Register when PENDING | API returns current state; show "Your application is already under review" |
| Register when APPROVED | API returns current state; show "You are already a verified broker" |
| Re-apply after rejection | Must re-select all 4 documents (new media IDs required) |
| Presigned URL expired | `cccdFrontUrl` etc. will be null or 403; refetch status to get fresh URLs |

---

## 8. Microcopy (EN / VI)

| Element | English | Vietnamese |
|---|---|---|
| CCCD front label | "National ID — Front Side" | "CCCD — Mặt trước" |
| CCCD back label | "National ID — Back Side" | "CCCD — Mặt sau" |
| Cert front label | "Practising Certificate — Front" | "Chứng chỉ hành nghề — Mặt trước" |
| Cert back label | "Practising Certificate — Back" | "Chứng chỉ hành nghề — Mặt sau" |
| Upload CTA | "Choose File" | "Chọn tệp" |
| Uploading state | "Uploading…" | "Đang tải lên…" |
| Register CTA | "Submit Application" | "Nộp đơn đăng ký" |
| Pending badge | "🕐 Verification in Progress" | "🕐 Đang xác minh" |
| Approved badge | "✓ Verified Broker" | "✓ Môi giới được xác nhận" |
| Rejected badge | "Registration Rejected" | "Đơn bị từ chối" |
| Re-apply CTA | "Re-apply with New Documents" | "Nộp lại đơn với hồ sơ mới" |

---

## 9. Backward Compatibility

Document URL fields (`cccdFrontUrl`, etc.) are absent in older backend versions. Guard:
```typescript
const hasDocs = status?.cccdFrontUrl != null;
```

---

## 10. QA Checklist

| # | Test | Pass |
|---|---|---|
| 1 | Upload 4 docs → submit → status = PENDING | [ ] |
| 2 | Missing any doc → 400 code 17003 | [ ] |
| 3 | Unconfirmed media → 400 code 17005 | [ ] |
| 4 | Wrong user's media → 404 code 17004 | [ ] |
| 5 | Register when PENDING → idempotent | [ ] |
| 6 | Register when APPROVED → idempotent | [ ] |
| 7 | Re-apply after rejection (new docs) → PENDING | [ ] |
| 8 | Status response includes presigned document URLs | [ ] |
| 9 | Broker badge shows after APPROVED | [ ] |
| 10 | `isBroker=true` filter returns only broker listings | [ ] |