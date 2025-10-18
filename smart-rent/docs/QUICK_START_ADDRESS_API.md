# Quick Start: Address API Integration

## TL;DR

You have an **internal address API** with Vietnamese location data. No external API calls needed!

## 3-Step Setup

### 1. Import Location Data

```bash
# Run the data fetcher script
node scripts/fetch-tinhthanhpho-data.js

# Apply migration
./gradlew flywayMigrate

# Verify (should see 63/~700/~11000)
mysql> SELECT
  (SELECT COUNT(*) FROM provinces) as provinces,
  (SELECT COUNT(*) FROM districts) as districts,
  (SELECT COUNT(*) FROM wards) as wards;
```

### 2. Test API

```bash
# Get provinces
curl http://localhost:8080/v1/addresses/provinces

# Get districts for Hanoi (provinceId=1)
curl http://localhost:8080/v1/addresses/provinces/1/districts

# Get wards for Ba Dinh (districtId=1)
curl http://localhost:8080/v1/addresses/districts/1/wards
```

### 3. Create Listing with Address

```javascript
// Frontend code
// Step 1: Create address
const address = await fetch('/v1/addresses', {
  method: 'POST',
  body: JSON.stringify({
    streetNumber: "123",
    streetId: 1,
    wardId: 1,
    districtId: 1,
    provinceId: 1
  })
}).then(r => r.json());

// Step 2: Create listing with addressId
const listing = await fetch('/v1/listings', {
  method: 'POST',
  body: JSON.stringify({
    title: "Cho thuê căn hộ 2PN",
    addressId: address.data.addressId,  // ← Use this
    price: 10000000,
    // ... other fields
  })
}).then(r => r.json());
```

## Key API Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /v1/addresses/provinces` | List all provinces |
| `GET /v1/addresses/provinces/{id}/districts` | Get districts |
| `GET /v1/addresses/districts/{id}/wards` | Get wards |
| `GET /v1/addresses/wards/{id}/streets` | Get streets |
| `POST /v1/addresses` | Create address |
| `GET /v1/addresses/suggest?q={text}` | Autocomplete |

## Files Created/Modified

### New Files
- ✅ `V18__Import_tinhthanhpho_location_data.sql` - Migration template
- ✅ `docs/TINHTHANHPHO_DATA_IMPORT_GUIDE.md` - Import guide
- ✅ `docs/INTERNAL_ADDRESS_API.md` - Full documentation
- ✅ `docs/QUICK_START_ADDRESS_API.md` - This file

### Modified Files
- ✅ `ProvinceRepository.java` - Added query methods
- ✅ `DistrictRepository.java` - Added query methods
- ✅ `WardRepository.java` - Added query methods
- ✅ `AddressController.java` - Cleaned up (removed external API endpoints)
- ✅ `DomainCode.java` - Added address error codes

### Removed Files
- ❌ External API connectors (TinhThanhPhoConnector, etc.)
- ❌ AddressLocationService (no longer needed)

## Frontend Integration Pattern

```jsx
// React component example
function AddressSelector({ onAddressSelect }) {
  const [provinces, setProvinces] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [wards, setWards] = useState([]);
  const [streets, setStreets] = useState([]);

  // Load provinces on mount
  useEffect(() => {
    fetch('/v1/addresses/provinces')
      .then(r => r.json())
      .then(data => setProvinces(data.data));
  }, []);

  // Load districts when province selected
  const handleProvinceChange = (provinceId) => {
    fetch(`/v1/addresses/provinces/${provinceId}/districts`)
      .then(r => r.json())
      .then(data => setDistricts(data.data));
  };

  // Load wards when district selected
  const handleDistrictChange = (districtId) => {
    fetch(`/v1/addresses/districts/${districtId}/wards`)
      .then(r => r.json())
      .then(data => setWards(data.data));
  };

  // Load streets when ward selected
  const handleWardChange = (wardId) => {
    fetch(`/v1/addresses/wards/${wardId}/streets`)
      .then(r => r.json())
      .then(data => setStreets(data.data));
  };

  return (
    <>
      <select onChange={(e) => handleProvinceChange(e.target.value)}>
        {provinces.map(p => <option key={p.provinceId} value={p.provinceId}>{p.name}</option>)}
      </select>
      <select onChange={(e) => handleDistrictChange(e.target.value)}>
        {districts.map(d => <option key={d.districtId} value={d.districtId}>{d.name}</option>)}
      </select>
      <select onChange={(e) => handleWardChange(e.target.value)}>
        {wards.map(w => <option key={w.wardId} value={w.wardId}>{w.name}</option>)}
      </select>
      <select>
        {streets.map(s => <option key={s.streetId} value={s.streetId}>{s.name}</option>)}
      </select>
      <input type="text" placeholder="Street number" />
    </>
  );
}
```

## Common Questions

**Q: Do I need to call external APIs?**
A: No! All data is in your local database.

**Q: How do I update location data?**
A: Re-run the data fetcher script and create a new migration.

**Q: Can listings have addresses without streets?**
A: Yes, street is optional. Just use ward-level location.

**Q: How do I implement autocomplete?**
A: Use `GET /v1/addresses/suggest?q=user-input`

**Q: What if a street doesn't exist in the database?**
A: You can add streets manually or allow users to enter free text.

## Next Steps

1. ✅ Import data (see step 1 above)
2. ✅ Test API endpoints
3. 📝 Implement frontend address selector
4. 📝 Update listing creation form to use addressId
5. 📝 Add address autocomplete for better UX

## Need Help?

- Full docs: `docs/INTERNAL_ADDRESS_API.md`
- Import guide: `docs/TINHTHANHPHO_DATA_IMPORT_GUIDE.md`
- API Explorer: http://localhost:8080/swagger-ui.html