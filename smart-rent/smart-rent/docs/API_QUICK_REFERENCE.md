# Address API - Quick Reference Card

## 🚀 Quick Start

**Base URL**: `http://localhost:8080`
**Authentication**: ❌ **NOT REQUIRED** (All endpoints are public)

---

## 📍 Legacy Structure Endpoints (63 provinces)

| Endpoint | Method | Description | Example |
|----------|--------|-------------|---------|
| `/v1/addresses/provinces` | GET | Get all provinces | - |
| `/v1/addresses/provinces/{id}` | GET | Get province by ID | `/v1/addresses/provinces/1` |
| `/v1/addresses/provinces/search` | GET | Search provinces | `?q=Hà Nội` |
| `/v1/addresses/provinces/{id}/districts` | GET | Get districts | `/v1/addresses/provinces/1/districts` |
| `/v1/addresses/districts/{id}` | GET | Get district by ID | `/v1/addresses/districts/1` |
| `/v1/addresses/districts/search` | GET | Search districts | `?q=Ba Đình&provinceId=1` |
| `/v1/addresses/districts/{id}/wards` | GET | Get wards | `/v1/addresses/districts/1/wards` |
| `/v1/addresses/wards/{id}` | GET | Get ward by ID | `/v1/addresses/wards/1` |
| `/v1/addresses/wards/search` | GET | Search wards | `?q=Phúc Xá&districtId=1` |

---

## 🆕 New Structure Endpoints (37 provinces)

| Endpoint | Method | Description | Example |
|----------|--------|-------------|---------|
| `/v1/addresses/new-provinces` | GET | Get provinces (paginated) | `?page=1&limit=20` |
| `/v1/addresses/new-provinces/{code}/wards` | GET | Get wards (paginated) | `/01/wards?page=1&limit=50` |
| `/v1/addresses/new-full-address` | GET | Get full address | `?provinceCode=01&wardCode=00004` |
| `/v1/addresses/search-new-address` | GET | Search addresses | `?keyword=Hà Nội&page=1&limit=20` |

---

## 🏥 Utility Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/addresses/health` | GET | API health check |

---

## 💡 Common Examples

### Get all provinces (Legacy)
```bash
GET http://localhost:8080/v1/addresses/provinces
```

### Search provinces
```bash
GET http://localhost:8080/v1/addresses/provinces/search?q=Hà Nội
```

### Get districts of Hanoi
```bash
GET http://localhost:8080/v1/addresses/provinces/1/districts
```

### Get wards of Ba Dinh district
```bash
GET http://localhost:8080/v1/addresses/districts/1/wards
```

### Get new provinces (paginated)
```bash
GET http://localhost:8080/v1/addresses/new-provinces?page=1&limit=20
```

### Get wards of Hanoi (new structure)
```bash
GET http://localhost:8080/v1/addresses/new-provinces/01/wards?page=1&limit=50
```

### Search new addresses
```bash
GET http://localhost:8080/v1/addresses/search-new-address?keyword=Ba Đình&page=1&limit=10
```

---

## 🔧 Postman Environment Variables

```json
{
  "baseUrl": "http://localhost:8080",
  "provinceId": "1",
  "districtId": "1",
  "wardId": "1",
  "provinceCode": "01",
  "wardCode": "00004"
}
```

---

## ✅ Success Response Format

### Legacy Endpoints
```json
{
  "data": { ... },
  "message": "Success message"
}
```

### New Endpoints (Paginated)
```json
{
  "success": true,
  "message": "Success",
  "data": [ ... ],
  "metadata": {
    "total": 100,
    "page": 1,
    "limit": 20
  }
}
```

---

## ❌ Error Response Format

```json
{
  "code": "4009",
  "message": "Resource not found: Province with code: 99",
  "data": null,
  "success": false
}
```

---

## 📚 Full Documentation

- **Complete Postman Guide**: `docs/POSTMAN_TESTING_GUIDE.md`
- **Security Configuration**: `docs/SECURITY_CHECK_REPORT.md`
- **DTO Reference**: `docs/DTO_REFERENCE.md`
- **Swagger UI**: http://localhost:8080/swagger-ui.html

---

**Quick Tip**: Import the Postman collection from `POSTMAN_TESTING_GUIDE.md` for instant testing! 🚀