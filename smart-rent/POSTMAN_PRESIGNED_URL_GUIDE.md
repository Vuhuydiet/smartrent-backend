# Postman Guide - Pre-signed URLs for Direct S3 Upload

## Overview

Pre-signed URLs allow **direct client-to-S3 uploads** without routing file data through your backend server.

### Benefits
- ✅ **Faster uploads** - Direct to S3/Cloudflare R2
- ✅ **Reduced server load** - No file data passes through backend
- ✅ **Better scalability** - Backend only generates URLs
- ✅ **Lower bandwidth costs** - Files don't use server bandwidth

### Architecture

```
┌────────┐    1. Request Pre-signed URL    ┌─────────┐
│ Client │ ────────────────────────────────▶│ Backend │
└────────┘                                  └─────────┘
                                                  │
                                                  │ 2. Generate
                                                  ▼
                                            ┌──────────┐
                                            │ S3/R2 API│
                                            └──────────┘
                                                  │
    ┌────────┐   3. Pre-signed URL Response│
    │ Client │ ◀──────────────────────────────────┘
    └────────┘
        │
        │ 4. Upload file directly
        ▼
    ┌──────────┐
    │ S3/R2    │
    │ Storage  │
    └──────────┘
```

---

## Step 1: Request Pre-signed URL for Image

**Endpoint**: `POST /v1/upload/presigned-url/image`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "filename": "living-room.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048576
}
```

**Field Details**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| filename | String | ✅ Yes | Original filename (e.g., "living-room.jpg") |
| contentType | String | ✅ Yes | MIME type: `image/jpeg`, `image/png`, `image/webp` |
| fileSize | Long | ❌ No | File size in bytes (for validation) |

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "uploadUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/a1b2c3d4-living-room.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...&X-Amz-Signature=...",
    "fileUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/a1b2c3d4-living-room.jpg",
    "fileKey": "images/a1b2c3d4-living-room.jpg",
    "expiresAt": "2025-10-04T10:45:00",
    "requiredHeaders": {
      "contentType": "image/jpeg"
    }
  }
}
```

**Response Fields**:
- `uploadUrl` - Pre-signed URL for uploading (valid for 15 minutes)
- `fileUrl` - Final public URL where file will be accessible
- `fileKey` - S3 object key (path in bucket)
- `expiresAt` - When the pre-signed URL expires
- `requiredHeaders` - Headers that MUST be included in upload request

**Save these values**:
- `uploadUrl` → For Step 2 (upload)
- `fileUrl` → For Step 3 (attach to listing)

---

## Step 2: Upload File Directly to S3

Now upload the file **directly to S3** using the pre-signed URL.

**Method**: `PUT`

**URL**: Use the `uploadUrl` from Step 1 response

**Headers**:
```
Content-Type: image/jpeg
```
⚠️ **IMPORTANT**: The `Content-Type` MUST match the `contentType` you specified in Step 1!

**Body**:
- **Type**: `binary`
- **Content**: Select your image file

**Postman Configuration**:
1. Create new request
2. Method: **PUT**
3. URL: Paste the `uploadUrl` from Step 1
4. Headers:
   - Add `Content-Type: image/jpeg` (match your file type)
5. Body:
   - Select **binary**
   - Click "Select File" and choose your image

**Expected Response**:
- **Status**: `200 OK` or `204 No Content`
- **Body**: Empty (S3 doesn't return body on successful upload)

**Verification**:
- If you get `200` or `204`, upload succeeded!
- If you get `403 Forbidden`, check:
  - Content-Type header matches
  - Pre-signed URL hasn't expired (15 min limit)
  - URL wasn't modified

---

## Step 3: Use File URL in Listing

Now use the `fileUrl` from Step 1 to attach the image to a listing.

**Endpoint**: `POST /v1/listings`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "Beautiful 2BR Apartment",
  "price": 15000000,
  "priceUnit": "MONTH",
  "userId": "user-123",
  "categoryId": 10,
  "productType": "APARTMENT",
  "addressId": 501,
  "area": 78.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "images": [
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/a1b2c3d4-living-room.jpg",
      "altText": "Spacious living room with city view",
      "isPrimary": true,
      "sortOrder": 0,
      "mimeType": "image/jpeg"
    }
  ]
}
```

---

## Complete Flow: Upload Multiple Images

Let's upload 3 images for a single listing.

### Request 1: Get Pre-signed URL for Living Room Image

```
POST /v1/upload/presigned-url/image
```

```json
{
  "filename": "living-room.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048576
}
```

**Save**: `uploadUrl1`, `fileUrl1`

### Request 2: Get Pre-signed URL for Bedroom Image

```
POST /v1/upload/presigned-url/image
```

```json
{
  "filename": "bedroom.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1536000
}
```

**Save**: `uploadUrl2`, `fileUrl2`

### Request 3: Get Pre-signed URL for Kitchen Image

```
POST /v1/upload/presigned-url/image
```

```json
{
  "filename": "kitchen.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1800000
}
```

**Save**: `uploadUrl3`, `fileUrl3`

### Upload Files

```
PUT <uploadUrl1>
Headers: Content-Type: image/jpeg
Body: binary (living-room.jpg file)

PUT <uploadUrl2>
Headers: Content-Type: image/jpeg
Body: binary (bedroom.jpg file)

PUT <uploadUrl3>
Headers: Content-Type: image/jpeg
Body: binary (kitchen.jpg file)
```

### Create Listing with All Images

```
POST /v1/listings
```

```json
{
  "title": "Beautiful 2BR Apartment",
  "price": 15000000,
  "priceUnit": "MONTH",
  "images": [
    {
      "url": "<fileUrl1>",
      "altText": "Living room",
      "isPrimary": true,
      "sortOrder": 0
    },
    {
      "url": "<fileUrl2>",
      "altText": "Bedroom",
      "sortOrder": 1
    },
    {
      "url": "<fileUrl3>",
      "altText": "Kitchen",
      "sortOrder": 2
    }
  ]
}
```

---

## Video Upload with Pre-signed URL

### Request Pre-signed URL for Video

**Endpoint**: `POST /v1/upload/presigned-url/video`

**Request**:
```json
{
  "filename": "apartment-tour.mp4",
  "contentType": "video/mp4",
  "fileSize": 10485760
}
```

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "uploadUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4?X-Amz-...",
    "fileUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4",
    "fileKey": "videos/uuid-tour.mp4",
    "expiresAt": "2025-10-04T10:45:00",
    "requiredHeaders": {
      "contentType": "video/mp4"
    }
  }
}
```

### Upload Video

```
PUT <uploadUrl>
Headers: Content-Type: video/mp4
Body: binary (apartment-tour.mp4 file)
```

### Attach to Listing

```json
{
  "videos": [
    {
      "url": "<fileUrl>",
      "title": "Virtual Tour",
      "description": "Complete walkthrough of the apartment",
      "sortOrder": 0,
      "mimeType": "video/mp4"
    }
  ]
}
```

---

## Error Handling

### Error: Invalid File Type

**Request**:
```json
{
  "filename": "document.pdf",
  "contentType": "application/pdf",
  "fileSize": 1000000
}
```

**Response**:
```json
{
  "code": "2009",
  "message": "Invalid file type. Allowed types: image/jpeg, image/png, image/webp",
  "data": null
}
```

### Error: File Size Exceeded

**Request**:
```json
{
  "filename": "huge-image.jpg",
  "contentType": "image/jpeg",
  "fileSize": 20971520
}
```

**Response**:
```json
{
  "code": "2010",
  "message": "File size exceeds maximum limit of 10 MB",
  "data": null
}
```

### Error: Content-Type Mismatch on Upload

If you upload with wrong Content-Type:

**Step 1 Request**: `contentType: "image/jpeg"`
**Step 2 Upload**: `Content-Type: image/png` ❌

**S3 Response**: `403 Forbidden`

**Fix**: Ensure Content-Type in Step 2 matches Step 1

### Error: Expired Pre-signed URL

If you wait more than 15 minutes before uploading:

**S3 Response**: `403 Forbidden` or `Request has expired`

**Fix**: Request a new pre-signed URL (Step 1 again)

---

## Postman Collection Setup

### Environment Variables

```
base_url = http://localhost:8080
access_token = <from login>

# Pre-signed URL responses
presigned_upload_url =
presigned_file_url =
presigned_file_key =
```

### Collection Folder: "Upload with Pre-signed URL"

**Request 1**: Generate Pre-signed URL
```javascript
// Pre-request Script
pm.environment.set("filename", "test-image-" + Date.now() + ".jpg");

// Test Script
pm.test("Pre-signed URL generated", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.uploadUrl).to.exist;

    pm.environment.set("presigned_upload_url", jsonData.data.uploadUrl);
    pm.environment.set("presigned_file_url", jsonData.data.fileUrl);
    pm.environment.set("presigned_file_key", jsonData.data.fileKey);
});
```

**Request 2**: Upload File to S3
```javascript
// Use {{presigned_upload_url}} as the request URL
// Body: binary file

// Test Script
pm.test("File uploaded successfully", function () {
    pm.response.to.have.status(200);
});
```

**Request 3**: Create Listing
```javascript
// Pre-request Script
var imageUrl = pm.environment.get("presigned_file_url");
pm.environment.set("current_image_url", imageUrl);

// Body uses {{current_image_url}}
```

---

## Comparison: Legacy vs Pre-signed URL

### Legacy Upload (POST /v1/upload/image)

**Flow**:
```
Client → Backend (with file) → S3 → Backend → Client (URL)
```

**Pros**:
- Simple single request
- Backend validates file content

**Cons**:
- File data passes through backend
- Higher server bandwidth
- Slower for large files

### Pre-signed URL Upload (Recommended)

**Flow**:
```
Client → Backend (request URL) → Client
Client → S3 (direct upload)
```

**Pros**:
- ✅ Direct to S3 (faster)
- ✅ No backend bandwidth used
- ✅ Better scalability
- ✅ Supports larger files

**Cons**:
- Requires 2 requests
- 15-minute URL expiration

---

## Best Practices

### 1. Request Pre-signed URLs in Batch

If uploading multiple files, request all pre-signed URLs first:

```javascript
const files = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
const presignedUrls = [];

for (const file of files) {
    const response = await fetch('/v1/upload/presigned-url/image', {
        method: 'POST',
        body: JSON.stringify({
            filename: file,
            contentType: 'image/jpeg',
            fileSize: fileSize
        })
    });
    presignedUrls.push(await response.json());
}

// Then upload all files in parallel
```

### 2. Upload in Parallel

Upload multiple files simultaneously:

```javascript
const uploads = presignedUrls.map(async (presigned, index) => {
    return fetch(presigned.data.uploadUrl, {
        method: 'PUT',
        headers: {
            'Content-Type': presigned.data.requiredHeaders.contentType
        },
        body: fileBlobs[index]
    });
});

await Promise.all(uploads);
```

### 3. Handle Expiration

Pre-signed URLs expire in 15 minutes. If upload fails, request a new URL:

```javascript
async function uploadWithRetry(file) {
    let presigned = await getPresignedUrl(file);

    try {
        await uploadToS3(presigned.uploadUrl, file);
    } catch (error) {
        if (error.status === 403) {
            // URL might be expired, get new one
            presigned = await getPresignedUrl(file);
            await uploadToS3(presigned.uploadUrl, file);
        }
    }
}
```

### 4. Verify Upload

After upload, verify the file is accessible:

```javascript
async function verifyUpload(fileUrl) {
    const response = await fetch(fileUrl, { method: 'HEAD' });
    return response.ok;
}
```

---

## Security Considerations

### Pre-signed URLs are Public

⚠️ Anyone with the pre-signed URL can upload to that specific S3 location within 15 minutes.

**Mitigations**:
- URLs expire in 15 minutes
- Only authenticated users can request URLs
- File overwrite is prevented (unique UUID in filename)

### Content-Type Enforcement

Pre-signed URLs enforce the Content-Type specified in the request. This prevents:
- Uploading different file types than requested
- Content-type confusion attacks

### File Size Validation

Validate file size **before** requesting pre-signed URL to prevent:
- Wasting pre-signed URLs on invalid files
- Uploading oversized files

---

## Summary

| Step | Endpoint | Purpose |
|------|----------|---------|
| 1 | `POST /v1/upload/presigned-url/image` | Get pre-signed URL |
| 2 | `PUT <uploadUrl>` | Upload file to S3 |
| 3 | `POST /v1/listings` | Create listing with fileUrl |

**Time Limit**: Complete upload within 15 minutes of requesting pre-signed URL.

**Supported Types**:
- Images: `image/jpeg`, `image/png`, `image/webp`
- Videos: `video/mp4`, `video/quicktime`

Now you can upload files directly to S3 with optimal performance! 🚀
