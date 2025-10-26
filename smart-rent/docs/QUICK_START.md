# SmartRent API Quick Start Guide

Quick reference for getting started with SmartRent API integration.

---

## 📚 Documentation Index

1. **[INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md)** - Detailed step-by-step implementation guide
2. **[FLOW_DIAGRAMS.md](./FLOW_DIAGRAMS.md)** - Visual flow diagrams and data flow
3. **[QUICK_START.md](./QUICK_START.md)** - This file (quick reference)

---

## 🚀 Quick Start Checklist

### Backend Setup

- [ ] **Database Migration**
  ```bash
  ./gradlew flywayMigrate
  ./gradlew flywayInfo  # Verify migrations
  ```

- [ ] **Start Application**
  ```bash
  ./gradlew bootRun --args='--spring.profiles.active=local'
  ```

- [ ] **Verify API**
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - API Base URL: `http://localhost:8080/v1`

### Frontend Setup

- [ ] **Install Dependencies**
  ```bash
  npm install axios  # or your preferred HTTP client
  ```

- [ ] **Configure API Base URL**
  ```javascript
  const API_BASE_URL = 'http://localhost:8080/v1';
  ```

- [ ] **Set Up Authentication**
  ```javascript
  // Store JWT token after login
  localStorage.setItem('accessToken', token);

  // Add to all authenticated requests
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
  ```

---

## 📍 Address Selection (5-Minute Setup)

### Step 1: Create AddressSelector Component

```jsx
// components/AddressSelector.jsx
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
    fetch('http://localhost:8080/v1/addresses/provinces')
      .then(res => res.json())
      .then(data => setProvinces(data.data));
  }, []);

  // Load districts when province changes
  useEffect(() => {
    if (selectedProvince) {
      fetch(`http://localhost:8080/v1/addresses/provinces/${selectedProvince.id}/districts`)
        .then(res => res.json())
        .then(data => setDistricts(data.data));
    }
  }, [selectedProvince]);

  // Load wards when district changes
  useEffect(() => {
    if (selectedDistrict) {
      fetch(`http://localhost:8080/v1/addresses/districts/${selectedDistrict.id}/wards`)
        .then(res => res.json())
        .then(data => setWards(data.data));
    }
  }, [selectedDistrict]);

  // Load streets when ward changes
  useEffect(() => {
    if (selectedWard) {
      fetch(`http://localhost:8080/v1/addresses/wards/${selectedWard.id}/streets`)
        .then(res => res.json())
        .then(data => setStreets(data.data));
    }
  }, [selectedWard]);

  // Notify parent when complete
  useEffect(() => {
    if (selectedStreet) {
      onAddressChange({
        streetNumber,
        streetId: selectedStreet.id,
        wardId: selectedWard.id,
        districtId: selectedDistrict.id,
        provinceId: selectedProvince.id,
        fullAddress: buildFullAddress()
      });
    }
  }, [selectedStreet, streetNumber]);

  const buildFullAddress = () => {
    const parts = [streetNumber, selectedStreet?.name, selectedWard?.name,
                   selectedDistrict?.name, selectedProvince?.name].filter(Boolean);
    return parts.join(', ');
  };

  return (
    <div>
      <select onChange={(e) => setSelectedProvince(provinces.find(p => p.id === +e.target.value))}>
        <option value="">Select Province</option>
        {provinces.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select>

      {selectedProvince && (
        <select onChange={(e) => setSelectedDistrict(districts.find(d => d.id === +e.target.value))}>
          <option value="">Select District</option>
          {districts.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
        </select>
      )}

      {selectedDistrict && (
        <select onChange={(e) => setSelectedWard(wards.find(w => w.id === +e.target.value))}>
          <option value="">Select Ward</option>
          {wards.map(w => <option key={w.id} value={w.id}>{w.name}</option>)}
        </select>
      )}

      {selectedWard && (
        <select onChange={(e) => setSelectedStreet(streets.find(s => s.id === +e.target.value))}>
          <option value="">Select Street</option>
          {streets.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
      )}

      {selectedStreet && (
        <input
          type="text"
          placeholder="Street Number (optional)"
          value={streetNumber}
          onChange={(e) => setStreetNumber(e.target.value)}
        />
      )}

      {selectedStreet && <p>Full Address: {buildFullAddress()}</p>}
    </div>
  );
};

export default AddressSelector;
```

---

## 🏠 Create Listing (10-Minute Setup)

### Step 2: Create Listing Form

```jsx
// components/CreateListingForm.jsx
import React, { useState } from 'react';
import AddressSelector from './AddressSelector';

const CreateListingForm = () => {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    listingType: 'RENT',
    productType: 'APARTMENT',
    price: '',
    priceUnit: 'MONTH',
    area: '',
    bedrooms: 1,
    bathrooms: 1
  });
  const [addressData, setAddressData] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const response = await fetch('http://localhost:8080/v1/listings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({
        ...formData,
        price: parseFloat(formData.price),
        area: parseFloat(formData.area),
        address: addressData
      })
    });

    const result = await response.json();
    console.log('Listing created:', result.data);
    alert(`Listing created! ID: ${result.data.listingId}`);
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2>Create New Listing</h2>

      <input
        type="text"
        placeholder="Title"
        value={formData.title}
        onChange={(e) => setFormData({...formData, title: e.target.value})}
        required
      />

      <textarea
        placeholder="Description"
        value={formData.description}
        onChange={(e) => setFormData({...formData, description: e.target.value})}
        required
      />

      <select value={formData.listingType}
              onChange={(e) => setFormData({...formData, listingType: e.target.value})}>
        <option value="RENT">For Rent</option>
        <option value="SALE">For Sale</option>
      </select>

      <input
        type="number"
        placeholder="Price (VND)"
        value={formData.price}
        onChange={(e) => setFormData({...formData, price: e.target.value})}
        required
      />

      <input
        type="number"
        placeholder="Area (m²)"
        value={formData.area}
        onChange={(e) => setFormData({...formData, area: e.target.value})}
        required
      />

      <AddressSelector onAddressChange={setAddressData} />

      <button type="submit" disabled={!addressData}>Create Listing</button>
    </form>
  );
};

export default CreateListingForm;
```

---

## 📸 Image Upload (10-Minute Setup)

### Step 3: Create Image Uploader

```jsx
// components/ImageUploader.jsx
import React, { useState } from 'react';

const ImageUploader = ({ listingId }) => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [uploading, setUploading] = useState(false);

  const uploadImage = async (file) => {
    // Step 1: Generate upload URL
    const generateRes = await fetch('http://localhost:8080/v1/media/upload-url', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({
        mediaType: 'IMAGE',
        filename: file.name,
        contentType: file.type,
        fileSize: file.size,
        listingId: listingId,
        isPrimary: false,
        sortOrder: 0
      })
    });
    const generateData = await generateRes.json();
    const { mediaId, uploadUrl } = generateData.data;

    // Step 2: Upload to R2
    await fetch(uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type },
      body: file
    });

    // Step 3: Confirm upload
    const confirmRes = await fetch(`http://localhost:8080/v1/media/${mediaId}/confirm`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({ contentType: file.type })
    });

    return confirmRes.json();
  };

  const handleUpload = async () => {
    setUploading(true);
    try {
      for (const file of selectedFiles) {
        await uploadImage(file);
        console.log(`Uploaded: ${file.name}`);
      }
      alert('All images uploaded successfully!');
      setSelectedFiles([]);
    } catch (error) {
      alert('Upload failed: ' + error.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h3>Upload Images</h3>
      <input
        type="file"
        multiple
        accept="image/jpeg,image/png,image/webp"
        onChange={(e) => setSelectedFiles(Array.from(e.target.files))}
      />

      {selectedFiles.length > 0 && (
        <div>
          <p>Selected: {selectedFiles.length} file(s)</p>
          <button onClick={handleUpload} disabled={uploading}>
            {uploading ? 'Uploading...' : 'Upload'}
          </button>
        </div>
      )}
    </div>
  );
};

export default ImageUploader;
```

---

## 🔗 Complete Integration Example

```jsx
// App.jsx
import React, { useState } from 'react';
import CreateListingForm from './components/CreateListingForm';
import ImageUploader from './components/ImageUploader';

const App = () => {
  const [listingId, setListingId] = useState(null);

  return (
    <div>
      {!listingId ? (
        <CreateListingForm onSuccess={(listing) => setListingId(listing.listingId)} />
      ) : (
        <>
          <h2>Listing Created! ID: {listingId}</h2>
          <ImageUploader listingId={listingId} />
        </>
      )}
    </div>
  );
};

export default App;
```

---

## 📝 API Endpoints Cheat Sheet

### Authentication
```
POST /v1/auth              Login (user)
POST /v1/auth/admin        Login (admin)
POST /v1/auth/refresh      Refresh token
```

### Address Selection
```
GET  /v1/addresses/provinces                    List provinces
GET  /v1/addresses/provinces/{id}/districts     List districts
GET  /v1/addresses/districts/{id}/wards         List wards
GET  /v1/addresses/wards/{id}/streets           List streets
```

### Listing Management
```
POST /v1/listings          Create listing (with address)
GET  /v1/listings          List all listings
GET  /v1/listings/{id}     Get listing details
PUT  /v1/listings/{id}     Update listing
DEL  /v1/listings/{id}     Delete listing
```

### Media Upload
```
POST /v1/media/upload-url        Generate upload URL
POST /v1/media/{id}/confirm      Confirm upload
POST /v1/media/external          Save YouTube/TikTok
GET  /v1/media/listing/{id}      Get listing media
GET  /v1/media/{id}/download-url Get download URL
DEL  /v1/media/{id}              Delete media
```

---

## 🔒 Authentication Flow

```javascript
// 1. Login
const loginResponse = await fetch('http://localhost:8080/v1/auth', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
});

const { accessToken, refreshToken } = (await loginResponse.json()).data;

// 2. Store tokens
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// 3. Use in requests
const headers = {
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
  'Content-Type': 'application/json'
};
```

---

## ⚠️ Common Errors & Solutions

| Error Code | Message | Solution |
|------------|---------|----------|
| `5001` | Token expired | Refresh token or re-login |
| `MEDIA_001` | Invalid file type | Use JPEG/PNG/WebP only |
| `MEDIA_002` | File too large | Max 100MB per file |
| `MEDIA_003` | Upload URL expired | Generate new URL (30 min limit) |
| `ADDRESS_001-004` | Invalid address ID | Re-select address |
| `LISTING_003` | Unauthorized | Check token & ownership |

---

## 🧪 Testing Your Integration

### Test Address Selection
```bash
# 1. Get provinces
curl http://localhost:8080/v1/addresses/provinces

# 2. Get districts (province_id=1)
curl http://localhost:8080/v1/addresses/provinces/1/districts

# 3. Get wards (district_id=1)
curl http://localhost:8080/v1/addresses/districts/1/wards

# 4. Get streets (ward_id=1)
curl http://localhost:8080/v1/addresses/wards/1/streets
```

### Test Listing Creation
```bash
curl -X POST http://localhost:8080/v1/listings \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Apartment",
    "description": "Beautiful apartment",
    "listingType": "RENT",
    "vipType": "NORMAL",
    "productType": "APARTMENT",
    "price": 12000000,
    "priceUnit": "MONTH",
    "area": 78.5,
    "bedrooms": 2,
    "bathrooms": 1,
    "address": {
      "streetNumber": "123",
      "streetId": 1,
      "wardId": 1,
      "districtId": 1,
      "provinceId": 1,
      "fullAddress": "123 Street, Ward, District, Province"
    }
  }'
```

---

## 📊 Response Format

All API responses follow this format:

```json
{
  "code": "999999",
  "message": "Success message",
  "data": {
    // Response data
  }
}
```

**Success:** `code: "999999"`
**Error:** `code: "1xxx" | "2xxx" | "3xxx" | "4xxx" | "5xxx"`

---

## 🎯 Next Steps

1. ✅ Follow this Quick Start guide
2. 📖 Read [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md) for detailed implementation
3. 🎨 Check [FLOW_DIAGRAMS.md](./FLOW_DIAGRAMS.md) for visual flows
4. 🔍 Explore Swagger UI: `http://localhost:8080/swagger-ui.html`
5. 📧 Contact support: `api-support@smartrent.com`

---

## 📚 Additional Resources

- **Swagger Documentation:** http://localhost:8080/swagger-ui.html
- **Error Codes Reference:** `docs/ERROR_CODES.md`
- **Database Schema:** `docs/DATABASE_SCHEMA.md`
- **Architecture Overview:** `CLAUDE.md`

---

**Last Updated:** January 2025
**Version:** 1.0.0