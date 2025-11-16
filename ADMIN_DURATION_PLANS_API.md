# Admin API: Duration Plans Management

## Overview
Admin-only APIs for CRUD operations on listing duration plans. Allows admins to create custom duration plans, activate/deactivate plans, and manage the pricing structure.

## Base URL
```
/v1/admin/duration-plans
```

## Authentication
All endpoints require admin authentication via `X-Admin-Id` header.

---

## Endpoints

### 1. Get All Plans (Including Inactive)
```http
GET /v1/admin/duration-plans
```

**Headers:**
```
X-Admin-Id: admin-uuid
```

**Response:**
```json
{
  "code": "200000",
  "message": "All duration plans retrieved successfully",
  "data": [
    {
      "planId": 1,
      "durationDays": 5,
      "isActive": true,
      "discountPercentage": 0,
      "discountDescription": "No discount",
      "normalPrice": 13500,
      "silverPrice": 250000,
      "goldPrice": 550000,
      "diamondPrice": 1400000
    },
    {
      "planId": 6,
      "durationDays": 45,
      "isActive": false,
      "discountPercentage": 0.185,
      "discountDescription": "18.5% off",
      "normalPrice": 99225,
      "silverPrice": 1833750,
      "goldPrice": 4034250,
      "diamondPrice": 10269000
    }
  ]
}
```

---

### 2. Get Plan by ID
```http
GET /v1/admin/duration-plans/{planId}
```

**Headers:**
```
X-Admin-Id: admin-uuid
```

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan retrieved successfully",
  "data": {
    "planId": 1,
    "durationDays": 5,
    "isActive": true,
    "discountPercentage": 0,
    "discountDescription": "No discount",
    "normalPrice": 13500,
    "silverPrice": 250000,
    "goldPrice": 550000,
    "diamondPrice": 1400000
  }
}
```

---

### 3. Create New Plan
```http
POST /v1/admin/duration-plans
```

**Headers:**
```
X-Admin-Id: admin-uuid
Content-Type: application/json
```

**Request Body:**
```json
{
  "durationDays": 45,
  "isActive": true
}
```

**Validation:**
- `durationDays`: Required, minimum 1
- `isActive`: Optional, defaults to `true`
- Duration must be unique (no duplicates)

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan created successfully",
  "data": {
    "planId": 6,
    "durationDays": 45,
    "isActive": true,
    "discountPercentage": 0.185,
    "discountDescription": "18.5% off",
    "normalPrice": 99225,
    "silverPrice": 1833750,
    "goldPrice": 4034250,
    "diamondPrice": 10269000
  }
}
```

**Error Cases:**
```json
// Duplicate duration
{
  "code": "400000",
  "message": "Duration plan already exists for 45 days"
}
```

---

### 4. Update Plan
```http
PUT /v1/admin/duration-plans/{planId}
```

**Headers:**
```
X-Admin-Id: admin-uuid
Content-Type: application/json
```

**Request Body:**
```json
{
  "durationDays": 60,
  "isActive": true
}
```

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan updated successfully",
  "data": {
    "planId": 6,
    "durationDays": 60,
    "isActive": true,
    "discountPercentage": 0.185,
    "discountDescription": "18.5% off",
    "normalPrice": 132300,
    "silverPrice": 2445000,
    "goldPrice": 5379000,
    "diamondPrice": 13692000
  }
}
```

**Error Cases:**
```json
// Plan not found
{
  "code": "404000",
  "message": "Duration plan not found with ID: 99"
}

// Duplicate duration conflict
{
  "code": "400000",
  "message": "Another plan already exists for 60 days"
}
```

---

### 5. Delete Plan (Soft Delete)
```http
DELETE /v1/admin/duration-plans/{planId}
```

**Headers:**
```
X-Admin-Id: admin-uuid
```

**Behavior:**
- Soft delete by setting `isActive = false`
- Plan remains in database but hidden from public APIs
- Cannot delete if it's the last active plan

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan deleted successfully"
}
```

**Error Cases:**
```json
// Last active plan
{
  "code": "400000",
  "message": "Cannot delete the last active duration plan. System must have at least one active plan."
}

// Plan not found
{
  "code": "404000",
  "message": "Duration plan not found with ID: 99"
}
```

---

### 6. Activate Plan
```http
PATCH /v1/admin/duration-plans/{planId}/activate
```

**Headers:**
```
X-Admin-Id: admin-uuid
```

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan activated successfully",
  "data": {
    "planId": 6,
    "durationDays": 45,
    "isActive": true,
    "discountPercentage": 0.185,
    "discountDescription": "18.5% off",
    "normalPrice": 99225,
    "silverPrice": 1833750,
    "goldPrice": 4034250,
    "diamondPrice": 10269000
  }
}
```

---

### 7. Deactivate Plan
```http
PATCH /v1/admin/duration-plans/{planId}/deactivate
```

**Headers:**
```
X-Admin-Id: admin-uuid
```

**Behavior:**
- Sets `isActive = false`
- Plan hidden from public APIs
- Cannot deactivate if it's the last active plan

**Response:**
```json
{
  "code": "200000",
  "message": "Duration plan deactivated successfully",
  "data": {
    "planId": 6,
    "durationDays": 45,
    "isActive": false,
    "discountPercentage": 0.185,
    "discountDescription": "18.5% off",
    "normalPrice": 99225,
    "silverPrice": 1833750,
    "goldPrice": 4034250,
    "diamondPrice": 10269000
  }
}
```

**Error Cases:**
```json
// Last active plan
{
  "code": "400000",
  "message": "Cannot deactivate the last active duration plan. System must have at least one active plan."
}
```

---

## Common Use Cases

### 1. Create Promotional Plan (e.g., 14-day trial)
```bash
curl -X POST http://localhost:8080/v1/admin/duration-plans \
  -H "X-Admin-Id: admin-123" \
  -H "Content-Type: application/json" \
  -d '{
    "durationDays": 14,
    "isActive": true
  }'
```

### 2. Create Long-term Plan (e.g., 90 days)
```bash
curl -X POST http://localhost:8080/v1/admin/duration-plans \
  -H "X-Admin-Id: admin-123" \
  -H "Content-Type: application/json" \
  -d '{
    "durationDays": 90,
    "isActive": true
  }'
```

### 3. Temporarily Disable a Plan
```bash
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/6/deactivate \
  -H "X-Admin-Id: admin-123"
```

### 4. Re-enable a Plan
```bash
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/6/activate \
  -H "X-Admin-Id: admin-123"
```

### 5. Update Plan Duration
```bash
curl -X PUT http://localhost:8080/v1/admin/duration-plans/6 \
  -H "X-Admin-Id: admin-123" \
  -H "Content-Type: application/json" \
  -d '{
    "durationDays": 60,
    "isActive": true
  }'
```

---

## Business Rules

### Discount Calculation
Discounts are automatically calculated based on duration:
- **5-10 days**: 0% discount
- **15 days**: 11% discount
- **30 days**: 18.5% discount
- **Custom durations**: Use 30-day discount logic (18.5%) if >= 30 days, otherwise no discount

### Active Plan Constraint
- System must have **at least one active plan** at all times
- Cannot delete or deactivate the last active plan
- This ensures users always have options when creating listings

### Duration Uniqueness
- Each duration (in days) must be unique across all plans
- Cannot create two plans with the same duration
- When updating, new duration must not conflict with existing plans

### Soft Delete
- Plans are never hard-deleted from database
- Deleted plans are marked `isActive = false`
- This preserves historical data and allows reactivation

---

## Impact on Public APIs

### Public Listing Creation API
```
GET /v1/listings/duration-plans
```
- Only returns plans where `isActive = true`
- Users cannot select inactive plans
- Deactivating a plan immediately hides it from users

### Price Calculator API
```
GET /v1/listings/calculate-price?vipType=SILVER&durationDays=45
```
- Works for ANY duration (not limited to active plans)
- Useful for custom pricing calculations
- Admin can test pricing before creating plan

---

## Testing Guide

### 1. Create Custom Plan
```bash
# Create 14-day plan
curl -X POST http://localhost:8080/v1/admin/duration-plans \
  -H "X-Admin-Id: admin-123" \
  -H "Content-Type: application/json" \
  -d '{"durationDays": 14, "isActive": true}'

# Verify it appears in public API
curl http://localhost:8080/v1/listings/duration-plans
```

### 2. Test Active/Inactive Logic
```bash
# Deactivate plan
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/6/deactivate \
  -H "X-Admin-Id: admin-123"

# Verify it's hidden from public API
curl http://localhost:8080/v1/listings/duration-plans

# Verify admin can still see it
curl http://localhost:8080/v1/admin/duration-plans \
  -H "X-Admin-Id: admin-123"

# Reactivate plan
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/6/activate \
  -H "X-Admin-Id: admin-123"
```

### 3. Test Last Active Plan Protection
```bash
# Deactivate all but one plan
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/1/deactivate \
  -H "X-Admin-Id: admin-123"
# ... repeat for other plans

# Try to deactivate last plan (should fail)
curl -X PATCH http://localhost:8080/v1/admin/duration-plans/5/deactivate \
  -H "X-Admin-Id: admin-123"

# Expected error: "Cannot deactivate the last active duration plan"
```

### 4. Test Duplicate Prevention
```bash
# Create plan with existing duration (should fail)
curl -X POST http://localhost:8080/v1/admin/duration-plans \
  -H "X-Admin-Id: admin-123" \
  -H "Content-Type: application/json" \
  -d '{"durationDays": 30, "isActive": true}'

# Expected error: "Duration plan already exists for 30 days"
```

---

## Security Considerations

### Admin-Only Access
- All endpoints require `X-Admin-Id` header
- Should be protected by admin authentication middleware
- Regular users should not have access to these APIs

### Validation
- ✅ Duration must be positive integer
- ✅ Duration must be unique
- ✅ Cannot delete/deactivate last active plan
- ✅ Plan ID must exist for updates

### Audit Logging
Consider adding audit logs for:
- Plan creation/updates (who, when, what changed)
- Activation/deactivation events
- Failed attempts to modify plans

---

## Database Schema

```sql
CREATE TABLE listing_duration_plans (
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    duration_days INT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## Monitoring & Metrics

### Recommended Metrics
1. **Active Plans Count**: Monitor number of active plans
2. **Plan Usage**: Track which plans are most popular with users
3. **Admin Activity**: Log plan modifications by admins
4. **Inactive Plans**: Alert if too many plans are inactive

### Example Query: Most Popular Plans
```sql
SELECT
    ldp.duration_days,
    COUNT(l.listing_id) as usage_count
FROM listing_duration_plans ldp
LEFT JOIN listings l ON l.duration_plan_id = ldp.plan_id
WHERE l.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY ldp.duration_days
ORDER BY usage_count DESC;
```

---

## Future Enhancements

### Potential Features
1. **Custom Discounts**: Admin can override auto-calculated discounts
2. **Promotional Pricing**: Temporary price overrides for specific plans
3. **Plan Scheduling**: Auto-activate/deactivate plans on specific dates
4. **Plan Categories**: Group plans (e.g., "Promotional", "Standard", "Premium")
5. **Usage Limits**: Cap how many times a plan can be used
6. **Tier-Specific Plans**: Different plans for different VIP tiers

---

## Support

For questions or issues with admin APIs:
- Check logs for admin actions: `grep "Admin.*duration plan" logs/application.log`
- Verify admin authentication is working
- Check database state: `SELECT * FROM listing_duration_plans;`
- Review implementation: `DurationPlanServiceImpl.java`
