# Membership Upgrade - Frontend Integration Guide

## Overview

This guide explains how to integrate the membership upgrade feature into your frontend application. Users with an active membership can upgrade to a higher-tier package and receive a discount based on their remaining membership time.

**Important:** Users who already have an active membership cannot purchase a new membership - they must use the upgrade feature instead.

## User Flow Decision Tree

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         User wants a membership                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │  Does user have an active     │
                    │  membership?                  │
                    └───────────────────────────────┘
                           │              │
                          YES             NO
                           │              │
                           ▼              ▼
              ┌────────────────────┐  ┌────────────────────┐
              │  Show "Upgrade"    │  │  Show "Purchase"   │
              │  options only      │  │  options           │
              └────────────────────┘  └────────────────────┘
                           │              │
                           ▼              ▼
              ┌────────────────────┐  ┌────────────────────┐
              │  GET /available-   │  │  POST /initiate-   │
              │  upgrades          │  │  purchase          │
              └────────────────────┘  └────────────────────┘
```

## Upgrade Flow

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  User clicks    │────▶│  Call Available  │────▶│  Show Upgrade   │────▶│  User selects   │
│  "Upgrade"      │     │  Upgrades API    │     │  Options List   │     │  a package      │
└─────────────────┘     └──────────────────┘     └─────────────────┘     └────────┬────────┘
                                                                                  │
                        ┌──────────────────┐     ┌─────────────────┐              │
                        │  Handle Result   │◀────│  Call Initiate  │◀─────────────┘
                        │  (redirect/done) │     │  Upgrade API    │
                        └──────────────────┘     └─────────────────┘
```

## API Endpoints

### 1. Get Available Upgrades (NEW)

Get all packages the user can upgrade to with pre-calculated discounts.

**Endpoint:** `GET /v1/memberships/available-upgrades`

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (User has active membership):**
```json
{
  "code": "999999",
  "message": null,
  "data": [
    {
      "currentMembershipId": 1,
      "currentPackageName": "Basic Monthly",
      "currentPackageLevel": "BASIC",
      "daysRemaining": 15,
      "targetMembershipId": 2,
      "targetPackageName": "Standard Monthly",
      "targetPackageLevel": "STANDARD",
      "targetDurationDays": 30,
      "targetPackagePrice": 299000,
      "discountAmount": 50000,
      "finalPrice": 249000,
      "discountPercentage": 16.72,
      "forfeitedBenefits": [...],
      "newBenefits": [...],
      "eligible": true,
      "ineligibilityReason": null
    },
    {
      "currentMembershipId": 1,
      "currentPackageName": "Basic Monthly",
      "currentPackageLevel": "BASIC",
      "daysRemaining": 15,
      "targetMembershipId": 3,
      "targetPackageName": "Premium Monthly",
      "targetPackageLevel": "ADVANCED",
      "targetDurationDays": 30,
      "targetPackagePrice": 599000,
      "discountAmount": 50000,
      "finalPrice": 549000,
      "discountPercentage": 8.35,
      "eligible": true,
      "ineligibilityReason": null
    }
  ]
}
```

**Response (User has no active membership):**
```json
{
  "code": "999999",
  "message": null,
  "data": []
}
```

---

### 2. Preview Specific Upgrade

Get upgrade details including discount calculation before committing.

**Endpoint:** `GET /v1/memberships/upgrade-preview/{targetMembershipId}`

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (Eligible):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "currentMembershipId": 1,
    "currentPackageName": "Basic Monthly",
    "currentPackageLevel": "BASIC",
    "daysRemaining": 15,
    "targetMembershipId": 2,
    "targetPackageName": "Standard Monthly",
    "targetPackageLevel": "STANDARD",
    "targetDurationDays": 30,
    "targetPackagePrice": 299000,
    "discountAmount": 50000,
    "finalPrice": 249000,
    "discountPercentage": 16.72,
    "forfeitedBenefits": [
      {
        "benefitType": "POST_SILVER",
        "benefitName": "VIP Silver Posts",
        "totalQuantity": 5,
        "usedQuantity": 2,
        "remainingQuantity": 3,
        "estimatedValue": 30000
      }
    ],
    "newBenefits": [
      {
        "benefitType": "POST_GOLD",
        "benefitName": "VIP Gold Posts",
        "quantity": 10,
        "description": "Post listings with Gold VIP status"
      }
    ],
    "eligible": true,
    "ineligibilityReason": null
  }
}
```

**Response (Not Eligible - Downgrade Attempt):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "currentMembershipId": 2,
    "currentPackageName": "Standard Monthly",
    "currentPackageLevel": "STANDARD",
    "daysRemaining": 20,
    "targetMembershipId": 1,
    "targetPackageName": "Basic Monthly",
    "targetPackageLevel": "BASIC",
    "eligible": false,
    "ineligibilityReason": "Cannot downgrade membership. Target package must be higher tier than current package."
  }
}
```

**Response (No Active Membership):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "eligible": false,
    "ineligibilityReason": "No active membership found. Please purchase a membership first."
  }
}
```

---

### 2. Initiate Upgrade

Start the upgrade process. Returns payment URL if payment is required.

**Endpoint:** `POST /v1/memberships/initiate-upgrade`

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "targetMembershipId": 2,
  "paymentProvider": "VNPAY"
}
```

**Response (Payment Required):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "transactionRef": "abc123-def456-ghi789",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=...",
    "paymentProvider": "VNPAY",
    "previousMembershipId": 1,
    "newMembershipPackageId": 2,
    "newPackageName": "Standard Monthly",
    "newPackageLevel": "STANDARD",
    "originalPrice": 299000,
    "discountAmount": 50000,
    "finalAmount": 249000,
    "status": "PENDING_PAYMENT",
    "message": "Please complete payment to finalize upgrade."
  }
}
```

**Response (Free Upgrade - Discount Covers Full Price):**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "transactionRef": "abc123-def456-ghi789",
    "paymentUrl": null,
    "paymentProvider": "VNPAY",
    "previousMembershipId": 1,
    "newMembershipPackageId": 2,
    "newPackageName": "Standard Monthly",
    "newPackageLevel": "STANDARD",
    "originalPrice": 299000,
    "discountAmount": 350000,
    "finalAmount": 0,
    "status": "COMPLETED",
    "message": "Upgrade completed successfully. No payment required due to discount."
  }
}
```

---

## Error Codes

| Code | Description | When It Occurs |
|------|-------------|----------------|
| `14001` | No active membership found | User tries to upgrade without an active membership |
| `14002` | Cannot downgrade membership | User tries to upgrade to a lower-tier package |
| `14003` | Target package is same level | User tries to "upgrade" to same tier package |
| `14004` | Upgrade already in progress | User has a pending upgrade transaction |
| `14005` | Membership upgrade failed | Internal error during upgrade process |
| `14006` | Invalid upgrade target | Target package doesn't exist or is inactive |
| `14007` | Already has active membership | User tries to purchase new membership instead of upgrading |

### Handling Error 14007 (Already Has Active Membership)

When a user with an active membership tries to purchase a new membership (instead of upgrading), the API returns error `14007`. The frontend should:

1. Show a friendly message explaining they already have a membership
2. Redirect them to the upgrade page or show upgrade options
3. Example message: "You already have an active membership. Would you like to upgrade to a higher tier instead?"

---

## Package Level Hierarchy

Upgrades are only allowed to higher tiers:

```
BASIC → STANDARD → ADVANCED (PREMIUM)
```

- **BASIC** can upgrade to STANDARD or ADVANCED
- **STANDARD** can upgrade to ADVANCED only
- **ADVANCED** cannot upgrade (highest tier)

---

## Discount Calculation Logic

The discount is calculated based on the **pro-rated time value** of the remaining membership period:

```
time_ratio = days_remaining / total_duration_days
pro_rated_discount = amount_paid × time_ratio
minimum_payment = target_price - current_price  (price difference)
discount = min(pro_rated_discount, target_price - minimum_payment)
final_price = max(minimum_payment, target_price - discount)
```

**Key Points:**
- User always pays at least the price difference between packages
- Discount is based on remaining time, not unused benefits
- If discount covers the full price difference, user pays only the difference
- Benefits are forfeited but their value contributes to the discount calculation for display purposes

---

## Frontend Implementation Example (React/TypeScript)

```typescript
// Types
interface UpgradePreview {
  currentMembershipId: number;
  currentPackageName: string;
  currentPackageLevel: string;
  daysRemaining: number;
  targetMembershipId: number;
  targetPackageName: string;
  targetPackageLevel: string;
  targetDurationDays: number;
  targetPackagePrice: number;
  discountAmount: number;
  finalPrice: number;
  discountPercentage: number;
  forfeitedBenefits: ForfeitedBenefit[];
  newBenefits: NewBenefit[];
  eligible: boolean;
  ineligibilityReason: string | null;
}

interface ForfeitedBenefit {
  benefitType: string;
  benefitName: string;
  totalQuantity: number;
  usedQuantity: number;
  remainingQuantity: number;
  estimatedValue: number;
}

interface UpgradeResponse {
  transactionRef: string;
  paymentUrl: string | null;
  paymentProvider: string;
  previousMembershipId: number;
  newMembershipPackageId: number;
  newPackageName: string;
  newPackageLevel: string;
  originalPrice: number;
  discountAmount: number;
  finalAmount: number;
  status: 'PENDING_PAYMENT' | 'COMPLETED';
  message: string;
}

// API Functions

// Get all available upgrade options (recommended for showing upgrade page)
async function getAvailableUpgrades(): Promise<UpgradePreview[]> {
  const response = await fetch('/v1/memberships/available-upgrades', {
    headers: { 'Authorization': `Bearer ${getToken()}` },
  });
  const result = await response.json();
  return result.data;
}

// Get preview for a specific package (optional - for detailed view)
async function getUpgradePreview(targetMembershipId: number): Promise<UpgradePreview> {
  const response = await fetch(`/v1/memberships/upgrade-preview/${targetMembershipId}`, {
    headers: { 'Authorization': `Bearer ${getToken()}` },
  });
  const result = await response.json();
  return result.data;
}

async function initiateUpgrade(targetMembershipId: number): Promise<UpgradeResponse> {
  const response = await fetch('/v1/memberships/initiate-upgrade', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ targetMembershipId, paymentProvider: 'VNPAY' }),
  });
  const result = await response.json();
  return result.data;
}

// Component Example - Upgrade Options Page
function UpgradeOptionsPage() {
  const [upgrades, setUpgrades] = useState<UpgradePreview[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUpgrades();
  }, []);

  const loadUpgrades = async () => {
    try {
      const data = await getAvailableUpgrades();
      setUpgrades(data);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  if (upgrades.length === 0) {
    return (
      <div>
        <p>No upgrade options available.</p>
        <p>You either have no active membership or are already at the highest tier.</p>
      </div>
    );
  }

  return (
    <div className="upgrade-options">
      <h2>Available Upgrades</h2>
      {upgrades.map(upgrade => (
        <UpgradeCard key={upgrade.targetMembershipId} upgrade={upgrade} />
      ))}
    </div>
  );
}

// Component Example - Single Upgrade Button
function UpgradeButton({ targetPackageId }: { targetPackageId: number }) {
  const [preview, setPreview] = useState<UpgradePreview | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(false);

  const handlePreview = async () => {
    setLoading(true);
    try {
      const data = await getUpgradePreview(targetPackageId);
      setPreview(data);
      setShowModal(true);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmUpgrade = async () => {
    if (!preview?.eligible) return;
    setLoading(true);
    try {
      const result = await initiateUpgrade(targetPackageId);
      if (result.paymentUrl) {
        window.location.href = result.paymentUrl; // Redirect to payment
      } else {
        toast.success(result.message); // Free upgrade completed
        refreshMembership();
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <button onClick={handlePreview} disabled={loading}>
        {loading ? 'Loading...' : 'Upgrade'}
      </button>
      {showModal && preview && (
        <UpgradePreviewModal
          preview={preview}
          onConfirm={handleConfirmUpgrade}
          onCancel={() => setShowModal(false)}
        />
      )}
    </>
  );
}
```

---

## UI/UX Recommendations

### Upgrade Preview Modal

Display the following information clearly:

```
┌─────────────────────────────────────────────────────────┐
│                    Upgrade Preview                       │
├─────────────────────────────────────────────────────────┤
│  Current Plan: Basic Monthly                            │
│  Days Remaining: 15 days                                │
│                                                         │
│  ↓ Upgrading to ↓                                       │
│                                                         │
│  New Plan: Standard Monthly                             │
│  Duration: 30 days                                      │
├─────────────────────────────────────────────────────────┤
│  PRICING                                                │
│  Original Price:        299,000 VND                     │
│  Your Discount:        -50,000 VND (16.7%)              │
│  Final Price:          249,000 VND                      │
├─────────────────────────────────────────────────────────┤
│  ⚠️ Benefits You'll Forfeit:                            │
│  • VIP Silver Posts: 3 remaining (value: 30,000 VND)    │
│  • Push Credits: 2 remaining (value: 20,000 VND)        │
├─────────────────────────────────────────────────────────┤
│  ✨ New Benefits You'll Receive:                        │
│  • 10x VIP Gold Posts                                   │
│  • 5x Push Credits                                      │
│  • Priority Support                                     │
├─────────────────────────────────────────────────────────┤
│         [Cancel]              [Confirm Upgrade]         │
└─────────────────────────────────────────────────────────┘
```

### Package Comparison Page

- Highlight current plan with "Current Plan" badge
- Show "Upgrade" button only for higher-tier packages
- Disable/hide lower-tier packages or show tooltip explaining why

---

## Post-Payment Handling

After VNPay payment callback, handle the result:

```typescript
// On payment result page
useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const success = params.get('success') === 'true';

  if (success) {
    toast.success('Membership upgraded successfully!');
    navigate('/membership');
  } else {
    toast.error('Payment failed. Please try again.');
  }
}, []);
```

---

## Testing Scenarios

| Scenario | Expected Behavior |
|----------|-------------------|
| User has no membership | `available-upgrades` returns empty list |
| User has no membership, tries to upgrade | Show error: "No active membership found" |
| User has membership, tries to purchase new | Show error 14007: "Already has active membership" |
| User tries to downgrade | Show error: "Cannot downgrade membership" |
| User upgrades with payment | Redirect to VNPay, complete after payment |
| User upgrades for free | Complete immediately, show success message |
| Target package inactive | Show error: "Target package is not active" |
| User at highest tier | `available-upgrades` returns empty list |

---

## Related APIs

### Membership Management
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/memberships/packages` | GET | List all available packages |
| `/v1/memberships/my-membership` | GET | Get current user's membership |
| `/v1/memberships/my-benefits` | GET | Get current user's benefits |

### Purchase (for users WITHOUT active membership)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/memberships/initiate-purchase` | POST | Start new membership purchase |

### Upgrade (for users WITH active membership)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/memberships/available-upgrades` | GET | Get all upgrade options with discounts |
| `/v1/memberships/upgrade-preview/{id}` | GET | Preview specific upgrade |
| `/v1/memberships/initiate-upgrade` | POST | Start upgrade process |

