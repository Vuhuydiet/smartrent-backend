# Membership Upgrade Feature - Business Document

## Executive Summary

The Membership Upgrade feature allows existing members to upgrade to higher-tier packages while receiving a fair discount based on their remaining membership time. This feature increases customer lifetime value, reduces churn, and provides a seamless path for users to access premium features.

## Business Objectives

1. **Increase Revenue**: Enable upselling to higher-tier packages
2. **Reduce Churn**: Provide upgrade path instead of cancellation
3. **Improve Customer Satisfaction**: Fair discount calculation rewards loyal customers
4. **Simplify User Experience**: One-click upgrade with transparent pricing

## Feature Overview

### Membership Tiers

| Tier | Level | Target Users |
|------|-------|--------------|
| BASIC | Entry | New users, casual landlords |
| STANDARD | Mid | Active landlords with multiple properties |
| ADVANCED | Premium | Professional property managers |

### Upgrade Rules

- Users can only upgrade to **higher tiers** (no downgrades)
- Users with active membership **cannot purchase new membership** (must upgrade)
- Upgrade starts a **new membership period** (not extension)
- Previous benefits are **forfeited** but contribute to discount

## Discount Calculation

### Formula

```
Pro-rated Discount = (Days Remaining / Total Days) × Amount Paid
Minimum Payment = Target Price - Current Price (price difference)
Final Discount = min(Pro-rated Discount, Target Price - Minimum Payment)
Final Price = Target Price - Final Discount
```

### Business Logic

1. **Fair Value**: Users receive credit for unused membership time
2. **Minimum Payment**: Users always pay at least the price difference between tiers
3. **No Free Upgrades**: Discount cannot exceed the price difference (user always pays something for the upgrade value)

### Example Scenarios

#### Scenario 1: Early Upgrade (High Discount)
- Current: BASIC (99,000 VND, 30 days)
- Days Used: 5 days (25 remaining)
- Target: STANDARD (299,000 VND)
- Pro-rated: (25/30) × 99,000 = 82,500 VND
- Price Difference: 299,000 - 99,000 = 200,000 VND
- Final Price: max(200,000, 299,000 - 82,500) = 216,500 VND

#### Scenario 2: Late Upgrade (Low Discount)
- Current: BASIC (99,000 VND, 30 days)
- Days Used: 28 days (2 remaining)
- Target: STANDARD (299,000 VND)
- Pro-rated: (2/30) × 99,000 = 6,600 VND
- Price Difference: 200,000 VND
- Final Price: max(200,000, 299,000 - 6,600) = 292,400 VND

## User Journey

### For Users WITHOUT Active Membership
```
Browse Packages → Select Package → Purchase → Payment → Active Membership
```

### For Users WITH Active Membership
```
View Current Plan → See Upgrade Options → Select Upgrade → 
Review Discount → Confirm → Payment → New Membership Active
```

### Blocked Actions
- Users with active membership **cannot** purchase a new membership
- System returns error: "You already have an active membership. Please use the upgrade feature."

## Membership Status Lifecycle

```
ACTIVE → (upgrade initiated) → UPGRADED → (new membership) → ACTIVE
                                    ↓
                            Previous membership marked as UPGRADED
                            New membership created as ACTIVE
```

## Revenue Impact Analysis

### Projected Benefits

| Metric | Before | After (Projected) |
|--------|--------|-------------------|
| Upgrade Rate | N/A | 15-20% of BASIC users |
| Average Revenue per User | 99,000 VND | 180,000 VND |
| Churn Rate | 25% | 18% |
| Customer Lifetime Value | 297,000 VND | 540,000 VND |

### Revenue Scenarios

**Conservative (10% upgrade rate)**
- 1,000 BASIC users × 10% × 200,000 VND = 20,000,000 VND additional revenue

**Moderate (20% upgrade rate)**
- 1,000 BASIC users × 20% × 200,000 VND = 40,000,000 VND additional revenue

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Users gaming the system | Minimum payment ensures fair pricing |
| Confusion about benefits | Clear preview showing forfeited vs new benefits |
| Payment failures | Transaction rollback, no membership changes until payment confirmed |
| Duplicate upgrades | System prevents multiple pending upgrade transactions |

## Success Metrics (KPIs)

1. **Upgrade Conversion Rate**: % of eligible users who upgrade
2. **Average Discount Given**: Monitor to ensure profitability
3. **Time to Upgrade**: Days from membership start to upgrade
4. **Post-Upgrade Retention**: % of upgraded users who renew
5. **Revenue per Upgrade**: Average additional revenue from upgrades

## Implementation Phases

### Phase 1 (Current) ✅
- Basic upgrade flow with discount calculation
- Payment integration (VNPay)
- Membership status management

### Phase 2 (Future)
- Promotional upgrade discounts
- Upgrade reminders/notifications
- A/B testing different discount strategies

### Phase 3 (Future)
- Subscription auto-renewal
- Downgrade with credit system
- Family/team membership plans

## API Summary for Stakeholders

| User State | Action | API Endpoint |
|------------|--------|--------------|
| No membership | View packages | `GET /v1/memberships/packages` |
| No membership | Purchase | `POST /v1/memberships/initiate-purchase` |
| Has membership | View upgrade options | `GET /v1/memberships/available-upgrades` |
| Has membership | Preview specific upgrade | `GET /v1/memberships/upgrade-preview/{id}` |
| Has membership | Initiate upgrade | `POST /v1/memberships/initiate-upgrade` |
| Has membership | Try to purchase (blocked) | Returns error 14007 |

## Appendix: Error Codes

| Code | Message | User Action |
|------|---------|-------------|
| 14001 | No active membership found | Direct to purchase page |
| 14002 | Cannot downgrade membership | Show only higher-tier options |
| 14003 | Same membership level | Hide current tier from options |
| 14004 | Upgrade already in progress | Show pending transaction |
| 14007 | Already has active membership | Redirect to upgrade page |

## Approval & Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Product Owner | | | |
| Tech Lead | | | |
| Business Analyst | | | |
| QA Lead | | | |

---

*Document Version: 1.0*
*Last Updated: January 2026*
*Author: SmartRent Development Team*

