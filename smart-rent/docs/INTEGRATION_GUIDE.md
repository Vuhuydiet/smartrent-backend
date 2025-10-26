# SmartRent Integration Guide

This guide provides step-by-step instructions for database migrations, address management, listing creation, and media upload flows.

---

## Table of Contents
1. [Database Migration for Address](#1-database-migration-for-address)
2. [Frontend: Address Selection Flow](#2-frontend-address-selection-flow)
3. [Frontend: Create Listing with Address](#3-frontend-create-listing-with-address)
4. [Frontend: Image Upload Flow](#4-frontend-image-upload-flow)

---

## 1. Database Migration for Address

### 1.1 Understanding the Address Hierarchy

SmartRent uses Vietnam's 3-tier administrative structure:

```
Province (Tỉnh/Thành phố)
  └── District (Quận/Huyện)
      └── Ward (Phường/Xã)
          └── Street (Đường)
```

### 1.2 Creating Address Migration Files

#### Step 1: Create Province Migration

**File:** `src/main/resources/db/migration/V1__Create_provinces_table.sql`

```sql
CREATE TABLE provinces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- CITY, PROVINCE
    is_active BOOLEAN DEFAULT TRUE,
    is_merged BOOLEAN DEFAULT FALSE,
    original_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_province_code (code),
    INDEX idx_province_name (name),
    INDEX idx_province_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Step 2: Create District Migration

**File:** `src/main/resources/db/migration/V2__Create_districts_table.sql`

```sql
CREATE TABLE districts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(50) NOT NULL, -- URBAN_DISTRICT, RURAL_DISTRICT, TOWN
    province_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (province_id) REFERENCES provinces(id) ON DELETE CASCADE,
    INDEX idx_district_province (province_id),
    INDEX idx_district_code (code),
    INDEX idx_district_name (name),
    INDEX idx_district_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Step 3: Create Ward Migration

**File:** `src/main/resources/db/migration/V3__Create_wards_table.sql`

```sql
CREATE TABLE wards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(50) NOT NULL, -- WARD, COMMUNE, TOWNSHIP
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (district_id) REFERENCES districts(id) ON DELETE CASCADE,
    FOREIGN KEY (province_id) REFERENCES provinces(id) ON DELETE CASCADE,
    INDEX idx_ward_district (district_id),
    INDEX idx_ward_province (province_id),
    INDEX idx_ward_code (code),
    INDEX idx_ward_name (name),
    INDEX idx_ward_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Step 4: Create Street Migration

**File:** `src/main/resources/db/migration/V4__Create_streets_table.sql`

```sql
CREATE TABLE streets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ward_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (ward_id) REFERENCES wards(id) ON DELETE CASCADE,
    FOREIGN KEY (district_id) REFERENCES districts(id) ON DELETE CASCADE,
    FOREIGN KEY (province_id) REFERENCES provinces(id) ON DELETE CASCADE,
    INDEX idx_street_ward (ward_id),
    INDEX idx_street_district (district_id),
    INDEX idx_street_province (province_id),
    INDEX idx_street_name (name),
    INDEX idx_street_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Step 5: Create Address Table Migration

**File:** `src/main/resources/db/migration/V5__Create_addresses_table.sql`

```sql
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    street_number VARCHAR(50),
    street_id BIGINT NOT NULL,
    ward_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    province_id BIGINT NOT NULL,
    full_address TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (street_id) REFERENCES streets(id),
    FOREIGN KEY (ward_id) REFERENCES wards(id),
    FOREIGN KEY (district_id) REFERENCES districts(id),
    FOREIGN KEY (province_id) REFERENCES provinces(id),
    INDEX idx_address_province (province_id),
    INDEX idx_address_district (district_id),
    INDEX idx_address_ward (ward_id),
    INDEX idx_address_street (street_id),
    INDEX idx_address_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Step 6: Integrate Address with Listing

**File:** `src/main/resources/db/migration/V6__Add_address_to_listings.sql`

```sql
-- Add address_id to listings table
ALTER TABLE listings
ADD COLUMN address_id BIGINT,
ADD CONSTRAINT fk_listing_address
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL;

-- Add index for better query performance
CREATE INDEX idx_listing_address ON listings(address_id);
```

### 1.3 Running Migrations

```bash
# Run all migrations
./gradlew flywayMigrate

# Run with custom database (if needed)
./gradlew flywayMigrate \
  -PdbUrl=jdbc:mysql://localhost:3306/smartrent \
  -PdbUser=root \
  -PdbPassword=yourpassword
```

### 1.4 Verify Migrations

```bash
# Check migration status
./gradlew flywayInfo

# Expected output:
# +-----------+---------+---------------------+----------+
# | Category  | Version | Description         | State    |
# +-----------+---------+---------------------+----------+
# | Versioned | 1       | Create provinces    | Success  |
# | Versioned | 2       | Create districts    | Success  |
# | Versioned | 3       | Create wards        | Success  |
# | Versioned | 4       | Create streets      | Success  |
# | Versioned | 5       | Create addresses    | Success  |
# | Versioned | 6       | Add address to...   | Success  |
# +-----------+---------+---------------------+----------+
```

---

## 2. Frontend: Address Selection Flow

### 2.1 Flow Overview

```
User Interaction Flow:
1. Select Province → Load Districts
2. Select District → Load Wards
3. Select Ward → Load Streets
4. Enter Street Number (optional)
5. Full Address Auto-Generated
```

### 2.2 Step-by-Step Implementation

#### Step 1: Fetch All Provinces

**Endpoint:** `GET /v1/addresses/provinces`

```javascript
// React/Vue/Angular Example
const fetchProvinces = async () => {
  try {
    const response = await fetch('http://localhost:8080/v1/addresses/provinces', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    // Response format:
    // {
    //   "code": "999999",
    //   "message": null,
    //   "data": [
    //     {
    //       "id": 1,
    //       "name": "Thành phố Hà Nội",
    //       "code": "01",
    //       "type": "CITY",
    //       "level": "PROVINCE",
    //       "isActive": true,
    //       "fullAddressText": "Thành phố Hà Nội"
    //     },
    //     // ... more provinces
    //   ]
    // }

    return result.data;
  } catch (error) {
    console.error('Error fetching provinces:', error);
    throw error;
  }
};
```

#### Step 2: Fetch Districts for Selected Province

**Endpoint:** `GET /v1/addresses/provinces/{provinceId}/districts`

```javascript
const fetchDistricts = async (provinceId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/v1/addresses/provinces/${provinceId}/districts`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    const result = await response.json();

    // Response format:
    // {
    //   "code": "999999",
    //   "message": null,
    //   "data": [
    //     {
    //       "id": 1,
    //       "name": "Quận Ba Đình",
    //       "code": "001",
    //       "type": "URBAN_DISTRICT",
    //       "level": "DISTRICT",
    //       "provinceId": 1,
    //       "provinceName": "Thành phố Hà Nội",
    //       "fullAddressText": "Quận Ba Đình, Thành phố Hà Nội"
    //     },
    //     // ... more districts
    //   ]
    // }

    return result.data;
  } catch (error) {
    console.error('Error fetching districts:', error);
    throw error;
  }
};
```

#### Step 3: Fetch Wards for Selected District

**Endpoint:** `GET /v1/addresses/districts/{districtId}/wards`

```javascript
const fetchWards = async (districtId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/v1/addresses/districts/${districtId}/wards`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    const result = await response.json();

    // Response format:
    // {
    //   "code": "999999",
    //   "message": null,
    //   "data": [
    //     {
    //       "id": 1,
    //       "name": "Phường Phúc Xá",
    //       "code": "00001",
    //       "type": "WARD",
    //       "level": "WARD",
    //       "districtId": 1,
    //       "districtName": "Quận Ba Đình",
    //       "provinceId": 1,
    //       "provinceName": "Thành phố Hà Nội",
    //       "fullAddressText": "Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
    //     },
    //     // ... more wards
    //   ]
    // }

    return result.data;
  } catch (error) {
    console.error('Error fetching wards:', error);
    throw error;
  }
};
```

#### Step 4: Fetch Streets for Selected Ward

**Endpoint:** `GET /v1/addresses/wards/{wardId}/streets`

```javascript
const fetchStreets = async (wardId) => {
  try {
    const response = await fetch(
      `http://localhost:8080/v1/addresses/wards/${wardId}/streets`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      }
    );

    const result = await response.json();

    // Response format:
    // {
    //   "code": "999999",
    //   "message": null,
    //   "data": [
    //     {
    //       "id": 1,
    //       "name": "Đường Nguyễn Trãi",
    //       "wardId": 1,
    //       "districtId": 1,
    //       "provinceId": 1
    //     },
    //     // ... more streets
    //   ]
    // }

    return result.data;
  } catch (error) {
    console.error('Error fetching streets:', error);
    throw error;
  }
};
```

### 2.3 Complete React Component Example

```jsx
import React, { useState, useEffect } from 'react';

const AddressSelector = ({ onAddressChange }) => {
  const [provinces, setProvinces] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [wards, setWards] = useState([]);
  const [streets, setStreets] = useState([]);

  const [selectedProvince, setSelectedProvince] = useState(null);
  const [selectedDistrict, setSelectedDistrict] = useState(null);
  const [selectedWard, setSelectedWard] = useState(null);
  const [selectedStreet, setSelectedStreet] = useState(null);
  const [streetNumber, setStreetNumber] = useState('');

  // Load provinces on mount
  useEffect(() => {
    fetchProvinces().then(setProvinces);
  }, []);

  // Load districts when province changes
  useEffect(() => {
    if (selectedProvince) {
      fetchDistricts(selectedProvince.id).then(setDistricts);
      // Reset dependent selections
      setSelectedDistrict(null);
      setSelectedWard(null);
      setSelectedStreet(null);
      setWards([]);
      setStreets([]);
    }
  }, [selectedProvince]);

  // Load wards when district changes
  useEffect(() => {
    if (selectedDistrict) {
      fetchWards(selectedDistrict.id).then(setWards);
      // Reset dependent selections
      setSelectedWard(null);
      setSelectedStreet(null);
      setStreets([]);
    }
  }, [selectedDistrict]);

  // Load streets when ward changes
  useEffect(() => {
    if (selectedWard) {
      fetchStreets(selectedWard.id).then(setStreets);
      // Reset dependent selection
      setSelectedStreet(null);
    }
  }, [selectedWard]);

  // Notify parent component when address is complete
  useEffect(() => {
    if (selectedStreet) {
      const addressData = {
        streetNumber: streetNumber || null,
        streetId: selectedStreet.id,
        wardId: selectedWard.id,
        districtId: selectedDistrict.id,
        provinceId: selectedProvince.id,
        fullAddress: buildFullAddress()
      };
      onAddressChange(addressData);
    }
  }, [selectedStreet, streetNumber]);

  const buildFullAddress = () => {
    const parts = [];
    if (streetNumber) parts.push(streetNumber);
    if (selectedStreet) parts.push(selectedStreet.name);
    if (selectedWard) parts.push(selectedWard.name);
    if (selectedDistrict) parts.push(selectedDistrict.name);
    if (selectedProvince) parts.push(selectedProvince.name);
    return parts.join(', ');
  };

  return (
    <div className="address-selector">
      <h3>Select Address</h3>

      {/* Province Selector */}
      <div className="form-group">
        <label>Province/City:</label>
        <select
          value={selectedProvince?.id || ''}
          onChange={(e) => {
            const province = provinces.find(p => p.id === parseInt(e.target.value));
            setSelectedProvince(province);
          }}
        >
          <option value="">Select Province/City</option>
          {provinces.map(province => (
            <option key={province.id} value={province.id}>
              {province.name}
            </option>
          ))}
        </select>
      </div>

      {/* District Selector */}
      {selectedProvince && (
        <div className="form-group">
          <label>District:</label>
          <select
            value={selectedDistrict?.id || ''}
            onChange={(e) => {
              const district = districts.find(d => d.id === parseInt(e.target.value));
              setSelectedDistrict(district);
            }}
          >
            <option value="">Select District</option>
            {districts.map(district => (
              <option key={district.id} value={district.id}>
                {district.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Ward Selector */}
      {selectedDistrict && (
        <div className="form-group">
          <label>Ward/Commune:</label>
          <select
            value={selectedWard?.id || ''}
            onChange={(e) => {
              const ward = wards.find(w => w.id === parseInt(e.target.value));
              setSelectedWard(ward);
            }}
          >
            <option value="">Select Ward/Commune</option>
            {wards.map(ward => (
              <option key={ward.id} value={ward.id}>
                {ward.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Street Selector */}
      {selectedWard && (
        <div className="form-group">
          <label>Street:</label>
          <select
            value={selectedStreet?.id || ''}
            onChange={(e) => {
              const street = streets.find(s => s.id === parseInt(e.target.value));
              setSelectedStreet(street);
            }}
          >
            <option value="">Select Street</option>
            {streets.map(street => (
              <option key={street.id} value={street.id}>
                {street.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Street Number Input */}
      {selectedStreet && (
        <div className="form-group">
          <label>Street Number (Optional):</label>
          <input
            type="text"
            value={streetNumber}
            onChange={(e) => setStreetNumber(e.target.value)}
            placeholder="e.g., 123"
          />
        </div>
      )}

      {/* Full Address Display */}
      {selectedStreet && (
        <div className="full-address">
          <label>Full Address:</label>
          <p>{buildFullAddress()}</p>
        </div>
      )}
    </div>
  );
};

export default AddressSelector;
```

---

## 3. Frontend: Create Listing with Address

### 3.1 Flow Overview

```
Listing Creation Flow:
1. User fills listing details (title, description, price, etc.)
2. User selects address (using AddressSelector component)
3. Frontend collects all data
4. Send POST request to create listing
5. Backend creates address and listing in single transaction
6. Return created listing with address details
```

### 3.2 Create Listing Request

**Endpoint:** `POST /v1/listings`

```javascript
const createListing = async (listingData) => {
  try {
    // Get JWT token from localStorage/sessionStorage
    const token = localStorage.getItem('accessToken');

    const response = await fetch('http://localhost:8080/v1/listings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(listingData)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    const result = await response.json();
    return result.data;

  } catch (error) {
    console.error('Error creating listing:', error);
    throw error;
  }
};
```

### 3.3 Request Body Format

```javascript
const listingRequest = {
  // Basic listing information
  title: "Beautiful 2-Bedroom Apartment in Ba Dinh",
  description: "Spacious apartment with modern amenities...",

  // Listing type and tier
  listingType: "RENT",  // RENT or SALE
  vipType: "NORMAL",    // NORMAL, SILVER, GOLD, DIAMOND
  productType: "APARTMENT", // APARTMENT, HOUSE, ROOM, etc.

  // Pricing
  price: 12000000,      // Price in VND
  priceUnit: "MONTH",   // MONTH, DAY, YEAR

  // Property details
  area: 78.5,           // Square meters
  bedrooms: 2,
  bathrooms: 1,

  // Address object (from AddressSelector)
  address: {
    streetNumber: "123",
    streetId: 456,
    wardId: 789,
    districtId: 12,
    provinceId: 1,
    fullAddress: "123 Nguyen Trai, Phuong Phuc Xa, Quan Ba Dinh, Thanh pho Ha Noi",
    latitude: 21.028511,   // Optional
    longitude: 105.804817, // Optional
    isVerified: false      // Optional
  },

  // Amenities (optional)
  amenityIds: [1, 2, 5, 8]  // Array of amenity IDs
};
```

### 3.4 Complete React Component Example

```jsx
import React, { useState } from 'react';
import AddressSelector from './AddressSelector';

const CreateListingForm = () => {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    listingType: 'RENT',
    vipType: 'NORMAL',
    productType: 'APARTMENT',
    price: '',
    priceUnit: 'MONTH',
    area: '',
    bedrooms: 1,
    bathrooms: 1,
    amenityIds: []
  });

  const [addressData, setAddressData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      // Validate address is selected
      if (!addressData) {
        throw new Error('Please select a complete address');
      }

      // Prepare request body
      const requestBody = {
        ...formData,
        price: parseFloat(formData.price),
        area: parseFloat(formData.area),
        address: addressData
      };

      // Create listing
      const result = await createListing(requestBody);

      console.log('Listing created successfully:', result);
      alert('Listing created successfully!');

      // Navigate to listing details or reset form
      // history.push(`/listings/${result.listingId}`);

    } catch (err) {
      console.error('Error:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="create-listing-form">
      <h2>Create New Listing</h2>

      {error && <div className="error-message">{error}</div>}

      {/* Basic Information */}
      <section>
        <h3>Basic Information</h3>

        <div className="form-group">
          <label>Title *</label>
          <input
            type="text"
            value={formData.title}
            onChange={(e) => setFormData({...formData, title: e.target.value})}
            required
            placeholder="Beautiful 2-Bedroom Apartment"
          />
        </div>

        <div className="form-group">
          <label>Description *</label>
          <textarea
            value={formData.description}
            onChange={(e) => setFormData({...formData, description: e.target.value})}
            required
            rows={5}
            placeholder="Describe your property..."
          />
        </div>

        <div className="form-group">
          <label>Listing Type *</label>
          <select
            value={formData.listingType}
            onChange={(e) => setFormData({...formData, listingType: e.target.value})}
          >
            <option value="RENT">For Rent</option>
            <option value="SALE">For Sale</option>
          </select>
        </div>

        <div className="form-group">
          <label>Property Type *</label>
          <select
            value={formData.productType}
            onChange={(e) => setFormData({...formData, productType: e.target.value})}
          >
            <option value="APARTMENT">Apartment</option>
            <option value="HOUSE">House</option>
            <option value="ROOM">Room</option>
            <option value="VILLA">Villa</option>
            <option value="OFFICE">Office</option>
          </select>
        </div>
      </section>

      {/* Pricing */}
      <section>
        <h3>Pricing</h3>

        <div className="form-group">
          <label>Price (VND) *</label>
          <input
            type="number"
            value={formData.price}
            onChange={(e) => setFormData({...formData, price: e.target.value})}
            required
            min="0"
            placeholder="12000000"
          />
        </div>

        <div className="form-group">
          <label>Price Unit *</label>
          <select
            value={formData.priceUnit}
            onChange={(e) => setFormData({...formData, priceUnit: e.target.value})}
          >
            <option value="MONTH">Per Month</option>
            <option value="DAY">Per Day</option>
            <option value="YEAR">Per Year</option>
          </select>
        </div>
      </section>

      {/* Property Details */}
      <section>
        <h3>Property Details</h3>

        <div className="form-group">
          <label>Area (m²) *</label>
          <input
            type="number"
            value={formData.area}
            onChange={(e) => setFormData({...formData, area: e.target.value})}
            required
            min="0"
            step="0.1"
            placeholder="78.5"
          />
        </div>

        <div className="form-group">
          <label>Bedrooms</label>
          <input
            type="number"
            value={formData.bedrooms}
            onChange={(e) => setFormData({...formData, bedrooms: parseInt(e.target.value)})}
            min="0"
          />
        </div>

        <div className="form-group">
          <label>Bathrooms</label>
          <input
            type="number"
            value={formData.bathrooms}
            onChange={(e) => setFormData({...formData, bathrooms: parseInt(e.target.value)})}
            min="0"
          />
        </div>
      </section>

      {/* Address Selection */}
      <section>
        <AddressSelector onAddressChange={setAddressData} />
      </section>

      {/* Submit Button */}
      <div className="form-actions">
        <button type="submit" disabled={loading || !addressData}>
          {loading ? 'Creating...' : 'Create Listing'}
        </button>
      </div>
    </form>
  );
};

export default CreateListingForm;
```

### 3.5 Response Format

```javascript
// Success Response
{
  "code": "999999",
  "message": "Listing created successfully",
  "data": {
    "listingId": 123,
    "title": "Beautiful 2-Bedroom Apartment in Ba Dinh",
    "description": "Spacious apartment with modern amenities...",
    "price": 12000000,
    "priceUnit": "MONTH",
    "vipType": "NORMAL",
    "addressId": 456,
    "area": 78.5,
    "bedrooms": 2,
    "bathrooms": 1,
    "amenities": [...],
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

---

## 4. Frontend: Image Upload Flow

### 4.1 Upload Flow Overview

```
3-Step Upload Process:
1. Generate Upload URL (Backend creates pre-signed URL)
2. Upload File to R2 (Frontend uploads directly to Cloudflare R2)
3. Confirm Upload (Frontend confirms completion to backend)
```

### 4.2 Step 1: Generate Upload URL

**Endpoint:** `POST /v1/media/upload-url`

```javascript
const generateUploadUrl = async (fileInfo) => {
  try {
    const token = localStorage.getItem('accessToken');

    const response = await fetch('http://localhost:8080/v1/media/upload-url', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(fileInfo)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    const result = await response.json();
    return result.data;

  } catch (error) {
    console.error('Error generating upload URL:', error);
    throw error;
  }
};
```

**Request Body:**

```javascript
const fileInfo = {
  mediaType: "IMAGE",           // IMAGE or VIDEO
  filename: "apartment-photo.jpg",
  contentType: "image/jpeg",    // image/jpeg, image/png, image/webp, video/mp4
  fileSize: 2048576,            // File size in bytes (max 100MB)
  listingId: 123,               // Optional: associate with listing
  title: "Living Room",         // Optional
  description: "Spacious living room with natural light",  // Optional
  altText: "Living room photo", // Optional
  isPrimary: true,              // Optional: mark as primary image
  sortOrder: 0                  // Optional: display order
};
```

**Response:**

```javascript
{
  "code": "999999",
  "message": "Upload URL generated successfully. Please upload file within 30 minutes.",
  "data": {
    "mediaId": 789,
    "uploadUrl": "https://r2.cloudflare.com/bucket/path/to/file?signature=...",
    "expiresIn": 1800,  // 30 minutes in seconds
    "storageKey": "media/2025/01/15/uuid-apartment-photo.jpg",
    "message": "Upload your file to the provided URL using PUT method"
  }
}
```

### 4.3 Step 2: Upload File to R2

```javascript
const uploadFileToR2 = async (file, uploadUrl) => {
  try {
    const response = await fetch(uploadUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': file.type
      },
      body: file
    });

    if (!response.ok) {
      throw new Error('Failed to upload file to R2');
    }

    console.log('File uploaded successfully to R2');
    return true;

  } catch (error) {
    console.error('Error uploading to R2:', error);
    throw error;
  }
};
```

### 4.4 Step 3: Confirm Upload

**Endpoint:** `POST /v1/media/{mediaId}/confirm`

```javascript
const confirmUpload = async (mediaId, confirmationData) => {
  try {
    const token = localStorage.getItem('accessToken');

    const response = await fetch(
      `http://localhost:8080/v1/media/${mediaId}/confirm`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(confirmationData)
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    const result = await response.json();
    return result.data;

  } catch (error) {
    console.error('Error confirming upload:', error);
    throw error;
  }
};
```

**Request Body:**

```javascript
const confirmationData = {
  checksum: "abc123...",  // Optional: MD5 checksum for verification
  contentType: "image/jpeg"  // Optional: actual content type
};
```

**Response:**

```javascript
{
  "code": "999999",
  "message": "Upload confirmed successfully. Media is now active.",
  "data": {
    "mediaId": 789,
    "listingId": 123,
    "userId": "456",
    "mediaType": "IMAGE",
    "sourceType": "UPLOADED",
    "status": "ACTIVE",
    "url": "https://cdn.smartrent.com/media/2025/01/15/uuid-apartment-photo.jpg",
    "thumbnailUrl": null,
    "title": "Living Room",
    "description": "Spacious living room with natural light",
    "altText": "Living room photo",
    "isPrimary": true,
    "sortOrder": 0,
    "fileSize": 2048576,
    "mimeType": "image/jpeg",
    "originalFilename": "apartment-photo.jpg",
    "uploadConfirmed": true,
    "createdAt": "2025-01-15T10:30:00Z",
    "updatedAt": "2025-01-15T10:31:00Z"
  }
}
```

### 4.5 Complete Upload Function

```javascript
const uploadImage = async (file, listingId, imageMetadata = {}) => {
  try {
    console.log('Starting upload process for:', file.name);

    // Step 1: Generate upload URL
    const fileInfo = {
      mediaType: file.type.startsWith('video/') ? 'VIDEO' : 'IMAGE',
      filename: file.name,
      contentType: file.type,
      fileSize: file.size,
      listingId: listingId,
      ...imageMetadata  // title, description, altText, isPrimary, sortOrder
    };

    const uploadData = await generateUploadUrl(fileInfo);
    console.log('Upload URL generated:', uploadData.mediaId);

    // Step 2: Upload to R2
    await uploadFileToR2(file, uploadData.uploadUrl);
    console.log('File uploaded to R2');

    // Step 3: Confirm upload
    const confirmedMedia = await confirmUpload(uploadData.mediaId, {
      contentType: file.type
    });
    console.log('Upload confirmed:', confirmedMedia);

    return confirmedMedia;

  } catch (error) {
    console.error('Upload failed:', error);
    throw error;
  }
};
```

### 4.6 React Image Upload Component

```jsx
import React, { useState } from 'react';

const ImageUploader = ({ listingId, onUploadComplete }) => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});
  const [error, setError] = useState(null);

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);

    // Validate files
    const validFiles = files.filter(file => {
      // Check file type
      const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'video/mp4'];
      if (!validTypes.includes(file.type)) {
        alert(`Invalid file type: ${file.name}`);
        return false;
      }

      // Check file size (max 100MB)
      if (file.size > 100 * 1024 * 1024) {
        alert(`File too large: ${file.name} (max 100MB)`);
        return false;
      }

      return true;
    });

    setSelectedFiles(validFiles);
  };

  const uploadFiles = async () => {
    if (selectedFiles.length === 0) {
      alert('Please select files to upload');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const uploadedMedia = [];

      for (let i = 0; i < selectedFiles.length; i++) {
        const file = selectedFiles[i];

        // Update progress
        setUploadProgress(prev => ({
          ...prev,
          [file.name]: 'uploading'
        }));

        // Upload file
        const metadata = {
          isPrimary: i === 0,  // First image is primary
          sortOrder: i,
          title: file.name.split('.')[0]  // Use filename without extension
        };

        const result = await uploadImage(file, listingId, metadata);
        uploadedMedia.push(result);

        // Update progress
        setUploadProgress(prev => ({
          ...prev,
          [file.name]: 'completed'
        }));
      }

      // Notify parent component
      if (onUploadComplete) {
        onUploadComplete(uploadedMedia);
      }

      // Reset form
      setSelectedFiles([]);
      setUploadProgress({});
      alert('All files uploaded successfully!');

    } catch (err) {
      console.error('Upload error:', err);
      setError(err.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="image-uploader">
      <h3>Upload Images</h3>

      {error && <div className="error-message">{error}</div>}

      <div className="file-input">
        <input
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp,video/mp4"
          onChange={handleFileSelect}
          disabled={uploading}
        />
      </div>

      {selectedFiles.length > 0 && (
        <div className="file-list">
          <h4>Selected Files:</h4>
          <ul>
            {selectedFiles.map((file, index) => (
              <li key={index}>
                {file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)
                {uploadProgress[file.name] && (
                  <span className="status"> - {uploadProgress[file.name]}</span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      <button
        onClick={uploadFiles}
        disabled={uploading || selectedFiles.length === 0}
      >
        {uploading ? 'Uploading...' : 'Upload Files'}
      </button>
    </div>
  );
};

export default ImageUploader;
```

### 4.7 Upload Multiple Images for Listing

```jsx
// Usage in CreateListingForm
const CreateListingForm = () => {
  const [listingId, setListingId] = useState(null);
  const [uploadedImages, setUploadedImages] = useState([]);

  const handleListingCreated = async (listing) => {
    // Listing created, now upload images
    setListingId(listing.listingId);
  };

  const handleImagesUploaded = (images) => {
    setUploadedImages(images);
    console.log('All images uploaded:', images);
    // Navigate to listing page or show success message
  };

  return (
    <div>
      {!listingId ? (
        <CreateListingForm onSuccess={handleListingCreated} />
      ) : (
        <ImageUploader
          listingId={listingId}
          onUploadComplete={handleImagesUploaded}
        />
      )}
    </div>
  );
};
```

### 4.8 External Media (YouTube/TikTok)

**Endpoint:** `POST /v1/media/external`

```javascript
const saveExternalMedia = async (videoUrl, listingId) => {
  try {
    const token = localStorage.getItem('accessToken');

    const response = await fetch('http://localhost:8080/v1/media/external', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        url: videoUrl,
        listingId: listingId,
        title: "Property Tour Video",
        description: "Video tour of the property",
        isPrimary: false,
        sortOrder: 10
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    const result = await response.json();
    return result.data;

  } catch (error) {
    console.error('Error saving external media:', error);
    throw error;
  }
};

// Usage
const youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
const tiktokUrl = "https://www.tiktok.com/@user/video/1234567890";

saveExternalMedia(youtubeUrl, 123);
```

---

## 5. Complete Integration Flow

### 5.1 Full Flow Example

```jsx
import React, { useState } from 'react';
import AddressSelector from './AddressSelector';
import ImageUploader from './ImageUploader';

const CompleteListingCreation = () => {
  const [step, setStep] = useState(1);
  const [listingData, setListingData] = useState({});
  const [addressData, setAddressData] = useState(null);
  const [createdListing, setCreatedListing] = useState(null);
  const [uploadedMedia, setUploadedMedia] = useState([]);

  // Step 1: Basic listing information
  const handleBasicInfoSubmit = (data) => {
    setListingData(data);
    setStep(2);
  };

  // Step 2: Address selection
  const handleAddressComplete = (address) => {
    setAddressData(address);
  };

  const handleAddressSubmit = async () => {
    if (!addressData) {
      alert('Please complete address selection');
      return;
    }

    try {
      // Create listing with address
      const requestBody = {
        ...listingData,
        address: addressData
      };

      const listing = await createListing(requestBody);
      setCreatedListing(listing);
      setStep(3);

    } catch (error) {
      alert('Error creating listing: ' + error.message);
    }
  };

  // Step 3: Image upload
  const handleMediaUploadComplete = (media) => {
    setUploadedMedia(media);
    setStep(4);
  };

  return (
    <div className="complete-listing-creation">
      {/* Progress Indicator */}
      <div className="progress-steps">
        <div className={step >= 1 ? 'active' : ''}>1. Listing Details</div>
        <div className={step >= 2 ? 'active' : ''}>2. Address</div>
        <div className={step >= 3 ? 'active' : ''}>3. Photos</div>
        <div className={step >= 4 ? 'active' : ''}>4. Complete</div>
      </div>

      {/* Step 1: Listing Details */}
      {step === 1 && (
        <ListingBasicInfoForm onSubmit={handleBasicInfoSubmit} />
      )}

      {/* Step 2: Address Selection */}
      {step === 2 && (
        <div>
          <AddressSelector onAddressChange={handleAddressComplete} />
          <button
            onClick={handleAddressSubmit}
            disabled={!addressData}
          >
            Continue to Photos
          </button>
        </div>
      )}

      {/* Step 3: Photo Upload */}
      {step === 3 && createdListing && (
        <div>
          <h3>Upload Photos for Your Listing</h3>
          <p>Listing ID: {createdListing.listingId}</p>
          <ImageUploader
            listingId={createdListing.listingId}
            onUploadComplete={handleMediaUploadComplete}
          />
          <button onClick={() => setStep(4)}>
            Skip Photo Upload
          </button>
        </div>
      )}

      {/* Step 4: Success */}
      {step === 4 && (
        <div className="success-message">
          <h2>Listing Created Successfully!</h2>
          <p>Listing ID: {createdListing?.listingId}</p>
          <p>Address: {addressData?.fullAddress}</p>
          <p>Photos Uploaded: {uploadedMedia.length}</p>
          <button onClick={() => window.location.href = `/listings/${createdListing.listingId}`}>
            View Listing
          </button>
        </div>
      )}
    </div>
  );
};

export default CompleteListingCreation;
```

---

## 6. Error Handling

### 6.1 Common Error Codes

```javascript
const ERROR_CODES = {
  // Address Errors
  'ADDRESS_001': 'Invalid province ID',
  'ADDRESS_002': 'Invalid district ID',
  'ADDRESS_003': 'Invalid ward ID',
  'ADDRESS_004': 'Invalid street ID',

  // Listing Errors
  'LISTING_001': 'Invalid listing data',
  'LISTING_002': 'Listing not found',
  'LISTING_003': 'Unauthorized to modify listing',

  // Media Errors
  'MEDIA_001': 'Invalid file type',
  'MEDIA_002': 'File too large',
  'MEDIA_003': 'Upload URL expired',
  'MEDIA_004': 'Upload not confirmed',
  'MEDIA_005': 'Media not found',

  // Authentication Errors
  '5001': 'Token expired',
  '5002': 'Invalid token',
  '5003': 'Unauthorized access'
};
```

### 6.2 Error Handling Example

```javascript
const handleApiError = (error) => {
  if (error.response) {
    const { code, message, data } = error.response.data;

    switch (code) {
      case '5001': // Token expired
        // Refresh token or redirect to login
        window.location.href = '/login';
        break;

      case 'MEDIA_001': // Invalid file type
        alert('Invalid file type. Please upload JPEG, PNG, or WebP images.');
        break;

      case 'MEDIA_002': // File too large
        alert('File is too large. Maximum size is 100MB.');
        break;

      default:
        alert(`Error: ${message}`);
    }
  } else {
    alert('Network error. Please check your connection.');
  }
};
```

---

## 7. Best Practices

### 7.1 Address Selection
- Always validate address completion before submission
- Cache province/district/ward lists to reduce API calls
- Implement search functionality for large lists
- Show loading states during cascading selections

### 7.2 Listing Creation
- Validate all required fields before submission
- Show progress indicator for multi-step forms
- Save draft data to localStorage for recovery
- Implement proper error handling and retry logic

### 7.3 Image Upload
- Validate file type and size before upload
- Show upload progress for better UX
- Implement retry mechanism for failed uploads
- Compress images on client-side before upload
- Allow users to reorder and set primary image
- Implement drag-and-drop functionality

### 7.4 Security
- Always include JWT token in Authorization header
- Never expose upload URLs to unauthorized users
- Validate file content type on both client and server
- Implement CSRF protection for state-changing operations

---

## 8. Testing Checklist

### 8.1 Address Flow
- [ ] Can load all provinces
- [ ] Can load districts for selected province
- [ ] Can load wards for selected district
- [ ] Can load streets for selected ward
- [ ] Full address is correctly generated
- [ ] Selections reset properly when changing parent level
- [ ] Search functionality works correctly

### 8.2 Listing Creation
- [ ] All required fields are validated
- [ ] Address integration works correctly
- [ ] Listing is created with correct data
- [ ] Error messages are displayed properly
- [ ] Success confirmation is shown
- [ ] Navigation works after creation

### 8.3 Image Upload
- [ ] File validation works (type, size)
- [ ] Upload URL generation succeeds
- [ ] File uploads to R2 successfully
- [ ] Upload confirmation succeeds
- [ ] Multiple files can be uploaded
- [ ] Primary image is set correctly
- [ ] Upload progress is shown
- [ ] Error handling works properly

---

## 9. API Reference Quick Links

| Feature | Endpoint | Method | Auth Required |
|---------|----------|--------|---------------|
| Get Provinces | `/v1/addresses/provinces` | GET | No |
| Get Districts | `/v1/addresses/provinces/{id}/districts` | GET | No |
| Get Wards | `/v1/addresses/districts/{id}/wards` | GET | No |
| Get Streets | `/v1/addresses/wards/{id}/streets` | GET | No |
| Create Listing | `/v1/listings` | POST | Yes |
| Generate Upload URL | `/v1/media/upload-url` | POST | Yes |
| Confirm Upload | `/v1/media/{id}/confirm` | POST | Yes |
| Save External Media | `/v1/media/external` | POST | Yes |
| Get Listing Media | `/v1/media/listing/{id}` | GET | No |

---

## 10. Support

For additional help:
- Check Swagger documentation: `http://localhost:8080/swagger-ui.html`
- View error codes: `docs/ERROR_CODES.md`
- Contact API support: `api-support@smartrent.com`

---

**Last Updated:** January 2025
**Version:** 1.0.0
