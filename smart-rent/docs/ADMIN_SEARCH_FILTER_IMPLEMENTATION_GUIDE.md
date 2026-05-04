# Admin Search Filters - Backend Implementation Guide

## Goal
Align backend list endpoints with the admin FE search/filter usage so that `search` in the UI actually returns filtered results.

## FE Admin Usage (current)
- Users list sends `keyword`, `isBroker`, `status` (optional):
  - Source: [smartrent-fe-admin/src/api/services/user.service.ts](smartrent-fe-admin/src/api/services/user.service.ts)
  - UI placeholder: "Search by name or user ID"
- Admins list sends `keyword`, `role`, `status` (optional):
  - Source: [smartrent-fe-admin/src/api/services/admin.service.ts](smartrent-fe-admin/src/api/services/admin.service.ts)
  - UI placeholder: "Search by name, ID or email"
- Admin news list sends `keyword`, `category`, `tag`, `status` (optional):
  - Source: [smartrent-fe-admin/src/api/services/news.service.ts](smartrent-fe-admin/src/api/services/news.service.ts)

## Current Backend State (verified)
- Users list supports only `page` and `size`:
  - Controller: [smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/UserController.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/UserController.java)
  - Service: [smartrent-backend/smart-rent/src/main/java/com/smartrent/service/user/UserService.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/service/user/UserService.java)
- Admins list supports only `page` and `size`:
  - Controller: [smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/AdminController.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/AdminController.java)
  - Service: [smartrent-backend/smart-rent/src/main/java/com/smartrent/service/admin/AdminService.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/service/admin/AdminService.java)
- Admin news list supports only `page`, `size`, `status`:
  - Controller: [smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/AdminNewsController.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/controller/AdminNewsController.java)
  - Service: [smartrent-backend/smart-rent/src/main/java/com/smartrent/service/news/NewsService.java](smartrent-backend/smart-rent/src/main/java/com/smartrent/service/news/NewsService.java)

## Target API Behavior
### 1) GET /v1/admin/news
Add optional query params:
- `keyword`: search in `title` + `summary` (same behavior as public news search)
- `category`: filter by category
- `tag`: filter by tag (CSV field, `LIKE` match)
- `status`: existing filter

Expected combinations:
- `keyword + category` should narrow results
- `keyword + status` should narrow results
- `tag + status` should narrow results

### 2) GET /v1/users/list
Add optional query params:
- `keyword`: search by `userId`, `firstName`, `lastName`, `email` (optionally include phone or idDocument)
- `isBroker`: filter by broker status
- `status`: FE uses `normal` / `banned` but backend has no field yet (see Open Questions)

### 3) GET /v1/admins/list
Add optional query params:
- `keyword`: search by `adminId`, `firstName`, `lastName`, `email` (optionally include phone)
- `role`: filter by role (see Role Mapping below)
- `status`: FE uses `active` / `inactive` but backend has no field yet (see Open Questions)

## Implementation Steps

### A) Admin news list
1. Update controller method signature in AdminNewsController to accept `keyword`, `category`, `tag`.
2. Update NewsService interface to add these params on `getAllNews`.
3. Update NewsServiceImpl logic to apply the same filter rules as public news search, but without forcing `PUBLISHED`.
4. Extend NewsRepository with admin search queries for:
   - keyword only
   - keyword + category
   - tag
   - tag + status
   - category + status

Suggested logic (pseudo):
```
if keyword present:
  if category present: search title/summary by category and status (if provided)
  else: search title/summary with status (if provided)
else if tag present:
  filter tag with status (if provided)
else if category present:
  filter category with status (if provided)
else if status present:
  filter status
else:
  find all
```

### B) Users list
1. Extend UserController.getUsers with optional `keyword`, `isBroker`, `status`.
2. Add new filter object or extend signature in UserService + UserServiceImpl.
3. Implement filtering using a JPA Specification or a repository method with dynamic predicates.
4. Update cache key in UserServiceImpl (currently `#page + #size`) to include filters, or disable cache for filtered queries.

Suggested searchable fields:
- `userId`, `firstName`, `lastName`, `email`
- Optional: `phoneNumber`, `idDocument`, `taxNumber`

### C) Admins list
1. Extend AdminController.getAllAdmins with optional `keyword`, `role`, `status`.
2. Add filter support in AdminService + AdminServiceImpl.
3. Implement query in AdminRepository (likely `@Query` + join to roles).

Suggested role filter:
- Join to roles: `JOIN a.roles r`
- Filter by `r.roleId` or `r.roleName`

## Role Mapping
- FE role filter values: `support`, `moderator`, `admin`, `super_admin`.
- Backend roles in DB appear as `roleId` (e.g. `ADMIN`, `SUPER_ADMIN`) and `roleName` (e.g. `Administrator`).
- Decide one canonical mapping for filtering:
  - Option 1: Normalize FE value to `roleId` (uppercase with `_`).
  - Option 2: Map FE value to `roleName` via lookup.

## Open Questions (need decision)
1. Admin/user `status` filter: there is no `active/inactive` or `banned` field in `Admin` or `User` entities.
   - Option A: add `isActive` or `accountStatus` to entities + DB.
   - Option B: ignore `status` for now and return all.
2. Admin role filter mapping: pick `roleId` vs `roleName` and update FE or backend normalization.

## Suggested Test Cases
- Users list: `keyword=abc`, `isBroker=true`, `page/size` pagination.
- Admins list: `keyword=admin`, `role=super_admin`.
- Admin news list: `keyword=market`, `category=BLOG`, `status=DRAFT`.

## Notes
- Listing admin search already supports `keyword` in `ListingFilterRequest`. No changes needed for listing search.
- Keep pagination 1-based to match FE.
