# Recommendation API - Frontend Integration Guide

> Audience: Frontend developers integrating listing recommendations from SmartRent backend.
> Source of truth: `RecommendationController` + `RecommendationServiceImpl` in backend.

---

## 1. Overview

Backend exposes 2 recommendation endpoints:

1. Similar listings for a detail page (`/similar/{listingId}`)
2. Personalized feed for logged-in users (`/personalized`)

Both endpoints return:

- Wrapper: `ApiResponse<T>`
- Payload: `RecommendationResponse`
- Listing items: full `ListingResponse` objects (listing-card-ready, not only IDs)

---

## 2. Base URL And Auth

- Base path: `/v1/recommendations`
- Public endpoint:
  - `GET /v1/recommendations/similar/{listingId}`
- Auth-required endpoint:
  - `GET /v1/recommendations/personalized`

Auth header format:

```http
Authorization: Bearer <access_token>
```

Notes:

- `similar` is publicly accessible. If token is present, backend can personalize lightly.
- `personalized` is protected by security config in production.
- Controller supports optional `userId` query param for test convenience, but FE production should always send JWT.

---

## 3. API Contracts

### 3.1 GET /v1/recommendations/similar/{listingId}

Purpose:

- Return listings similar to target listing.
- Anonymous users: content-based similar results.
- Authenticated users: hybrid personalization overlay.

Query params:

- `topN` (optional, default `8`)

Example request:

```http
GET /v1/recommendations/similar/123?topN=8
```

Example response:

```json
{
  "code": "200",
  "message": "Successfully retrieved similar listings",
  "data": {
    "listings": [
      {
        "listingId": 456,
        "title": "Cho thue can ho 2PN quan 7",
        "description": "Can ho view song, noi that co ban, gan Lotte Mart.",
        "price": 8500000,
        "priceUnit": "MONTH",
        "area": 68,
        "bedrooms": 2,
        "bathrooms": 2,
        "productType": "APARTMENT",
        "listingType": "RENT",
        "verified": true,
        "expired": false,
        "postDate": "2026-04-01T09:20:00",
        "expiryDate": "2026-05-01T09:20:00",
        "pushedAt": "2026-04-12T08:30:00",
        "vipType": "GOLD",
        "address": {
          "fullAddress": "Nguyen Huu Tho, Tan Hung, Quan 7, TP Ho Chi Minh",
          "fullNewAddress": "Nguyen Huu Tho, Tan Hung, TP Ho Chi Minh",
          "latitude": 10.7292,
          "longitude": 106.7214
        },
        "media": [
          {
            "mediaId": 9001,
            "mediaType": "IMAGE",
            "url": "https://cdn.smartrent.vn/listings/456/cover.jpg",
            "isPrimary": true,
            "sortOrder": 1
          }
        ],
        "recommendationScore": 0.81,
        "personalizationScore": 0.35,
        "similarityScore": 0.76
      }
    ],
    "mode": "similar_personalized",
    "totalReturned": 1,
    "coldStart": false
  }
}
```

Possible `data.mode` values:

- `similar`
- `similar_personalized`
- `similar_fallback`

---

### 3.2 GET /v1/recommendations/personalized

Purpose:

- Return personalized listing feed based on user interactions.
- Interaction signals include saved listings, phone clicks, and recently viewed listings.

Query params:

- `topN` (optional, default `20`)
- `userId` (optional, test convenience only in controller logic)

Example request (production):

```http
GET /v1/recommendations/personalized?topN=20
Authorization: Bearer <access_token>
```

Example response:

```json
{
  "code": "200",
  "message": "Successfully retrieved personalized feed",
  "data": {
    "listings": [
      {
        "listingId": 789,
        "title": "Phong tro gan trung tam",
        "description": "Phong moi, co gac, gio giac tu do, an ninh.",
        "price": 4200000,
        "priceUnit": "MONTH",
        "area": 28,
        "bedrooms": 1,
        "bathrooms": 1,
        "productType": "ROOM",
        "listingType": "RENT",
        "verified": true,
        "expired": false,
        "postDate": "2026-04-10T11:45:00",
        "expiryDate": "2026-05-10T11:45:00",
        "pushedAt": "2026-04-15T07:15:00",
        "vipType": "DIAMOND",
        "address": {
          "fullAddress": "Le Van Sy, Ward 13, Phu Nhuan, TP Ho Chi Minh",
          "fullNewAddress": "Le Van Sy, Ward 13, TP Ho Chi Minh",
          "latitude": 10.7951,
          "longitude": 106.6776
        },
        "media": [
          {
            "mediaId": 9101,
            "mediaType": "IMAGE",
            "url": "https://cdn.smartrent.vn/listings/789/cover.jpg",
            "isPrimary": true,
            "sortOrder": 1
          }
        ],
        "recommendationScore": 0.89,
        "personalizationScore": 0.62,
        "similarityScore": 0.54
      }
    ],
    "mode": "personalized",
    "totalReturned": 1,
    "coldStart": false
  }
}
```

Possible `data.mode` values:

- `personalized`
- `cold_start`

When does `mode='cold_start'` (and `coldStart=true`) happen:

- User has no interaction history.
- AI recommendation service fails and backend falls back to trending/global candidates.

---

## 4. Response Data Model (TypeScript)

```ts
export interface ApiResponse<T> {
  code: string;
  message: string;
  data?: T;
}

export interface RecommendationResponse {
  listings: ListingResponse[];
  mode: 'similar' | 'similar_personalized' | 'similar_fallback' | 'personalized' | 'cold_start';
  totalReturned: number;
  coldStart: boolean;
}

export interface ListingResponse {
  listingId: number;
  title: string;
  description?: string;
  price?: number;
  priceUnit?: string;
  area?: number;
  bedrooms?: number;
  bathrooms?: number;
  productType?: string;
  listingType?: string;
  verified?: boolean;
  expired?: boolean;
  postDate?: string;
  expiryDate?: string;
  pushedAt?: string;
  vipType?: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND' | string;
  address?: {
    fullAddress?: string;
    fullNewAddress?: string;
    latitude?: number;
    longitude?: number;
  };
  media?: Array<{
    mediaId?: number;
    mediaType?: 'IMAGE' | 'VIDEO' | string;
    url?: string;
    isPrimary?: boolean;
    sortOrder?: number;
  }>;
  recommendationScore?: number;
  personalizationScore?: number;
  similarityScore?: number;
  // Backend returns full ListingResponse; fields above are the common listing card subset.
  [key: string]: unknown;
}
```

---

## 5. Error Handling For FE

### 5.1 401 Unauthenticated

Mostly for `/personalized` when token is missing/invalid:

```json
{
  "code": "5001",
  "message": "Unauthenticated"
}
```

FE handling recommendation:

1. If personalized request returns 401, fallback to calling `/similar/{listingId}` on detail page or hide personalized section on home page.
2. Optionally show login CTA.

### 5.2 400 Bad Request

Controller-level validation for personalized request can return:

```json
{
  "code": "400",
  "message": "User ID must be provided via authentication or userId parameter"
}
```

This is mainly for test mode without JWT and without `userId`.

---

## 6. FE Integration Recommendations

### 6.1 Detail Page (Similar Listings)

- Trigger when listing detail has loaded (`listingId` known).
- Use `topN=6..10` depending on UI slots.
- If call fails, render section hidden or fallback to existing "related listings" source.

### 6.2 Home/Explore (Personalized Feed)

- Call only when user is logged in.
- Typical `topN=10..20`.
- Cache per user session.
- On 401, skip retry loop and gracefully degrade.

### 6.3 Score Usage In UI

- `recommendationScore`: final ranking score after backend hybrid ranking (best for sorting display order).
- `personalizationScore`: collaborative filtering signal.
- `similarityScore`: content-based signal.

Recommended display:

- Sort by backend return order (already ranked).
- Do not hard-filter by score unless product requirement explicitly asks.

---

## 7. React Query + Axios Example

```ts
import axios from 'axios';
import { useQuery } from '@tanstack/react-query';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
});

export interface ApiResponse<T> {
  code: string;
  message: string;
  data?: T;
}

export interface RecommendationResponse {
  listings: ListingResponse[];
  mode: 'similar' | 'similar_personalized' | 'similar_fallback' | 'personalized' | 'cold_start';
  totalReturned: number;
  coldStart: boolean;
}

export interface ListingResponse {
  listingId: number;
  title: string;
  description?: string;
  price?: number;
  priceUnit?: string;
  area?: number;
  bedrooms?: number;
  bathrooms?: number;
  productType?: string;
  vipType?: 'NORMAL' | 'SILVER' | 'GOLD' | 'DIAMOND' | string;
  verified?: boolean;
  expired?: boolean;
  postDate?: string;
  expiryDate?: string;
  pushedAt?: string;
  address?: {
    fullAddress?: string;
    fullNewAddress?: string;
    latitude?: number;
    longitude?: number;
  };
  media?: Array<{
    mediaId?: number;
    mediaType?: 'IMAGE' | 'VIDEO' | string;
    url?: string;
    isPrimary?: boolean;
    sortOrder?: number;
  }>;
  recommendationScore?: number;
  personalizationScore?: number;
  similarityScore?: number;
}

export function useSimilarListings(listingId: number, topN = 8, token?: string) {
  return useQuery({
    queryKey: ['recommendations', 'similar', listingId, topN, !!token],
    queryFn: async () => {
      const res = await api.get<ApiResponse<RecommendationResponse>>(
        `/v1/recommendations/similar/${listingId}`,
        {
          params: { topN },
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        }
      );
      return res.data.data;
    },
    enabled: !!listingId,
  });
}

export function usePersonalizedFeed(topN = 20, token?: string) {
  return useQuery({
    queryKey: ['recommendations', 'personalized', topN],
    queryFn: async () => {
      const res = await api.get<ApiResponse<RecommendationResponse>>(
        '/v1/recommendations/personalized',
        {
          params: { topN },
          headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        }
      );
      return res.data.data;
    },
    enabled: !!token,
    retry: (count, error: any) => {
      if (error?.response?.status === 401) return false;
      return count < 2;
    },
  });
}
```

---

## 8. Quick QA Checklist

1. Anonymous user can call `/similar/{listingId}` successfully.
2. Logged-in user can call both endpoints successfully.
3. `/personalized` without token returns 401 in production config.
4. FE handles `coldStart=true` without special-case crash.
5. FE uses returned order directly and does not reorder unexpectedly.

---

## 9. Backend References

- Controller: `src/main/java/com/smartrent/controller/RecommendationController.java`
- Service logic: `src/main/java/com/smartrent/service/recommendation/impl/RecommendationServiceImpl.java`
- Response DTO: `src/main/java/com/smartrent/dto/response/RecommendationResponse.java`
- Security public paths: `src/main/resources/application.yml`
