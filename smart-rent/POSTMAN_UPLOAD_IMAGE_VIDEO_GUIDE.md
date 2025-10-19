# Postman Guide - Upload Images & Videos for Listings

## Complete Flow: Upload Files → Create Listing with Media

This guide shows you how to upload images/videos to storage and then attach them to a listing.

---

## Step 1: Upload Images

Upload images first to get their URLs, then use those URLs when creating/updating listings.

**Endpoint**: `POST /v1/upload/image`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Body Type**: `form-data`

**Form Data**:
| Key  | Type | Value |
|------|------|-------|
| file | File | (Select your image file) |

**Allowed Image Types**:
- `image/jpeg` (.jpg, .jpeg)
- `image/png` (.png)
- `image/webp` (.webp)

**Max File Size**: 10 MB (configurable via `S3_MAX_FILE_SIZE_MB`)

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "status": "success",
    "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-filename.jpg"
  }
}
```

**Save this URL** - you'll use it in the listing creation request.

---

## Step 2: Upload Videos

Similar to images, upload videos to get their URLs.

**Endpoint**: `POST /v1/upload/video`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Body Type**: `form-data`

**Form Data**:
| Key  | Type | Value |
|------|------|-------|
| file | File | (Select your video file) |

**Allowed Video Types**:
- `video/mp4` (.mp4)
- `video/quicktime` (.mov)

**Max File Size**: 10 MB (configurable)

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "status": "success",
    "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-video.mp4"
  }
}
```

**Save this URL** - you'll use it in the listing creation request.

---

## Step 3: Create Listing with Images and Videos

Now create a listing using the uploaded file URLs.

**Endpoint**: `POST /v1/listings`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "description": "Căn hộ 2 phòng ngủ rộng rãi, có ban công và tầm nhìn đẹp ra thành phố. Gần chợ Bến Thành, tiện ích đầy đủ.",
  "userId": "user-123e4567-e89b-12d3-a456-426614174000",
  "expiryDate": "2025-12-31T23:59:59",
  "listingType": "RENT",
  "verified": false,
  "isVerify": false,
  "expired": false,
  "vipType": "NORMAL",
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 15000000.00,
  "priceUnit": "MONTH",
  "addressId": 501,
  "area": 78.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "direction": "NORTHEAST",
  "furnishing": "SEMI_FURNISHED",
  "propertyType": "APARTMENT",
  "roomCapacity": 4,
  "amenityIds": [1, 3, 5],
  "images": [
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-living-room.jpg",
      "altText": "Living room with city view",
      "sortOrder": 0,
      "isPrimary": true,
      "mimeType": "image/jpeg"
    },
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-bedroom.jpg",
      "altText": "Master bedroom",
      "sortOrder": 1,
      "isPrimary": false,
      "mimeType": "image/jpeg"
    },
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-kitchen.jpg",
      "altText": "Modern kitchen",
      "sortOrder": 2,
      "isPrimary": false,
      "mimeType": "image/jpeg"
    }
  ],
  "videos": [
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4",
      "title": "Virtual tour of the apartment",
      "description": "Complete walkthrough of the 2-bedroom apartment",
      "durationSeconds": 120,
      "mimeType": "video/mp4",
      "thumbnailUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-video-thumb.jpg",
      "sortOrder": 0
    }
  ]
}
```

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "status": "CREATED"
  }
}
```

**What Happened**:
- Listing created with ID 123
- 3 images attached (1 primary, 2 secondary)
- 1 video attached
- Initial pricing history created

---

## Step 4: Get Listing with Images and Videos

**Endpoint**: `GET /v1/listings/123`

**Headers**:
```
Authorization: Bearer <your_access_token>
```

**Response**:
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "listingId": 123,
    "title": "Căn hộ 2PN thoáng mát quận 1",
    "description": "Căn hộ 2 phòng ngủ rộng rãi...",
    "price": 15000000.00,
    "priceUnit": "MONTH",
    "images": [
      {
        "id": 1,
        "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-living-room.jpg",
        "altText": "Living room with city view",
        "sortOrder": 0,
        "isPrimary": true,
        "fileSize": null,
        "mimeType": "image/jpeg",
        "createdAt": "2025-10-04T10:30:00",
        "updatedAt": "2025-10-04T10:30:00"
      },
      {
        "id": 2,
        "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-bedroom.jpg",
        "altText": "Master bedroom",
        "sortOrder": 1,
        "isPrimary": false,
        "fileSize": null,
        "mimeType": "image/jpeg",
        "createdAt": "2025-10-04T10:30:00",
        "updatedAt": "2025-10-04T10:30:00"
      },
      {
        "id": 3,
        "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-kitchen.jpg",
        "altText": "Modern kitchen",
        "sortOrder": 2,
        "isPrimary": false,
        "fileSize": null,
        "mimeType": "image/jpeg",
        "createdAt": "2025-10-04T10:30:00",
        "updatedAt": "2025-10-04T10:30:00"
      }
    ],
    "videos": [
      {
        "id": 1,
        "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4",
        "title": "Virtual tour of the apartment",
        "description": "Complete walkthrough of the 2-bedroom apartment",
        "durationSeconds": 120,
        "fileSize": null,
        "mimeType": "video/mp4",
        "thumbnailUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-video-thumb.jpg",
        "sortOrder": 0,
        "createdAt": "2025-10-04T10:30:00",
        "updatedAt": "2025-10-04T10:30:00"
      }
    ],
    "currentPricing": {
      "id": 1,
      "listingId": 123,
      "newPrice": 15000000.00,
      "newPriceUnit": "MONTH",
      "changeType": "INITIAL",
      "isCurrent": true,
      "changedAt": "2025-10-04T10:30:00"
    }
  }
}
```

**Image Ordering**:
- Primary images appear first
- Then sorted by `sortOrder` (ascending)

**Video Ordering**:
- Sorted by `sortOrder` (ascending)

---

## Step 5: Update Listing Images/Videos

You can update images and videos when updating a listing.

**Endpoint**: `PUT /v1/listings/123`

**Headers**:
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

**Request Body** (Add more images):
```json
{
  "title": "Căn hộ 2PN thoáng mát quận 1",
  "userId": 42,
  "categoryId": 10,
  "productType": "APARTMENT",
  "price": 15000000.00,
  "priceUnit": "MONTH",
  "addressId": 501,
  "images": [
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-living-room.jpg",
      "altText": "Living room with city view",
      "sortOrder": 0,
      "isPrimary": true,
      "mimeType": "image/jpeg"
    },
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-bedroom.jpg",
      "altText": "Master bedroom",
      "sortOrder": 1,
      "isPrimary": false,
      "mimeType": "image/jpeg"
    },
    {
      "url": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-balcony-new.jpg",
      "altText": "Balcony with view",
      "sortOrder": 2,
      "isPrimary": false,
      "mimeType": "image/jpeg"
    }
  ]
}
```

**Important**:
- Providing `images` array **replaces** all existing images
- Providing `videos` array **replaces** all existing videos
- Omitting `images` or `videos` keeps existing ones unchanged

---

## Upload Error Handling

### Error: Invalid File Type

**Request**: Upload a `.txt` file to `/v1/upload/image`

**Response**:
```json
{
  "code": "2009",
  "message": "Invalid file type. Allowed types: image/jpeg, image/png, image/webp",
  "data": null
}
```

### Error: File Size Exceeded

**Request**: Upload 15 MB file (exceeds 10 MB limit)

**Response**:
```json
{
  "code": "2010",
  "message": "File size exceeds maximum limit of 10 MB",
  "data": null
}
```

### Error: Empty File

**Request**: Upload empty file

**Response**:
```json
{
  "code": "2012",
  "message": "File is empty",
  "data": null
}
```

---

## Best Practices

### 1. Image Upload Strategy

**Recommended Order**:
1. Upload all images first
2. Collect all image URLs
3. Create listing with all images at once

**Why**: Avoids orphaned images in storage if listing creation fails.

### 2. Primary Image

Always set exactly **one image** with `isPrimary: true`:
```json
{
  "url": "...",
  "isPrimary": true,
  "sortOrder": 0
}
```

This will be displayed as the listing's cover/thumbnail.

### 3. Sort Order

Use consistent sorting:
- Primary image: `sortOrder: 0`
- Secondary images: `sortOrder: 1, 2, 3...`
- Videos: `sortOrder: 0, 1, 2...`

### 4. Alt Text for SEO

Provide descriptive alt text for images:
```json
{
  "altText": "Spacious living room with panoramic city view and modern furniture"
}
```

### 5. Video Thumbnails

Always provide a thumbnail for videos:
```json
{
  "url": "https://.../video.mp4",
  "thumbnailUrl": "https://.../video-thumb.jpg"
}
```

---

## Postman Collection Scripts

### Pre-request: Upload Multiple Images

```javascript
// Upload 3 images and store URLs
const images = [];
const imageFiles = ['living-room.jpg', 'bedroom.jpg', 'kitchen.jpg'];

imageFiles.forEach((file, index) => {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/v1/upload/image",
        method: 'POST',
        header: {
            'Authorization': 'Bearer ' + pm.environment.get("access_token")
        },
        body: {
            mode: 'formdata',
            formdata: [
                { key: 'file', src: file, type: 'file' }
            ]
        }
    }, (err, res) => {
        if (!err && res.code === 200) {
            images.push({
                url: res.json().data.url,
                isPrimary: index === 0,
                sortOrder: index
            });
        }
    });
});

// Store in environment
pm.environment.set("uploaded_images", JSON.stringify(images));
```

### Test Script: Verify Images Attached

```javascript
pm.test("Listing has images", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.images).to.be.an('array');
    pm.expect(jsonData.data.images.length).to.be.greaterThan(0);
});

pm.test("Primary image exists", function () {
    var images = pm.response.json().data.images;
    var primaryImage = images.find(img => img.isPrimary === true);
    pm.expect(primaryImage).to.exist;
});

pm.test("Images are sorted correctly", function () {
    var images = pm.response.json().data.images;
    var sortOrders = images.map(img => img.sortOrder);
    var isSorted = sortOrders.every((val, i, arr) => !i || arr[i - 1] <= val);
    pm.expect(isSorted).to.be.true;
});
```

---

## Complete Example Flow in Postman

### Collection: "Listing with Media"

**Folder 1: Authentication**
- `POST Login` → Save access_token

**Folder 2: Upload Files**
- `POST Upload Image 1 (Living Room)` → Save URL
- `POST Upload Image 2 (Bedroom)` → Save URL
- `POST Upload Image 3 (Kitchen)` → Save URL
- `POST Upload Video (Tour)` → Save URL

**Folder 3: Listing Operations**
- `POST Create Listing with Media` → Use saved URLs
- `GET Get Listing` → Verify images/videos attached
- `PUT Update Listing Images` → Add/remove images
- `DELETE Delete Listing` → Cleanup

---

## Image/Video Fields Reference

### ImageRequest Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| url | String | ✅ Yes | URL from upload endpoint |
| altText | String | ❌ No | SEO/accessibility text |
| sortOrder | Integer | ❌ No | Display order (default: 0) |
| isPrimary | Boolean | ❌ No | Is cover image (default: false) |
| mimeType | String | ❌ No | MIME type (e.g., "image/jpeg") |

### VideoRequest Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| url | String | ✅ Yes | URL from upload endpoint |
| title | String | ❌ No | Video title |
| description | String | ❌ No | Video description |
| durationSeconds | Integer | ❌ No | Video length in seconds |
| mimeType | String | ❌ No | MIME type (e.g., "video/mp4") |
| thumbnailUrl | String | ❌ No | Thumbnail image URL |
| sortOrder | Integer | ❌ No | Display order (default: 0) |

---

## Summary

✅ **Upload Flow**: Upload files → Get URLs → Attach to listing
✅ **Validation**: File type, size, and empty file checks
✅ **Ordering**: Primary images first, then by sortOrder
✅ **Updates**: Replace all images/videos or keep existing
✅ **Cleanup**: Deleting listing cascades to images/videos

Now you can create rich listings with multiple images and videos! 🎉
