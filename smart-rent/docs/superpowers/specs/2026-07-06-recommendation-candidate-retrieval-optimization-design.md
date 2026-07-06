# Tối ưu candidate retrieval của recommendation (personalized + similar)

- **Ngày:** 2026-07-06
- **Phạm vi:** `RecommendationServiceImpl.getPersonalizedFeed` ("Bất động sản dành cho bạn")
  và `getSimilarListings` ("Bất động sản tương tự").
- **Trạng thái:** đã duyệt, đang implement.

## 1. Bối cảnh & nguyên nhân (đã xác minh bằng đo đạc)

Endpoint `/v1/recommendations/personalized` mất **63–81 giây**. Dòng log
`[PerfTrace]` sẵn có trong `RecommendationServiceImpl` chia nhỏ latency và cho thấy
toàn bộ thời gian nằm ở **stage `candidateRetrieval`** (≈63–81s), còn mọi stage khác
là mili-giây (`feignAi`≈17ms, `cfGlobal+features`≈15ms, `interactions`≈250–650ms).
→ Không phải AI, không phải payload — 100% là các query DB lấy candidate pool.

`EXPLAIN ANALYZE` một channel candidate trên chính DB app:
`-> Sort row IDs: l.pushed_at DESC, l.post_date DESC` → **filesort**, ~2s khi chạy
đơn lẻ (cache ấm, không tranh chấp). Trong thực tế mỗi request chạy **3 channel song
song** filesort tập lớn trên **DB DigitalOcean Managed 1GB / 1 vCPU với
`innodb_buffer_pool_size = 32MB`** → thrash buffer pool + đói I/O → 63s. Query
tra-theo-id (stage `interactions`) chỉ chạm vài trang nên vẫn nhanh → giải thích vì
sao chậm *có chọn lọc*.

### Root cause: filter và sort ở hai bảng khác nhau

Query candidate **lọc trên `addresses`** (`new_province_code` / `new_ward_code` /
`legacy_district_id` / `legacy_ward_id` / `legacy_province_id`) nhưng **sắp xếp trên
`listings`** (`pushed_at`, `post_date`). Không B-tree index nào phục vụ được đồng thời
"lọc bảng X, sort bảng Y" → MySQL buộc filesort toàn bộ tập matched của cả tỉnh
(HCMC ~chục nghìn dòng) chỉ để lấy 100. Index `idx_listings_pushed_at` (V81, tạo đúng
cho recommendation) **vô dụng** vì bảng lái query là `addresses`.

Đây là schema chuẩn hoá cho write nhưng **chưa thiết kế read-model cho hot path** —
không phải index thiếu, mà thiếu điều kiện để index tồn tại. Vì `addresses` là dữ
liệu **cố định** (không đổi trong tương lai), denormalize là an toàn và sạch.

Ghi chú: `innodb_buffer_pool_size` **không sửa được** vì đây là DO Managed DB (không
cấp SUPER, buffer pool cố định theo plan). Nên phải trị ở tầng query để chạy nhanh
ngay cả với buffer pool 32MB.

## 2. Giải pháp: denormalize location key + composite index (single-table)

### 2.1 Schema — 5 cột copy nguyên trạng từ `addresses` sang `listings`

| Cột (`listings`) | Kiểu | Nguồn |
|---|---|---|
| `new_province_code` | `VARCHAR(10)` NULL | `addresses.new_province_code` |
| `new_ward_code` | `VARCHAR(10)` NULL | `addresses.new_ward_code` |
| `legacy_province_id` | `INT` NULL | `addresses.legacy_province_id` |
| `legacy_district_id` | `INT` NULL | `addresses.legacy_district_id` |
| `legacy_ward_id` | `INT` NULL | `addresses.legacy_ward_id` |

Chỉ copy các cột dùng để **lọc**; không copy lat/lon (chỉ dùng lúc build DTO, không
nằm trong `WHERE`).

### 2.2 Index — 1 composite / mỗi shape location, mirror pattern `idx_listings_public_default_sort` (V82)

Mẫu: `(location_col, is_draft, is_shadow, verified, expired, pushed_at, post_date)`.
4 boolean visibility là equality → nằm giữa; `pushed_at, post_date` ở đuôi cho
**index-ordered scan → 0 filesort**. `expiry_date`, `NOT IN(excluded)`, `product_type`,
`listing_type`, `price` là residual filter (không phá thứ tự).

1. `idx_listings_reco_new_prov`  = `(new_province_code, is_draft, is_shadow, verified, expired, pushed_at, post_date)`
2. `idx_listings_reco_new_ward`  = `(new_ward_code, …)`
3. `idx_listings_reco_legacy_dist` = `(legacy_district_id, …)`
4. `idx_listings_reco_legacy_ward` = `(legacy_ward_id, …)`
5. `idx_listings_reco_legacy_prov` = `(legacy_province_id, …)`
6. `idx_listings_reco_fresh`     = `(is_draft, is_shadow, verified, expired, pushed_at, post_date)` (global / cold-start / top-up)

Cùng 6 index phục vụ **cả hai endpoint**; `similar` thêm `product_type`/`listing_type`
làm residual.

### 2.3 Viết lại query (signature-preserving)

Trong `ListingRepository`, mỗi query candidate: đổi filter `a.<col>` →
**`l.<col>`** (cột denormalized), giữ `LEFT JOIN FETCH l.address` để hydrate ~LIMIT
dòng cuối, bỏ `NULLS LAST` (MySQL DESC vốn để NULL cuối; Hibernate đã compile bỏ nên
SQL không đổi — dọn cho sạch). **Không đổi tên/tham số method** → không lan sang
`RecommendationServiceImpl` hay test.

Điều kiện OR `(new_province_code OR legacy_province_id)` ở price/similar channel tạm
**giữ nguyên nhưng đổi sang `l.*`** (để không đổi signature). Đây là channel phụ; khi
lọc đã nằm trên `listings`, MySQL index-merge/residual thay vì filesort xuyên bảng.
*Tinh chỉnh tương lai (deferred):* tách OR thành 2 query new/legacy + branch trong
Java để bỏ hẳn filesort ở channel giá.

### 2.4 Đồng bộ + backfill

- **Write-sync:** mở rộng hook `@PrePersist @PreUpdate updateSearchFields()` để copy 5
  mã từ `address`. Idempotent; vì address cố định thực chất chỉ cần lúc create.
- **Backfill (Flyway V97):** thêm cột → `UPDATE listings l JOIN addresses a` copy 5
  mã cho 94k dòng hiện có → tạo 6 index (tạo index *sau* backfill để nhanh hơn).
  Guard idempotent bằng `information_schema` theo đúng style V81/82/83.
- **Thứ tự rollout an toàn:** migration chạy trước, code đọc cột mới deploy sau; code
  cũ bỏ qua cột mới → không có khoảng gãy.

## 3. Nghiệm thu

- **Tương đương:** bộ candidate trước/sau phải trùng (cùng freshest theo location).
- **Perf:** `EXPLAIN ANALYZE` từng channel → dùng index mới, **không `Sort row IDs`**;
  đo lại `[PerfTrace] candidateRetrieval` — mục tiêu **< 300ms**.
- **Backfill đúng:** `COUNT(*)` các dòng `l.new_province_code <> a.new_province_code`
  (và 4 cột kia) = 0.
- **Integration/regression:** 2 endpoint trả feed đúng; `RecommendationServiceImplTest`
  xanh.

## 4. Rủi ro & giới hạn

- Ghi thêm 6 index → mỗi lần "đẩy tin" (update `pushed_at`) ghi lại 6 index; chấp nhận
  với tần suất push thực tế. Tổng dung lượng index thêm ~25MB / 94k dòng (không đáng
  trên 10GiB).
- Backfill `UPDATE` 94k dòng khoá bảng ngắn khi deploy — chấp nhận (one-time).
- Denormalized data lệ thuộc address **cố định**; nếu sau này address đổi hàng loạt
  độc lập thì cần 1 migration re-sync (ngoài phạm vi hiện tại).
