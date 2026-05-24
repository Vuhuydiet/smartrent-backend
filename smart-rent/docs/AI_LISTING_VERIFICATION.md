# SmartRent AI Listing Verification

This guide documents the AI-powered listing verification (auto-moderation) system implemented across:

- Spring Boot backend: orchestration, batch scheduling, database state management
- MySQL: `listing_ai_moderation` table as the source of truth for moderation lifecycle
- Python AI server: multimodal analysis using Google Gemini 2.5 Flash via Vertex AI
- Gemini Vision: image/video content analysis, stock photo detection, consistency checking

The important rule is simple: **AI never writes to the database and never makes moderation decisions directly.** AI only analyses listing content and returns a structured score and suggested status. Spring Boot controls every state transition.

---

## Current Support

Supported now:

- Automatic batch moderation via a scheduled job (`processPendingListings`) every 5 minutes
- Manual one-off verification via `POST /v1/ai/listings/verify`
- Multimodal analysis: images downloaded and passed as binary to Gemini
- Video support: keyframes extracted using OpenCV and sent to Gemini; full-video fallback if OpenCV is unavailable
- Text-only fallback when no media is available or all downloads fail
- Self-healing: hourly job resets listings stuck in `IN_PROGRESS` for more than 30 minutes back to `PENDING`
- Parallel batch processing using Java `ForkJoinPool` (each listing runs in its own `REQUIRES_NEW` transaction to avoid deadlocks)
- AI service availability check via `GET /v1/ai/listings/service-status`

Not included intentionally:

- AI writing directly to `listing_ai_moderation` or `listings` tables
- Vector database or RAG-based content retrieval
- Real-time push notifications on moderation result (planned)
- Per-listing cache or deduplication across batch runs

---

## Verification Lifecycle

Each listing goes through the following states stored in `listing_ai_moderation.verification_status`:

```
[New listing submitted]
        |
        v
    PENDING  ──────────────────────────────────────────────────────┐
        |                                                          │
        v (Scheduler picks up batch of 20)                        │
   IN_PROGRESS                                                     │
        |                                                          │
        v (AI responds)                                            │
   ┌─────────────────────────────────────────┐                    │
   │ AI suggested_status == "APPROVED"       │──> VERIFIED        │
   │ AI suggested_status == "REJECTED"       │──> REJECTED        │
   │ AI suggested_status == "NEEDS_REVIEW"   │──> UNDER_REVIEW    │
   └─────────────────────────────────────────┘                    │
        |                                                          │
        v (on any exception)                                       │
   retry_count++ → back to PENDING ─────────────────────────────>─┘

[Self-Healing Job - runs every 1 hour]
   Find all IN_PROGRESS with updated_at < now() - 30min
        │
        v
   Reset to PENDING
```

State mapping to the primary `listings` table:

| `verification_status` | `listings.verified` | `listings.moderation_status` |
|---|---:|---|
| `VERIFIED` | `true` | `APPROVED` |
| `REJECTED` | `false` | `REJECTED` |
| `UNDER_REVIEW` | `false` | `PENDING_REVIEW` |

---

## Auto-Moderation Flow (Scheduled)

1. Spring Boot scheduler triggers every **5 minutes** (configurable via `smartrent.ai.verification.scheduler.delay`, default `300000ms`).
2. Fetches up to **20 listings** that need AI verification from `listing_ai_moderation` where `verification_status = PENDING`.
3. Marks all fetched listings as `IN_PROGRESS` in a single batch write.
4. Processes listings **in parallel** using `IntStream.parallel()` / `ForkJoinPool`. Each listing runs in its own `REQUIRES_NEW` transaction.
5. For each listing: fetches associated images/videos from the `media` table, builds the verification request, and calls the Python AI service.
6. Python AI service (Gemini) analyses content and returns a JSON response with score, suggested status, and structured reason.
7. Spring Boot maps the `suggested_status` to a `VerificationStatus` enum and persists both `listing_ai_moderation` and the parent `listings` record.
8. On failure: `retry_count` is incremented and `verification_status` is reset to `PENDING` for a future run.

---

## Manual Verification API

### Verify Using Payload

`POST /v1/ai/listings/verify`

Send a full listing payload directly to trigger AI verification without touching the database.

Request body:

```json
{
  "title": "Cho thuê căn hộ 3PN view sông Vinhomes Central Park",
  "description": "Căn hộ 3 phòng ngủ, diện tích 95m2, đầy đủ nội thất cao cấp, view sông Sài Gòn tuyệt đẹp.",
  "price": 35000000,
  "address": "208 Nguyễn Hữu Cảnh, Phường 22, Quận Bình Thạnh, TP.HCM",
  "property_type": "APARTMENT",
  "area": 95,
  "amenities": ["WIFI", "AIR_CONDITIONING", "SWIMMING_POOL", "GYM", "PARKING"],
  "images": [
    "https://example.com/room1.jpg",
    "https://example.com/room2.jpg"
  ],
  "videos": [
    { "url": "https://example.com/tour.mp4" }
  ],
  "metadata": {
    "bedrooms": 3,
    "bathrooms": 2
  }
}
```

Request fields:

| Field | Type | Required | Notes |
|---|---|---:|---|
| `title` | string | yes | Max 500 chars |
| `description` | string | yes | 10–5000 chars |
| `price` | number | yes | In VND (e.g. `5000000` = 5 triệu) |
| `address` | string | yes | Min 5 chars |
| `property_type` | string | no | `APARTMENT`, `ROOM`, `HOUSE`, `STUDIO`, `OFFICE` |
| `area` | number | no | In m² |
| `amenities` | string[] | no | List of amenity names |
| `images` | string[] | no | Public URLs; downloaded and sent as binary to Gemini |
| `videos` | object[] | no | Each with `url`, optional `thumbnailUrl`, `durationSeconds` |
| `metadata` | object | no | `bedrooms`, `bathrooms`, `floor` |

Example response:

```json
{
  "code": "999999",
  "message": "Listing verification completed successfully",
  "data": {
    "is_valid": false,
    "score": 0.35,
    "confidence": 0.95,
    "suggested_status": "NEEDS_REVIEW",
    "image_validation": {
      "is_valid": false,
      "total_images": 3,
      "valid_images": 0,
      "quality_score": 0.1,
      "issues": [
        "All images appear to be stock photos and do not depict the actual property.",
        "Image 1: view does not match described Saigon river view."
      ]
    },
    "video_validation": {
      "is_valid": true,
      "total_videos": 0,
      "valid_videos": 0,
      "quality_score": 1.0,
      "issues": []
    },
    "content_validation": {
      "is_rental_related": true,
      "category_match": true,
      "content_score": 0.9,
      "issues": []
    },
    "completeness_validation": {
      "is_complete": true,
      "completeness_score": 1.0,
      "missing_fields": [],
      "quality_issues": []
    },
    "violations": [
      {
        "category": "Media",
        "severity": "high",
        "message": "All images are stock photos and do not represent the actual property.",
        "field": ""
      }
    ],
    "suggestions": [
      {
        "category": "Media",
        "message": "Replace all stock photos with actual high-quality photos of the specific apartment.",
        "field": "",
        "priority": "high"
      }
    ],
    "reason": {
      "blurriness_issue": false,
      "missing_fields": [],
      "inconsistent_info": true,
      "watermark_or_phone": false,
      "stock_photo": true,
      "details": "All provided images are stock photos inconsistent with the described property and location."
    },
    "violation_codes": ["SCAM", "INCONSISTENT_INFO"],
    "model_used": "gemini-2.5-flash",
    "processing_time_seconds": 23.86,
    "verification_timestamp": "2026-05-13T23:16:52.599444"
  }
}
```

### Check AI Service Status

`GET /v1/ai/listings/service-status`

Returns whether the Python AI service is reachable.

```json
{
  "code": "999999",
  "message": "AI service status retrieved successfully",
  "data": {
    "available": true,
    "checked_at": "2026-05-13T23:17:00"
  }
}
```

---

## AI Response Fields

### `suggested_status` values

| Value | Backend action |
|---|---|
| `APPROVED` | `verification_status = VERIFIED`, `listings.verified = true`, `moderation_status = APPROVED` |
| `REJECTED` | `verification_status = REJECTED`, `listings.verified = false`, `moderation_status = REJECTED` |
| `NEEDS_REVIEW` | `verification_status = UNDER_REVIEW`, `listings.verified = false`, `moderation_status = PENDING_REVIEW` |

### `violation_codes` values

| Code | Meaning |
|---|---|
| `SCAM` | Content or media suggests fraudulent intent |
| `INCONSISTENT_INFO` | Text description does not match images/videos |
| `STOCK_PHOTO` | Images are stock photos, not actual property photos |
| `CONTACT_IN_LISTING` | Phone number or contact info embedded in text or image |
| `BLURRY_IMAGE` | Images are too blurry to be useful |
| `MISSING_MEDIA` | No images or videos provided |
| `PRICE_ANOMALY` | Price is unrealistically low or high |

### Score thresholds (AI model guidance)

| Score range | Typical result |
|---|---|
| `>= 0.75` | `APPROVED` — high quality, consistent media and content |
| `0.50 – 0.74` | `NEEDS_REVIEW` — minor issues, needs human check |
| `< 0.50` | `REJECTED` — significant violations or missing media |

These thresholds are encoded in the Gemini system prompt and are not enforced by Spring Boot. Spring Boot relies entirely on `suggested_status`.

---

## Python AI Server Contract

The AI server endpoint called by Spring Boot:

`POST /ai/verify-listing` (on `http://localhost:8000`)

Input:

- `title`, `description`, `price`, `address`, `property_type`, `area`
- `amenities`: list of strings
- `images`: list of public image URLs (downloaded server-side as binary PIL images)
- `videos`: list of `{ url, thumbnailUrl, duration }` objects

Processing inside Python:

1. All image URLs are downloaded in parallel using `httpx`.
2. Video URLs are downloaded; OpenCV extracts keyframes as PIL images. Falls back to sending the full video binary if OpenCV fails.
3. If no media downloads succeed, falls back to text-only analysis.
4. A multimodal prompt is sent to Gemini 2.5 Flash including all binary images/frames alongside the structured text.
5. Response is parsed and sanitised (field types validated) before returning.

AI prompt contract:

- Gemini is instructed to return only valid JSON matching the schema.
- Never generate SQL.
- Never invent property details not present in the input.
- Always check visual content against the text description.
- Score between `0.0` (fraudulent/empty) and `1.0` (legitimate, high quality).

---

## Database Schema

```sql
CREATE TABLE listing_ai_moderation (
    listing_id         BIGINT PRIMARY KEY,
    ai_score           DECIMAL(4,3)  DEFAULT NULL COMMENT 'AI moderation score (0.0 to 1.0)',
    ai_reason          JSON          DEFAULT NULL COMMENT 'Full AI response JSON for admin review',
    manual_override    BOOLEAN       DEFAULT FALSE COMMENT 'Whether admin has manually overridden AI decision',
    verification_status VARCHAR(20)  DEFAULT 'PENDING'
                       COMMENT 'PENDING | IN_PROGRESS | VERIFIED | REJECTED | UNDER_REVIEW',
    retry_count        INT           DEFAULT 0,
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_ai_moderation_listing
        FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE
);

CREATE INDEX idx_listing_ai_moderation_status
    ON listing_ai_moderation (verification_status, manual_override);
```

---

## Configuration

```yaml
smartrent:
  ai:
    verification:
      url: http://localhost:8000          # Python AI service base URL
      timeout-seconds: 15                # FeignClient / RestTemplate timeout
      scheduler:
        delay: 300000                    # ms between batch runs (default 5 min)
```

Self-healing job configuration (hardcoded):

- Runs every **1 hour** (`fixedDelayString = "3600000"`).
- Resets any `IN_PROGRESS` record whose `updated_at` is older than **30 minutes**.

---

## Concurrency Design

- Outer scheduler method (`processPendingListings`) has **no `@Transactional`** to avoid holding a connection across the parallel stream.
- Each listing is processed in `processSingleListing` annotated with `@Transactional(propagation = REQUIRES_NEW)` so failures are isolated.
- The self-healing recovery job uses `@Transactional` because it is a single bulk UPDATE.
- This design eliminates the MySQL deadlock (Error 1213) that occurred when the outer transaction held row locks while inner parallel threads tried to acquire the same locks.

---

## Error Handling

| Exception | Backend action |
|---|---|
| `ResourceAccessException` (timeout) | Throws `AI_SERVICE_TIMEOUT`. Scheduler resets listing to `PENDING`. |
| `RestClientException` | Throws `AI_SERVICE_UNAVAILABLE`. Scheduler resets listing to `PENDING`. |
| Any other exception | Throws `AI_SERVICE_ERROR`. Scheduler increments `retry_count` and resets to `PENDING`. |
| Null response from AI | Throws `AI_SERVICE_INVALID_RESPONSE`. |

---

## Why This Fits A Graduation Project

This design is realistic because it uses tools already in the stack:

- Spring Boot for scheduling, state machine, and transaction management
- Gemini 2.5 Flash for multimodal analysis without running a local model
- MySQL for all persistent state — no extra infrastructure
- Python service as a thin wrapper around the Gemini SDK
- Self-healing pattern demonstrates production-grade resilience thinking

It gives a modern "AI-powered content moderation" demo while staying maintainable and explainable in a thesis defence.

---

## Roadmap

1. Build Admin UI panel to display `ai_reason` JSON and allow manual override of `UNDER_REVIEW` listings.
2. Add webhook or WebSocket notification to listing owner when moderation result changes.
3. Tune Gemini system prompt thresholds based on real rejection/approval data.
4. Add per-listing moderation history log for audit trail.
5. Resolve `HHH90000026` Hibernate dialect deprecation warning by migrating to `MySQLDialect`.
6. Add Prometheus/Micrometer metrics for batch size, processing latency, and retry rate.
