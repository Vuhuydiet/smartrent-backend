# Listing Moderation & Report-Resolution Flow Requirements (Implementation Starter Pack)

## 1) Goal

Unify two flows into one moderation system:

1. **Flow A**: Owner updates listing after admin resolves a listing report.
2. **Flow B**: Admin reviews newly created listing; if rejected, owner updates and resubmits.

Both flows should share the same moderation lifecycle, audit trail, and notification behavior.

---

## 2) Current behavior (from existing backend)

### 2.1 New listing creation state
- New listing is created with:
  - `verified = false`
  - `isVerify = true`
- This maps to `IN_REVIEW` in computed owner status.

### 2.2 Admin review endpoint
- Existing endpoint: `PUT /v1/admin/listings/{listingId}/status`
- Current request shape:
  - `verified: Boolean`
  - `reason: String`
- Current reject behavior:
  - `verified = false`
  - `isVerify = false`
  - Listing enters `REJECTED` status via computed logic.

### 2.3 Public visibility protection
- Public search defaults to `verified = true` only.
- Therefore rejected/unverified listings are not publicly visible.

### 2.4 Listing report resolution
- Existing admin report endpoint resolves report status and notes.
- Current behavior updates report fields (`status`, `resolvedBy`, `resolvedAt`, `adminNotes`) only.
- It does not define/track an owner-action workflow.

### 2.5 Owner-facing rejection context gap
- Owner response DTO has `verificationNotes` and `rejectionReason`.
- Current owner listing services fill these fields with `null` placeholders.

---

## 3) Unified lifecycle model (required)

Introduce a **single moderation lifecycle** for listing content changes, regardless of source (new listing or report-fix).

### 3.1 Lifecycle states
Use one canonical moderation state (new field):

- `PENDING_REVIEW`
- `APPROVED`
- `REJECTED`
- `REVISION_REQUIRED`
- `RESUBMITTED`
- `SUSPENDED` (optional, for severe violations)

> Keep backward compatibility with current `verified/isVerify` flags during migration.

### 3.2 Source context
Track source of moderation cycle:

- `NEW_SUBMISSION`
- `REPORT_RESOLUTION`
- `OWNER_EDIT` (optional)

This enables analytics and SLA per flow.

---

## 4) Data model requirements

## 4.1 New table: `listing_moderation_events`
Purpose: immutable audit trail.

Suggested columns:
- `event_id` (PK)
- `listing_id` (FK)
- `source` (`NEW_SUBMISSION`, `REPORT_RESOLUTION`, ...)
- `from_status`
- `to_status`
- `action` (`APPROVE`, `REJECT`, `REQUEST_REVISION`, `RESUBMIT`, ...)
- `reason_code` (nullable)
- `reason_text` (nullable)
- `admin_id` (nullable)
- `triggered_by_user_id` (nullable)
- `report_id` (nullable, when linked to a report)
- `created_at`

## 4.2 New table: `listing_owner_actions`
Purpose: track owner obligations for revision.

Suggested columns:
- `owner_action_id` (PK)
- `listing_id` (FK)
- `trigger_type` (`REPORT_RESOLVED`, `LISTING_REJECTED`)
- `trigger_ref_id` (report id or moderation event id)
- `required_action` (`UPDATE_LISTING`, `CONTACT_SUPPORT`, ...)
- `status` (`PENDING_OWNER`, `OWNER_UPDATED`, `SUBMITTED_FOR_REVIEW`, `COMPLETED`, `EXPIRED`)
- `deadline_at` (nullable)
- `completed_at` (nullable)
- `created_at`, `updated_at`

## 4.3 Listing table additions (minimal)
Add nullable fields for fast querying:
- `moderation_status` (enum)
- `last_moderated_by`
- `last_moderated_at`
- `last_moderation_reason_code`
- `last_moderation_reason_text`
- `revision_count` (default 0)

---

## 5) API requirements

## 5.1 Extend admin listing review API
Endpoint: `PUT /v1/admin/listings/{listingId}/status`

### Request (new)
```json
{
  "decision": "APPROVE | REJECT | REQUEST_REVISION",
  "reasonCode": "MISSING_INFO",
  "reasonText": "Missing legal information in description",
  "ownerActionRequired": true,
  "ownerActionDeadlineAt": "2026-03-01T23:59:59"
}
```

### Rules
- `decision=APPROVE`:
  - set listing `moderation_status=APPROVED`
  - map compatibility flags: `verified=true`, `isVerify=true`
- `decision=REJECT` or `REQUEST_REVISION`:
  - set listing `moderation_status=REVISION_REQUIRED` (recommended) or `REJECTED`
  - map compatibility flags: `verified=false`, `isVerify=false`
  - require `reasonCode` + `reasonText`
  - create `listing_owner_actions` row when `ownerActionRequired=true`
- Always write a `listing_moderation_events` record.

## 5.2 Extend admin report resolve API
Endpoint: `PUT /v1/admin/listing-reports/{reportId}/resolve`

### Request (new)
```json
{
  "status": "RESOLVED | REJECTED",
  "adminNotes": "Please update listing title and real area",
  "ownerActionRequired": true,
  "ownerActionType": "UPDATE_LISTING",
  "ownerActionDeadlineAt": "2026-03-01T23:59:59",
  "listingVisibilityAction": "KEEP_VISIBLE | HIDE_UNTIL_REVIEW"
}
```

### Rules
- If `ownerActionRequired=true`:
  - create owner action row (`trigger_type=REPORT_RESOLVED`)
  - if `HIDE_UNTIL_REVIEW`, set listing to moderation state requiring review (non-public)
- If `ownerActionRequired=false`, report closes with no owner workflow.

## 5.3 Owner update/resubmit endpoints
Keep existing owner edit endpoint:
- `PUT /v1/listings/{id}` (content update)

Add one explicit action endpoint:
- `POST /v1/listings/{id}/resubmit-for-review`

### Resubmit rules
- Allowed only when listing has open owner action or status is `REVISION_REQUIRED/REJECTED`.
- Validate ownership.
- Transition:
  - `moderation_status -> RESUBMITTED/PENDING_REVIEW`
  - compatibility flags: `verified=false`, `isVerify=true`
- Close or advance owner action status.
- Create moderation event.

## 5.4 Owner read APIs must return reason context
Enhance owner detail/list responses to include latest moderation context:
- `verificationNotes`
- `rejectionReason`
- `moderationStatus`
- `pendingOwnerAction` object (if exists)

This removes current `null` placeholder issue.

---

## 6) Notification requirements

## 6.1 Email templates
Implement with existing email service abstraction.

Required templates:
1. `LISTING_REJECTED`
2. `REVISION_REQUESTED`
3. `REPORT_RESOLVED_OWNER_ACTION_REQUIRED`
4. `LISTING_RESUBMITTED`
5. `LISTING_APPROVED_AFTER_RESUBMIT`

## 6.2 Trigger points
- On admin reject/revision request.
- On report resolved with owner action required.
- On owner resubmit (notify admins queue).
- On final approval.

## 6.3 Reliability
- Send asynchronously (event queue recommended).
- Do not fail moderation transaction if email fails.
- Log/send retry with existing resilient email layer.

---

## 7) Authorization & security requirements

- Only owner can update/resubmit own listing.
- Only admin roles can moderate listings/reports.
- Enforce ownership checks on all owner-action endpoints.
- Audit all admin decisions with actor + timestamp.

---

## 8) Business rules (explicit)

1. Unverified/rejected/revision-required listings must never appear in public listings.
2. Every rejection/revision request must include a human-readable reason.
3. Owner edits do not auto-publish listing.
4. Owner must resubmit to return listing to review queue.
5. Report-resolution-required fix follows same moderation queue as rejected-new-listing.
6. Repeated violations can escalate to `SUSPENDED`.

---

## 9) Admin & owner UI/UX requirements

## 9.1 Owner
- Add “Needs your update” filter/tab.
- Show reasons, deadline, and “Edit + Resubmit” CTA.
- Show status timeline (`Rejected -> Updated -> Resubmitted -> Approved`).

## 9.2 Admin
- Separate queues:
  - `New submissions`
  - `Resubmissions`
  - `Report-driven fixes`
- Show diff summary and linked report context.
- Quick actions: approve, request revision, reject, suspend.

---

## 10) Suggested migration/rollout plan

### Phase 1 (safe foundation)
- Add new DB tables + nullable listing moderation fields.
- Start writing moderation events for existing admin review endpoint.
- Keep current API contract backward compatible.

### Phase 2 (reason visibility)
- Persist reason data and return via owner detail/list endpoints.
- Add rejection/revision emails.

### Phase 3 (full unified workflow)
- Extend report resolve API for owner action required.
- Add owner resubmit endpoint.
- Add owner action state machine + SLA reminders.

### Phase 4 (cleanup)
- Gradually reduce dependency on dual-flag logic (`verified/isVerify`) once all consumers migrate to `moderation_status`.

---

## 11) Acceptance criteria

1. Admin rejects listing with reason -> owner receives email -> owner sees reason in app -> listing not public.
2. Owner updates and resubmits -> listing enters review queue (`PENDING_REVIEW`) -> admin can approve.
3. Admin resolves report with owner action required -> owner receives email + pending action appears -> owner can edit/resubmit.
4. All transitions generate audit events.
5. Public search never leaks non-approved listings.
6. Existing clients depending on `verified/isVerify` continue working during migration.

---

## 12) QA test matrix (minimum)

1. **New listing happy path**: create -> pending -> approve -> public visible.
2. **New listing rejected path**: create -> reject(reason) -> owner edit -> resubmit -> approve.
3. **Report action path**: report -> admin resolve(owner action required) -> owner edit -> resubmit -> approve.
4. **Authorization**: other user cannot resubmit/update listing.
5. **Edge cases**:
   - resubmit without pending action
   - double resubmit
   - approve already approved
   - reject without reason
6. **Notification failure isolation**: moderation succeeds when email provider fails.

---

## 13) Open decisions (product)

1. Should report-driven fixes hide listing immediately or after deadline?
2. Max revision attempts before suspension?
3. Deadline policy (e.g., 7 days default)?
4. Should owner update auto-create draft or directly mutate live rejected listing?

