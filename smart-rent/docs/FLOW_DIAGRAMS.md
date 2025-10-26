# SmartRent Flow Diagrams

Visual representation of key integration flows.

---

## 1. Database Migration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    DATABASE MIGRATION FLOW                       │
└─────────────────────────────────────────────────────────────────┘

Step 1: Create Migration Files
┌──────────────────────────────────────────────────────────────┐
│  V1__Create_provinces_table.sql                              │
│  V2__Create_districts_table.sql                              │
│  V3__Create_wards_table.sql                                  │
│  V4__Create_streets_table.sql                                │
│  V5__Create_addresses_table.sql                              │
│  V6__Add_address_to_listings.sql                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
Step 2: Run Flyway Migration
┌──────────────────────────────────────────────────────────────┐
│  $ ./gradlew flywayMigrate                                   │
└──────────────────────────────────────────────────────────────┘
                            ↓
Step 3: Verify
┌──────────────────────────────────────────────────────────────┐
│  $ ./gradlew flywayInfo                                      │
│                                                               │
│  ✓ V1 - provinces  [Success]                                 │
│  ✓ V2 - districts  [Success]                                 │
│  ✓ V3 - wards      [Success]                                 │
│  ✓ V4 - streets    [Success]                                 │
│  ✓ V5 - addresses  [Success]                                 │
│  ✓ V6 - listings   [Success]                                 │
└──────────────────────────────────────────────────────────────┘
                            ↓
                    ✅ MIGRATION COMPLETE

Database Structure:
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  ┌──────────────┐                                           │
│  │  provinces   │                                           │
│  │  - id        │                                           │
│  │  - name      │                                           │
│  │  - code      │                                           │
│  └──────┬───────┘                                           │
│         │ 1:N                                               │
│         ↓                                                    │
│  ┌──────────────┐                                           │
│  │  districts   │                                           │
│  │  - id        │                                           │
│  │  - province_id                                           │
│  └──────┬───────┘                                           │
│         │ 1:N                                               │
│         ↓                                                    │
│  ┌──────────────┐                                           │
│  │    wards     │                                           │
│  │  - id        │                                           │
│  │  - district_id                                           │
│  └──────┬───────┘                                           │
│         │ 1:N                                               │
│         ↓                                                    │
│  ┌──────────────┐                                           │
│  │   streets    │                                           │
│  │  - id        │                                           │
│  │  - ward_id   │                                           │
│  └──────┬───────┘                                           │
│         │ 1:N                                               │
│         ↓                                                    │
│  ┌──────────────┐        ┌──────────────┐                  │
│  │  addresses   │───────▶│   listings   │                  │
│  │  - id        │   1:N  │  - id        │                  │
│  │  - street_id │        │  - address_id│                  │
│  └──────────────┘        └──────────────┘                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Address Selection Flow (Frontend)

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADDRESS SELECTION FLOW                        │
└─────────────────────────────────────────────────────────────────┘

User Opens Form
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 1: Load Provinces                                      │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ GET /v1/addresses/provinces                             │ │
│ │                                                          │ │
│ │ Response:                                                │ │
│ │ [                                                        │ │
│ │   { id: 1, name: "Hà Nội" },                            │ │
│ │   { id: 2, name: "Hồ Chí Minh" },                       │ │
│ │   ...                                                    │ │
│ │ ]                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📋 Display: <select> Province Dropdown                      │
└─────────────────────────────────────────────────────────────┘
      │
      │ User selects "Hà Nội" (id: 1)
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Load Districts                                      │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ GET /v1/addresses/provinces/1/districts                 │ │
│ │                                                          │ │
│ │ Response:                                                │ │
│ │ [                                                        │ │
│ │   { id: 1, name: "Ba Đình", provinceId: 1 },            │ │
│ │   { id: 2, name: "Hoàn Kiếm", provinceId: 1 },          │ │
│ │   ...                                                    │ │
│ │ ]                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📋 Display: <select> District Dropdown                      │
└─────────────────────────────────────────────────────────────┘
      │
      │ User selects "Ba Đình" (id: 1)
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 3: Load Wards                                          │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ GET /v1/addresses/districts/1/wards                     │ │
│ │                                                          │ │
│ │ Response:                                                │ │
│ │ [                                                        │ │
│ │   { id: 1, name: "Phường Phúc Xá", districtId: 1 },     │ │
│ │   { id: 2, name: "Phường Trúc Bạch", districtId: 1 },   │ │
│ │   ...                                                    │ │
│ │ ]                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📋 Display: <select> Ward Dropdown                          │
└─────────────────────────────────────────────────────────────┘
      │
      │ User selects "Phường Phúc Xá" (id: 1)
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 4: Load Streets                                        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ GET /v1/addresses/wards/1/streets                       │ │
│ │                                                          │ │
│ │ Response:                                                │ │
│ │ [                                                        │ │
│ │   { id: 1, name: "Nguyễn Trãi", wardId: 1 },            │ │
│ │   { id: 2, name: "Kim Mã", wardId: 1 },                 │ │
│ │   ...                                                    │ │
│ │ ]                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📋 Display: <select> Street Dropdown                        │
└─────────────────────────────────────────────────────────────┘
      │
      │ User selects "Nguyễn Trãi" (id: 1)
      │ User enters street number: "123"
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 5: Generate Full Address                              │
│                                                              │
│ Address Object:                                             │
│ {                                                            │
│   streetNumber: "123",                                      │
│   streetId: 1,                                              │
│   wardId: 1,                                                │
│   districtId: 1,                                            │
│   provinceId: 1,                                            │
│   fullAddress: "123 Nguyễn Trãi, Phường Phúc Xá,           │
│                 Quận Ba Đình, Thành phố Hà Nội"            │
│ }                                                            │
│                                                              │
│ 📋 Display: Full Address Preview                            │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
✅ ADDRESS SELECTION COMPLETE
```

---

## 3. Create Listing with Address Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  CREATE LISTING WITH ADDRESS                     │
└─────────────────────────────────────────────────────────────────┘

Frontend Form
┌─────────────────────────────────────────────────────────────┐
│  📝 Listing Details                                         │
│  ├─ Title: "Beautiful Apartment"                            │
│  ├─ Description: "Modern 2BR apartment..."                  │
│  ├─ Type: RENT                                              │
│  ├─ Price: 12,000,000 VND/month                             │
│  ├─ Area: 78.5 m²                                           │
│  ├─ Bedrooms: 2                                             │
│  └─ Bathrooms: 1                                            │
│                                                              │
│  📍 Address (from AddressSelector)                          │
│  ├─ Province: Hà Nội (id: 1)                                │
│  ├─ District: Ba Đình (id: 1)                               │
│  ├─ Ward: Phường Phúc Xá (id: 1)                            │
│  ├─ Street: Nguyễn Trãi (id: 1)                             │
│  ├─ Number: 123                                             │
│  └─ Full: "123 Nguyễn Trãi, Phường Phúc Xá..."             │
└─────────────────────────────────────────────────────────────┘
      │
      │ User clicks "Create Listing"
      ↓
┌─────────────────────────────────────────────────────────────┐
│ POST /v1/listings                                           │
│ Authorization: Bearer <JWT_TOKEN>                           │
│                                                              │
│ Request Body:                                                │
│ {                                                            │
│   "title": "Beautiful Apartment",                           │
│   "description": "Modern 2BR apartment...",                 │
│   "listingType": "RENT",                                    │
│   "vipType": "NORMAL",                                      │
│   "productType": "APARTMENT",                               │
│   "price": 12000000,                                        │
│   "priceUnit": "MONTH",                                     │
│   "area": 78.5,                                             │
│   "bedrooms": 2,                                            │
│   "bathrooms": 1,                                           │
│   "address": {                                              │
│     "streetNumber": "123",                                  │
│     "streetId": 1,                                          │
│     "wardId": 1,                                            │
│     "districtId": 1,                                        │
│     "provinceId": 1,                                        │
│     "fullAddress": "123 Nguyễn Trãi, ...",                 │
│     "latitude": 21.028511,                                  │
│     "longitude": 105.804817                                 │
│   }                                                          │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Backend Processing (Transactional)                          │
│                                                              │
│ BEGIN TRANSACTION;                                          │
│                                                              │
│ 1. Validate Request Data                                    │
│    ├─ Check required fields                                 │
│    ├─ Validate address IDs exist                            │
│    └─ Check user authorization                              │
│                                                              │
│ 2. Create Address Record                                    │
│    ├─ INSERT INTO addresses                                 │
│    ├─ street_number = "123"                                 │
│    ├─ street_id = 1                                         │
│    ├─ ward_id = 1                                           │
│    ├─ district_id = 1                                       │
│    ├─ province_id = 1                                       │
│    ├─ full_address = "123 Nguyễn Trãi..."                  │
│    └─ Returns: address_id = 456                             │
│                                                              │
│ 3. Create Listing Record                                    │
│    ├─ INSERT INTO listings                                  │
│    ├─ title = "Beautiful Apartment"                         │
│    ├─ address_id = 456  ← Links to address                 │
│    ├─ price = 12000000                                      │
│    └─ Returns: listing_id = 789                             │
│                                                              │
│ COMMIT;                                                     │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Response: 200 OK                                            │
│                                                              │
│ {                                                            │
│   "code": "999999",                                         │
│   "message": "Listing created successfully",                │
│   "data": {                                                 │
│     "listingId": 789,                                       │
│     "title": "Beautiful Apartment",                         │
│     "addressId": 456,                                       │
│     "price": 12000000,                                      │
│     "priceUnit": "MONTH",                                   │
│     "createdAt": "2025-01-15T10:30:00Z"                     │
│   }                                                          │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Show Success Message                              │
│                                                              │
│  ✅ Listing Created Successfully!                           │
│  📋 Listing ID: 789                                         │
│  📍 Address ID: 456                                         │
│                                                              │
│  [Continue to Upload Photos] [View Listing]                │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
✅ LISTING CREATION COMPLETE
```

---

## 4. Image Upload Flow (3-Step Process)

```
┌─────────────────────────────────────────────────────────────────┐
│                     IMAGE UPLOAD FLOW                            │
└─────────────────────────────────────────────────────────────────┘

User Selects Images
┌─────────────────────────────────────────────────────────────┐
│ 📁 Selected Files:                                          │
│  ├─ apartment-living-room.jpg (2.1 MB)                      │
│  ├─ apartment-bedroom.jpg (1.8 MB)                          │
│  └─ apartment-kitchen.jpg (2.3 MB)                          │
└─────────────────────────────────────────────────────────────┘
      │
      │ User clicks "Upload"
      ↓

┌─────────────────────────────────────────────────────────────┐
│ STEP 1: Generate Upload URL                                │
│                                                              │
│ For each file, send request:                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ POST /v1/media/upload-url                               │ │
│ │ Authorization: Bearer <JWT_TOKEN>                       │ │
│ │                                                          │ │
│ │ Request:                                                 │ │
│ │ {                                                        │ │
│ │   "mediaType": "IMAGE",                                 │ │
│ │   "filename": "apartment-living-room.jpg",              │ │
│ │   "contentType": "image/jpeg",                          │ │
│ │   "fileSize": 2097152,  // bytes                        │ │
│ │   "listingId": 789,                                     │ │
│ │   "title": "Living Room",                               │ │
│ │   "isPrimary": true,                                    │ │
│ │   "sortOrder": 0                                        │ │
│ │ }                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Backend: Generate Pre-signed URL                            │
│                                                              │
│ 1. Validate file info                                       │
│    ├─ Check file type (JPEG/PNG/WebP)                       │
│    ├─ Check file size (max 100MB)                           │
│    └─ Verify listing ownership                              │
│                                                              │
│ 2. Create Media record (status: PENDING)                    │
│    ├─ INSERT INTO media                                     │
│    ├─ listing_id = 789                                      │
│    ├─ user_id = current_user                                │
│    ├─ status = "PENDING"                                    │
│    └─ Returns: media_id = 1001                              │
│                                                              │
│ 3. Generate R2 Pre-signed URL                               │
│    ├─ storage_key = "media/2025/01/15/uuid-filename.jpg"   │
│    ├─ expires_in = 1800 seconds (30 min)                    │
│    └─ upload_url = "https://r2.cloudflare.com/..."         │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Response:                                                    │
│ {                                                            │
│   "code": "999999",                                         │
│   "message": "Upload URL generated...",                     │
│   "data": {                                                 │
│     "mediaId": 1001,                                        │
│     "uploadUrl": "https://r2.cloudflare.com/...",          │
│     "expiresIn": 1800,                                      │
│     "storageKey": "media/2025/01/15/uuid-filename.jpg"     │
│   }                                                          │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
      │
      ↓

┌─────────────────────────────────────────────────────────────┐
│ STEP 2: Upload File Directly to R2                         │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ PUT https://r2.cloudflare.com/bucket/...               │ │
│ │ Content-Type: image/jpeg                                │ │
│ │                                                          │ │
│ │ Body: <binary file data>                                │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📤 Uploading... [████████████████] 100%                     │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Cloudflare R2 Storage                                       │
│                                                              │
│ ✅ File Stored Successfully                                 │
│ 📁 Location: media/2025/01/15/uuid-filename.jpg            │
│ 🔗 Public URL: https://cdn.smartrent.com/...               │
└─────────────────────────────────────────────────────────────┘
      │
      ↓

┌─────────────────────────────────────────────────────────────┐
│ STEP 3: Confirm Upload                                     │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ POST /v1/media/1001/confirm                             │ │
│ │ Authorization: Bearer <JWT_TOKEN>                       │ │
│ │                                                          │ │
│ │ Request:                                                 │ │
│ │ {                                                        │ │
│ │   "contentType": "image/jpeg"                           │ │
│ │ }                                                        │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Backend: Confirm Upload                                     │
│                                                              │
│ 1. Verify ownership                                         │
│    └─ Check media belongs to current user                   │
│                                                              │
│ 2. Update Media record                                      │
│    ├─ UPDATE media SET status = "ACTIVE"                    │
│    ├─ upload_confirmed = true                               │
│    ├─ url = "https://cdn.smartrent.com/..."                │
│    └─ updated_at = NOW()                                    │
│                                                              │
│ 3. Generate thumbnail (async)                               │
│    └─ Queue background job for thumbnail generation         │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Response:                                                    │
│ {                                                            │
│   "code": "999999",                                         │
│   "message": "Upload confirmed successfully",               │
│   "data": {                                                 │
│     "mediaId": 1001,                                        │
│     "listingId": 789,                                       │
│     "mediaType": "IMAGE",                                   │
│     "status": "ACTIVE",                                     │
│     "url": "https://cdn.smartrent.com/...",                │
│     "title": "Living Room",                                 │
│     "isPrimary": true,                                      │
│     "sortOrder": 0,                                         │
│     "uploadConfirmed": true,                                │
│     "createdAt": "2025-01-15T10:30:00Z"                     │
│   }                                                          │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ Frontend: Display Success                                   │
│                                                              │
│  ✅ apartment-living-room.jpg uploaded                      │
│  ✅ apartment-bedroom.jpg uploaded                          │
│  ✅ apartment-kitchen.jpg uploaded                          │
│                                                              │
│  All 3 images uploaded successfully!                        │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
✅ IMAGE UPLOAD COMPLETE
```

---

## 5. Complete Flow: Create Listing + Upload Images

```
┌─────────────────────────────────────────────────────────────────┐
│            COMPLETE LISTING CREATION FLOW                        │
└─────────────────────────────────────────────────────────────────┘

START
  │
  ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: Fill Listing Details                               │
│                                                              │
│ User fills:                                                 │
│  • Title, Description                                       │
│  • Type, Price, Area                                        │
│  • Bedrooms, Bathrooms                                      │
│                                                              │
│ [Continue] ─────────────────────────────────────────────────▶│
└─────────────────────────────────────────────────────────────┘
  │
  ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: Select Address                                     │
│                                                              │
│ User selects:                                               │
│  1. Province  ──GET /v1/addresses/provinces                │
│  2. District  ──GET /v1/addresses/provinces/{id}/districts │
│  3. Ward      ──GET /v1/addresses/districts/{id}/wards     │
│  4. Street    ──GET /v1/addresses/wards/{id}/streets       │
│  5. Number    ──Manual input                                │
│                                                              │
│ Full address auto-generated                                 │
│                                                              │
│ [Continue] ─────────────────────────────────────────────────▶│
└─────────────────────────────────────────────────────────────┘
  │
  ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 3: Create Listing                                     │
│                                                              │
│ POST /v1/listings                                           │
│ {                                                            │
│   ...listing details,                                       │
│   address: { ...address details }                           │
│ }                                                            │
│                                                              │
│ Backend:                                                    │
│  ✓ Create Address record                                   │
│  ✓ Create Listing record (linked to address)               │
│                                                              │
│ Response: { listingId: 789, addressId: 456 }               │
│                                                              │
│ [Continue to Photos] ───────────────────────────────────────▶│
└─────────────────────────────────────────────────────────────┘
  │
  ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 4: Upload Images                                      │
│                                                              │
│ For each selected image file:                               │
│                                                              │
│ 4.1) POST /v1/media/upload-url                             │
│      └─ Get pre-signed URL + mediaId                        │
│                                                              │
│ 4.2) PUT <pre-signed-url>                                  │
│      └─ Upload file directly to R2                          │
│                                                              │
│ 4.3) POST /v1/media/{mediaId}/confirm                      │
│      └─ Activate media record                               │
│                                                              │
│ All images uploaded and confirmed                           │
│                                                              │
│ [Finish] ───────────────────────────────────────────────────▶│
└─────────────────────────────────────────────────────────────┘
  │
  ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 5: Success!                                           │
│                                                              │
│  ✅ Listing created (ID: 789)                               │
│  ✅ Address saved (ID: 456)                                 │
│  ✅ 3 images uploaded                                       │
│                                                              │
│  Full Address:                                              │
│   123 Nguyễn Trãi, Phường Phúc Xá,                         │
│   Quận Ba Đình, Thành phố Hà Nội                           │
│                                                              │
│  [View Listing] [Create Another]                           │
└─────────────────────────────────────────────────────────────┘
  │
  ↓
END
```

---

## 6. Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       DATA FLOW OVERVIEW                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐                              ┌─────────────┐
│   Browser   │                              │   Backend   │
│  (Frontend) │                              │   (Spring)  │
└──────┬──────┘                              └──────┬──────┘
       │                                            │
       │  1. Load Provinces                         │
       │ ──────────────────────────────────────────▶│
       │    GET /v1/addresses/provinces             │
       │                                            │
       │◀───────────────────────────────────────────│
       │  [{ id: 1, name: "Hà Nội" }, ...]          │
       │                                            │
       │  2. Load Districts (Province=1)            │
       │ ──────────────────────────────────────────▶│
       │    GET /v1/addresses/provinces/1/districts │
       │                                            │
       │◀───────────────────────────────────────────│
       │  [{ id: 1, name: "Ba Đình" }, ...]         │
       │                                            │
       │  3. Load Wards (District=1)                │
       │ ──────────────────────────────────────────▶│
       │    GET /v1/addresses/districts/1/wards     │
       │                                            │
       │◀───────────────────────────────────────────│
       │  [{ id: 1, name: "Phường Phúc Xá" }, ...]  │
       │                                            │
       │  4. Load Streets (Ward=1)                  │
       │ ──────────────────────────────────────────▶│
       │    GET /v1/addresses/wards/1/streets       │
       │                                            │
       │◀───────────────────────────────────────────│
       │  [{ id: 1, name: "Nguyễn Trãi" }, ...]     │
       │                                            │
       │  5. Create Listing + Address               │
       │ ──────────────────────────────────────────▶│
       │    POST /v1/listings                       │
       │    { ...listing, address: {...} }          │
       │                                            │
       │                                     ┌──────▼──────┐
       │                                     │   Database  │
       │                                     │   (MySQL)   │
       │                                     │             │
       │                                     │ INSERT INTO │
       │                                     │  addresses  │
       │                                     │             │
       │                                     │ INSERT INTO │
       │                                     │  listings   │
       │                                     └──────┬──────┘
       │                                            │
       │◀───────────────────────────────────────────│
       │  { listingId: 789, addressId: 456 }        │
       │                                            │
       │  6. Generate Upload URL                    │
       │ ──────────────────────────────────────────▶│
       │    POST /v1/media/upload-url               │
       │    { mediaType, filename, ... }            │
       │                                            │
       │                                     ┌──────▼──────┐
       │                                     │   Database  │
       │                                     │             │
       │                                     │ INSERT INTO │
       │                                     │    media    │
       │                                     │ (PENDING)   │
       │                                     └──────┬──────┘
       │                                            │
       │                                     ┌──────▼──────┐
       │                                     │Cloudflare R2│
       │                                     │             │
       │                                     │  Generate   │
       │                                     │Pre-signed   │
       │                                     │     URL     │
       │                                     └──────┬──────┘
       │                                            │
       │◀───────────────────────────────────────────│
       │  { mediaId: 1001, uploadUrl: "..." }       │
       │                                            │
       │  7. Upload File to R2                      │
       │ ────────────────────────────────────┐      │
       │    PUT <pre-signed-url>              │     │
       │    <binary file data>                │     │
       │                                      │     │
       │                              ┌───────▼─────▼──┐
       │                              │  Cloudflare R2 │
       │                              │   Storage      │
       │                              │                │
       │                              │  Store File    │
       │                              └───────┬────────┘
       │◀─────────────────────────────────────┘
       │  200 OK                                       │
       │                                            │
       │  8. Confirm Upload                         │
       │ ──────────────────────────────────────────▶│
       │    POST /v1/media/1001/confirm             │
       │                                            │
       │                                     ┌──────▼──────┐
       │                                     │   Database  │
       │                                     │             │
       │                                     │ UPDATE media│
       │                                     │SET status=  │
       │                                     │  'ACTIVE'   │
       │                                     └──────┬──────┘
       │                                            │
       │◀───────────────────────────────────────────│
       │  { ...media details, status: "ACTIVE" }    │
       │                                            │

✅ Complete Flow Finished
```

---

## 7. Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     ERROR HANDLING FLOW                          │
└─────────────────────────────────────────────────────────────────┘

User Action
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ API Request                                                 │
└─────────────────────────────────────────────────────────────┘
      │
      ├─────▶ Success (200 OK)
      │       │
      │       ↓
      │      ┌─────────────────────────────────────────────┐
      │      │ Process Response                            │
      │      │ Update UI                                   │
      │      │ Show Success Message                        │
      │      └─────────────────────────────────────────────┘
      │
      └─────▶ Error (4xx, 5xx)
              │
              ↓
         ┌────────────────────────────────────────────────┐
         │ Parse Error Response                           │
         │ { code: "...", message: "...", data: {...} }   │
         └────────┬───────────────────────────────────────┘
                  │
                  ├──▶ Token Expired (5001)
                  │    └─▶ Redirect to Login
                  │
                  ├──▶ Invalid File Type (MEDIA_001)
                  │    └─▶ Show Error: "Please upload JPEG/PNG/WebP"
                  │
                  ├──▶ File Too Large (MEDIA_002)
                  │    └─▶ Show Error: "Max file size is 100MB"
                  │
                  ├──▶ Upload URL Expired (MEDIA_003)
                  │    └─▶ Retry: Generate new upload URL
                  │
                  ├──▶ Invalid Address (ADDRESS_001-004)
                  │    └─▶ Show Error: "Please reselect address"
                  │
                  └──▶ Other Errors
                       └─▶ Show Generic Error Message
                           Offer Retry Option
```

---

## Quick Reference

### Address Endpoints
```
GET  /v1/addresses/provinces                 → List all provinces
GET  /v1/addresses/provinces/{id}/districts  → List districts
GET  /v1/addresses/districts/{id}/wards      → List wards
GET  /v1/addresses/wards/{id}/streets        → List streets
```

### Listing Endpoints
```
POST /v1/listings                            → Create listing (with address)
GET  /v1/listings/{id}                       → Get listing details
```

### Media Endpoints
```
POST /v1/media/upload-url                    → Generate upload URL
POST /v1/media/{id}/confirm                  → Confirm upload
POST /v1/media/external                      → Save YouTube/TikTok
GET  /v1/media/listing/{id}                  → Get listing media
```

---

**Last Updated:** January 2025