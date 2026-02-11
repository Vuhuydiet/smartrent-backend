# [SMARTRENT][BUSINESS LOGIC] - Charge Features (VNPay Only - Updated)

## I. TỔNG QUAN CÁC LOẠI TIN ĐĂNG

### 1. **TIN THƯỜNG (NORMAL)**
```
Giá gốc: 2,700 VND/ngày
Pricing theo duration:
- 10 ngày: 27,000 VND (2,700 đ/ngày)
- 15 ngày: 36,000 VND (2,400 đ/ngày) - Tiết kiệm 11%
- 30 ngày: 66,000 VND (2,200 đ/ngày) - Tiết kiệm 18.5%

Features:
- Hiển thị theo thứ tự thời gian
- Cần chờ kiểm duyệt (4-8h)
- Có banner quảng cáo
- Tối đa 5 ảnh, 1 video
- Không có badge
- Hiển thị dưới cùng
```

### 2. **VIP BẠC (VIP SILVER)**
```
Giá gốc: 50,000 VND/ngày
Pricing theo duration:
- 10 ngày: 500,000 VND (50,000 đ/ngày)
- 15 ngày: 667,500 VND (44,500 đ/ngày) - Tiết kiệm 11%
- 30 ngày: 1,222,500 VND (40,750 đ/ngày) - Tiết kiệm 18.5%

Features:
- Hiển thị ngay (không chờ kiểm duyệt với membership)
- Không có banner quảng cáo
- Badge "VIP BẠC" màu xanh ngọc
- Ưu tiên hiển thị trên tin thường
- Tối đa 10 ảnh, 2 video
```

### 3. **VIP VÀNG (VIP GOLD)**
```
Giá gốc: 110,000 VND/ngày
Pricing theo duration:
- 10 ngày: 1,100,000 VND (110,000 đ/ngày)
- 15 ngày: 1,468,500 VND (97,900 đ/ngày) - Tiết kiệm 11%
- 30 ngày: 2,689,500 VND (89,650 đ/ngày) - Tiết kiệm 18.5%

Features:
- Tất cả tính năng của VIP Bạc
- Badge "VIP VÀNG" màu vàng đồng
- Ưu tiên hiển thị trên VIP Bạc
- Tối đa 12 ảnh, 2 video
- Hiển thị ở vị trí tốt hơn
```

### 4. **VIP KIM CƯƠNG (VIP DIAMOND)**
```
Giá gốc: 280,000 VND/ngày
Pricing theo duration:
- 10 ngày: 2,800,000 VND (280,000 đ/ngày)
- 15 ngày: 3,738,000 VND (249,200 đ/ngày) - Tiết kiệm 11%
- 30 ngày: 6,846,000 VND (228,200 đ/ngày) - Tiết kiệm 18.5%

Features:
- Tất cả tính năng của VIP Vàng
- Badge "VIP KIM CƯƠNG" màu đỏ
- Ưu tiên hiển thị CAO NHẤT
- Tối đa 15 ảnh, 3 video
- Hiển thị vị trí TOP trang chủ
- Nhân đôi hiển thị: Tặng kèm 1 tin Thường
- Đẩy Kim Cương → Tin Thường đi kèm cũng được đẩy free
```

### 5. **PUSH (ĐẨY TIN)**
```
Giá: 40,000 VND/lần
- Đẩy tin lên đầu danh sách
- Có thể mua lẻ hoặc dùng quota từ membership
- Reset lại thời gian post_date
```

---

## II. PAYMENT FLOW (VNPAY ONLY)

### Nguyên tắc chung:
1. **Không còn wallet**: Web không quản lý số dư của user
2. **Mọi thanh toán qua VNPay**: Mỗi lần mua phải thanh toán trực tiếp qua VNPay
3. **Không nạp tiền trước**: User chỉ thanh toán khi cần mua

### Các trường hợp thanh toán:

#### Case 1: Mua Membership Package
- User chọn gói membership
- Redirect sang VNPay để thanh toán
- Sau khi thanh toán thành công → Kích hoạt membership và cấp quota

#### Case 2: Đăng bài KHÔNG có quota
User có 2 lựa chọn:
- **Option A**: Mua membership package (được nhiều quota, tiết kiệm hơn)
- **Option B**: Trả tiền đăng bài đó (pay-per-post, không nhận quota)

**Pay-per-post pricing (30 ngày):**
```
NORMAL:       66,000 VND
VIP BẠC:   1,222,500 VND
VIP VÀNG:  2,689,500 VND
VIP KC:    6,846,000 VND
```

#### Case 3: Đẩy tin KHÔNG có quota
User có 2 lựa chọn:
- **Option A**: Mua membership package
- **Option B**: Trả 40,000 VND/lần đẩy

---

## III. FLOW ĐĂNG TIN VỚI QUOTA CHECK

### Bước 1: User click "Đăng tin"
→ Chuyển đến màn hình "Chọn loại tin"

### Bước 2: Màn hình "Chọn loại tin"
System check quota và hiển thị:

```
┌─────────────────────────────────────────────────┐
│  CHỌN LOẠI TIN                                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ VIP KC       │  │ VIP VÀNG     │            │
│  │ 280k đ/ngày  │  │ 110k đ/ngày  │            │
│  │              │  │              │            │
│  │ [Còn 2/10]   │  │ [Hết quota]  │            │
│  │ DÙNG QUOTA   │  │ THANH TOÁN   │            │
│  └──────────────┘  └──────────────┘            │
│                                                 │
│  ┌──────────────┐  ┌──────────────┐            │
│  │ VIP BẠC      │  │ TIN THƯỜNG   │            │
│  │ 50k đ/ngày   │  │ 2.7k đ/ngày  │            │
│  │              │  │              │            │
│  │ [Còn 5/15]   │  │ THANH TOÁN   │            │
│  │ DÙNG QUOTA   │  │ 66,000 VND   │            │
│  └──────────────┘  └──────────────┘            │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Bước 3: User chọn loại tin

**Case A: Có quota**
1. User click card có "DÙNG QUOTA"
2. Tiếp tục flow đăng tin bình thường
3. Khi submit → Trừ quota + Đăng tin ngay

**Case B: Hết quota**
1. User click card "THANH TOÁN"
2. Hiển thị popup confirm:
```
┌────────────────────────────────────────────┐
│  BẠN ĐÃ HẾT QUOTA VIP VÀNG                │
│                                            │
│  Chọn một trong hai cách:                 │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │ 🎁 MUA GÓI MEMBERSHIP                │ │
│  │                                      │ │
│  │ Nhận nhiều quota miễn phí            │ │
│  │ • 10 tin VIP Vàng                    │ │
│  │ • 5 tin VIP KC                       │ │
│  │ • 20 lượt đẩy                        │ │
│  │                                      │ │
│  │ Chỉ 1,400,000 VND                    │ │
│  │ [XEM CÁC GÓI]                        │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │ 💳 TRẢ CHO TIN NÀY                   │ │
│  │                                      │ │
│  │ VIP Vàng - 30 ngày                   │ │
│  │ 2,689,500 VND                        │ │
│  │                                      │ │
│  │ [THANH TOÁN]                         │ │
│  └──────────────────────────────────────┘ │
│                                            │
│                         [HUỶ]             │
└────────────────────────────────────────────┘
```

3. Nếu chọn "TRẢ CHO TIN NÀY":
   - Redirect sang VNPay
   - Sau thanh toán thành công → Đăng tin ngay

---

## IV. ID PATTERNS

### Pattern Format:
```
Users:                    USR-{yyyymmdd}-{6-digit-random}
Memberships:              PKG-{LEVEL}-{DURATION}
Transactions:             TXN-{yyyymmdd}-{type}-{6-digit}
Listings:                 LST-{yyyymmdd}-{6-digit}
User Memberships:         USRM-{yyyymmdd}-{6-digit}
Benefits:                 BNF-{type}-{sequential}
User Membership Benefits: UMB-{yyyymmdd}-{6-digit}
Push History:             PSH-{yyyymmdd}-{6-digit}
```

### Examples:
```
USR-20250101-847291
PKG-STANDARD-1M
TXN-20250101-MEM-385729
LST-20250102-192847
USRM-20250101-573829
BNF-VIP-001
UMB-20250101-847392
PSH-20250103-283947
```

---

## V. SCENARIO CẬP NHẬT

**Timeline:**
- **01/01/2025 10:00** - Minh mua Gói Tiêu Chuẩn 1 tháng qua VNPay
- **02/01/2025 14:00** - Minh đăng tin VIP Bạc (dùng quota)
- **03/01/2025 15:00** - Minh đẩy tin (dùng quota)
- **05/01/2025 11:00** - Minh đẩy tin lần 2 (hết quota, trả per-push)
- **10/01/2025 16:00** - Minh đăng tin VIP Kim Cương (dùng quota)
- **15/01/2025 09:00** - Minh đăng tin VIP Vàng (hết quota, trả per-post)
- **01/02/2025 00:00** - Membership hết hạn

---

## BƯỚC 0: Khởi tạo

### benefits
```
benefit_id   | benefit_name              | benefit_type | metadata
-------------|---------------------------|--------------|----------
BNF-VIP-001  | Đăng VIP Bạc miễn phí     | POST_SILVER  | ...
BNF-VIP-002  | Đăng VIP Vàng miễn phí    | POST_GOLD    | ...
BNF-VIP-003  | Đăng VIP KC miễn phí      | POST_DIAMOND | ...
BNF-PSH-001  | Đẩy bài miễn phí          | PUSH         | ...
BNF-APV-001  | Kiểm duyệt tự động        | AUTO_APPROVE | ...
BNF-BDG-001  | Badge đối tác tin cậy     | BADGE        | ...
```

### users
```
user_id            | email          | first_name | last_name | created_at
-------------------|----------------|------------|-----------|--------------------
USR-20250101-84729 | minh@email.com | Anh        | Minh      | 2025-01-01 08:00:00
```

### membership_packages (Admin đã tạo sẵn)
```
membership_id   | package_name            | package_level | duration_months | original_price | sale_price | discount_pct
----------------|-------------------------|---------------|-----------------|----------------|------------|-------------
PKG-BASIC-1M    | Gói Cơ Bản 1 Tháng     | BASIC         | 1               | 1,000,000      | 700,000    | 30.00
PKG-STANDARD-1M | Gói Tiêu Chuẩn 1 Tháng | STANDARD      | 1               | 2,000,000      | 1,400,000  | 30.00
PKG-ADVANCED-1M | Gói Nâng Cao 1 Tháng   | ADVANCED      | 1               | 4,000,000      | 2,800,000  | 30.00
```

### membership_package_benefits (PER MONTH)
```
benefit_id  | membership_id   | benefit_name_display        | quantity_per_month
------------|-----------------|-----------------------------|-----------------
BNF-VIP-001 | PKG-BASIC-1M    | 5 tin VIP Bạc miễn phí      | 5
BNF-BST-001 | PKG-BASIC-1M    | 10 lượt đẩy tin miễn phí    | 10
BNF-VIP-001 | PKG-STANDARD-1M | 10 tin VIP Bạc miễn phí     | 10
BNF-VIP-002 | PKG-STANDARD-1M | 5 tin VIP Vàng miễn phí     | 5
BNF-VIP-003 | PKG-STANDARD-1M | 2 tin VIP KC miễn phí       | 2
BNF-BST-001 | PKG-STANDARD-1M | 20 lượt đẩy tin miễn phí    | 20
BNF-APV-001 | PKG-STANDARD-1M | Duyệt tin ngay lập tức      | 1
BNF-VIP-001 | PKG-ADVANCED-1M | 15 tin VIP Bạc miễn phí     | 15
BNF-VIP-002 | PKG-ADVANCED-1M | 10 tin VIP Vàng miễn phí    | 10
BNF-VIP-003 | PKG-ADVANCED-1M | 5 tin VIP KC miễn phí       | 5
BNF-BST-001 | PKG-ADVANCED-1M | 40 lượt đẩy tin miễn phí    | 40
BNF-APV-001 | PKG-ADVANCED-1M | Duyệt tin ngay lập tức      | 1
BNF-BDG-001 | PKG-ADVANCED-1M | Badge đối tác tin cậy       | 1
```

---

## BƯỚC 1: Minh mua Gói Tiêu Chuẩn (01/01/2025 10:00)

**Action:** Thanh toán 1,400,000 VND qua VNPay

**LOGIC:**
- total_quantity = quantity_per_month × duration_months
- 10 VIP Bạc/tháng × 1 = **10 tin VIP Bạc**
- 5 VIP Vàng/tháng × 1 = **5 tin VIP Vàng**
- 2 VIP KC/tháng × 1 = **2 tin VIP KC**
- 20 Push/tháng × 1 = **20 lượt Push**

### transactions
```
transaction_id        | user_id            | amount    | transaction_type | reference_type | reference_id    | status    | payment_provider | provider_tx_id | created_at
----------------------|--------------------|-----------|------------------|----------------|-----------------|-----------|------------------|----------------|--------------------
TXN-20250101-MEM-3857 | USR-20250101-84729 | 1,400,000 | MEMBERSHIP       | MEMBERSHIP     | PKG-STANDARD-1M | COMPLETED | VNPAY            | VNP20250101001 | 2025-01-01 10:00:00
```

### user_memberships
```
user_membership_id  | user_id            | membership_id   | start_date          | end_date            | duration | status | total_paid | transaction_id        | created_at
--------------------|--------------------|-----------------|---------------------|---------------------|----------|--------|------------|-----------------------|--------------------
USRM-20250101-57382 | USR-20250101-84729 | PKG-STANDARD-1M | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 30       | ACTIVE | 1,400,000  | TXN-20250101-MEM-3857 | 2025-01-01 10:00:00
```

### user_membership_benefits
```
user_benefit_id     | user_membership_id  | benefit_id  | user_id            | granted_at          | expires_at          | total_quantity | quantity_used | status
--------------------|---------------------|-------------|--------------------|---------------------|---------------------|----------------|---------------|-------
UMB-20250101-84739  | USRM-20250101-57382 | BNF-VIP-001 | USR-20250101-84729 | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 10             | 0             | ACTIVE
UMB-20250101-84740  | USRM-20250101-57382 | BNF-VIP-002 | USR-20250101-84729 | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 5              | 0             | ACTIVE
UMB-20250101-84741  | USRM-20250101-57382 | BNF-VIP-003 | USR-20250101-84729 | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 2              | 0             | ACTIVE
UMB-20250101-84742  | USRM-20250101-57382 | BNF-BST-001 | USR-20250101-84729 | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 20             | 0             | ACTIVE
UMB-20250101-84743  | USRM-20250101-57382 | BNF-APV-001 | USR-20250101-84729 | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | 1              | 0             | ACTIVE
```

**Giá trị nhận được:**
```
10 VIP Bạc × 1.2M = 12,225,000
5 VIP Vàng × 2.7M = 13,447,500
2 VIP KC × 6.8M = 13,692,000
20 Push × 40k = 800,000
------------------------
Total value: 40,164,500 VND
Paid: 1,400,000 VND
ROI: 2,869%
```

---

## BƯỚC 2: Minh đăng tin VIP Bạc (02/01/2025 14:00)

**Flow:**
1. Minh click "Đăng tin"
2. Màn hình "Chọn loại tin" hiển thị:
   - VIP KC: Còn 2/2 → DÙNG QUOTA
   - VIP Vàng: Còn 5/5 → DÙNG QUOTA
   - VIP Bạc: Còn 10/10 → DÙNG QUOTA
3. Minh chọn VIP Bạc
4. Điền thông tin, chọn 30 ngày
5. Submit → Đăng tin ngay (không thanh toán)

### listings
```
listing_id          | user_id            | title                      | vip_type | price      | duration_days | is_verified | expired | post_source | transaction_id | post_date           | created_at
--------------------|--------------------|----------------------------|----------|------------|---------------|-------------|---------|-------------|----------------|---------------------|--------------------
LST-20250102-19284  | USR-20250101-84729 | Cho thuê căn hộ 2PN Q7     | SILVER   | 15,000,000 | 30            | TRUE        | FALSE   | QUOTA       | NULL           | 2025-01-02 14:00:00 | 2025-01-02 14:00:00
```

### user_membership_benefits (UPDATED)
```
user_benefit_id     | benefit_id  | total_quantity | quantity_used | status
--------------------|-------------|----------------|---------------|-------
UMB-20250101-84739  | BNF-VIP-001 | 10             | 1             | ACTIVE (9/10)
UMB-20250101-84740  | BNF-VIP-002 | 5              | 0             | ACTIVE
UMB-20250101-84741  | BNF-VIP-003 | 2              | 0             | ACTIVE
UMB-20250101-84742  | BNF-PSH-001 | 20             | 0             | ACTIVE
UMB-20250101-84743  | BNF-APV-001 | 1              | 0             | ACTIVE
```

---

## BƯỚC 3: Minh đẩy tin (03/01/2025 15:00)

**Action:** Đẩy tin #LST-20250102-19284 - Dùng quota

### listings (UPDATED)
```
listing_id          | post_date           | pushed_at
--------------------|---------------------|--------------------
LST-20250102-19284  | 2025-01-03 15:00:00 | 2025-01-03 15:00:00
```

### user_membership_benefits (UPDATED)
```
user_benefit_id     | benefit_id  | total_quantity | quantity_used | status
--------------------|-------------|----------------|---------------|-------
UMB-20250101-84739  | BNF-VIP-001 | 10             | 1             | ACTIVE
UMB-20250101-84740  | BNF-VIP-002 | 5              | 0             | ACTIVE
UMB-20250101-84741  | BNF-VIP-003 | 2              | 0             | ACTIVE
UMB-20250101-84742  | BNF-PSH-001 | 20             | 1             | ACTIVE (19/20)
UMB-20250101-84743  | BNF-APV-001 | 1              | 0             | ACTIVE
```

### push_history
```
push_id             | listing_id         | user_id            | push_source      | user_benefit_id     | transaction_id | pushed_at
--------------------|--------------------|--------------------|------------------|---------------------|----------------|--------------------
PSH-20250103-28394  | LST-20250102-19284 | USR-20250101-84729 | MEMBERSHIP_QUOTA | UMB-20250101-84742  | NULL           | 2025-01-03 15:00:00
```

---

## BƯỚC 4: Minh đẩy 19 lần nữa → HẾT QUOTA

### user_membership_benefits (UPDATED)
```
user_benefit_id     | benefit_id  | total_quantity | quantity_used | status
--------------------|-------------|----------------|---------------|-------
UMB-20250101-84739  | BNF-VIP-001 | 10             | 1             | ACTIVE
UMB-20250101-84740  | BNF-VIP-002 | 5              | 0             | ACTIVE
UMB-20250101-84741  | BNF-VIP-003 | 2              | 0             | ACTIVE
UMB-20250101-84742  | BNF-PSH-001 | 20             | 20            | FULLY_USED
UMB-20250101-84743  | BNF-APV-001 | 1              | 0             | ACTIVE
```

---

## BƯỚC 5: Minh hết quota push, trả tiền (05/01/2025 11:00)

**Flow:**
1. Minh click "Đẩy tin" trên listing
2. System check: Hết quota push (20/20)
3. Hiển thị popup:
```
┌────────────────────────────────────────┐
│  BẠN ĐÃ HẾT LƯỢT ĐẨY TIN MIỄN PHÍ    │
│                                        │
│  🎁 Mua gói membership → Nhiều quota   │
│  💳 Trả 40,000 VND cho lần này        │
│                                        │
│  [MUA GÓI]     [TRẢ 40K]      [HUỶ]   │
└────────────────────────────────────────┘
```
4. Minh chọn "TRẢ 40K"
5. Redirect VNPay → Thanh toán thành công
6. Đẩy tin ngay

### transactions
```
transaction_id        | user_id            | amount | transaction_type | reference_type | reference_id       | status    | payment_provider | provider_tx_id | created_at
----------------------|--------------------|--------|------------------|----------------|--------------------|-----------|------------------|----------------|--------------------
TXN-20250105-PSH-4729 | USR-20250101-84729 | 40,000 | PUSH_FEE         | PUSH           | LST-20250102-19284 | COMPLETED | VNPAY            | VNP20250105002 | 2025-01-05 11:00:00
```

### push_history
```
push_id             | listing_id         | user_id            | push_source    | user_benefit_id | transaction_id        | pushed_at
--------------------|--------------------|--------------------|----------------|-----------------|------------------------|--------------------
PSH-20250105-74829  | LST-20250102-19284 | USR-20250101-84729 | DIRECT_PAYMENT | NULL            | TXN-20250105-BST-4729 | 2025-01-05 11:00:00
```

---

## BƯỚC 6: Minh đăng VIP Kim Cương (10/01/2025 16:00)

**Flow:**
1. Click "Đăng tin"
2. Chọn VIP Kim Cương (Còn 2/2 quota)
3. Điền thông tin, chọn 30 ngày
4. Submit → Đăng tin ngay + Tạo tin shadow

### listings (VIP KC chính)
```
listing_id          | user_id            | title               | vip_type | price      | duration_days | is_verified | is_shadow | parent_listing_id | post_source | transaction_id | post_date           | created_at
--------------------|--------------------|---------------------|----------|------------|---------------|-------------|-----------|-------------------|-------------|----------------|---------------------|--------------------
LST-20250110-84729  | USR-20250101-84729 | Bán biệt thự Q2     | DIAMOND  | 25,000,000 | 30            | TRUE        | FALSE     | NULL              | QUOTA       | NULL           | 2025-01-10 16:00:00 | 2025-01-10 16:00:00
```

### listings (Tin thường đi kèm)
```
listing_id          | user_id            | title               | vip_type | price      | duration_days | is_verified | is_shadow | parent_listing_id  | post_source | transaction_id | post_date           | created_at
--------------------|--------------------|---------------------|----------|------------|---------------|-------------|-----------|-------------------|-------------|----------------|---------------------|--------------------
LST-20250110-84730  | USR-20250101-84729 | Bán biệt thự Q2     | NORMAL   | 25,000,000 | 30            | TRUE        | TRUE      | LST-20250110-84729| QUOTA       | NULL           | 2025-01-10 16:00:00 | 2025-01-10 16:00:00
```

### user_membership_benefits (UPDATED)
```
user_benefit_id     | benefit_id  | total_quantity | quantity_used | status
--------------------|-------------|----------------|---------------|-------
UMB-20250101-84739  | BNF-VIP-001 | 10             | 1             | ACTIVE
UMB-20250101-84740  | BNF-VIP-002 | 5              | 0             | ACTIVE
UMB-20250101-84741  | BNF-VIP-003 | 2              | 1             | ACTIVE (1/2)
UMB-20250101-84742  | BNF-BST-001 | 20             | 20            | FULLY_USED
UMB-20250101-84743  | BNF-APV-001 | 1              | 0             | ACTIVE
```

---

## BƯỚC 7: Minh hết quota VIP Vàng, trả per-post (15/01/2025)

**Giả sử:** Minh đã dùng hết 5/5 VIP Vàng

**Flow:**
1. Click "Đăng tin"
2. Chọn VIP Vàng → Hiển thị "HẾT QUOTA - THANH TOÁN"
3. Click vào → Popup:
```
┌──────────────────────────────────────────┐
│  BẠN ĐÃ HẾT QUOTA VIP VÀNG              │
│                                          │
│  🎁 Mua gói → Nhận thêm 5-10 tin VIP     │
│  💳 Trả 2,689,500 cho tin này            │
│                                          │
│  [MUA GÓI]    [THANH TOÁN]      [HUỶ]    │
└──────────────────────────────────────────┘
```
4. Chọn "THANH TOÁN"
5. Redirect VNPay → Thanh toán 2,689,500
6. Đăng tin ngay

### transactions
```
transaction_id        | user_id            | amount    | transaction_type | reference_type | reference_id       | status    | payment_provider | provider_tx_id | created_at
----------------------|--------------------|-----------|------------------|----------------|--------------------|-----------|-----------------|--------------------|--------------------
TXN-20250115-PST-8392 | USR-20250101-84729 | 2,689,500 | POST_FEE         | LISTING        | LST-20250115-93847 | COMPLETED | VNPAY           | VNP20250115003     | 2025-01-15 09:00:00
```

### listings
```
listing_id          | user_id            | title                  | vip_type | price      | duration_days | is_verified | post_source    | transaction_id        | post_date           | created_at
--------------------|--------------------|------------------------|----------|------------|---------------|-------------|----------------|-----------------------|---------------------|--------------------
LST-20250115-93847  | USR-20250101-84729 | Văn phòng Q1 cho thuê  | GOLD     | 20,000,000 | 30            | TRUE        | DIRECT_PAYMENT | TXN-20250115-PST-8392 | 2025-01-15 09:00:00 | 2025-01-15 09:00:00
```

**Lưu ý:**
- Quota VIP Vàng vẫn 5/5 (không tăng)
- Tin này không tính vào quota
- User trả tiền cho TIN NÀY, không nhận quota

---

## BƯỚC 8: Membership hết hạn (01/02/2025 00:00)

### user_memberships (UPDATED)
```
user_membership_id  | user_id            | membership_id   | start_date          | end_date            | status  | updated_at
--------------------|--------------------|-----------------|---------------------|---------------------|---------|--------------------
USRM-20250101-57382 | USR-20250101-84729 | PKG-STANDARD-1M | 2025-01-01 10:00:00 | 2025-02-01 10:00:00 | EXPIRED | 2025-02-01 00:00:00
```

### user_membership_benefits (ALL EXPIRED)
```
user_benefit_id     | benefit_id  | total_quantity | quantity_used | status
--------------------|-------------|----------------|---------------|-------
UMB-20250101-84739  | BNF-VIP-001 | 10             | 1             | EXPIRED (MẤT 9)
UMB-20250101-84740  | BNF-VIP-002 | 5              | 5             | EXPIRED
UMB-20250101-84741  | BNF-VIP-003 | 2              | 1             | EXPIRED (MẤT 1)
UMB-20250101-84742  | BNF-BST-001 | 20             | 20            | EXPIRED
UMB-20250101-84743  | BNF-APV-001 | 1              | 0             | EXPIRED
```

### Listings status
```
#LST-20250102-19284: VIP Bạc - Hết hạn 01/02
#LST-20250110-84729: VIP KC - Còn active (hết 09/02)
#LST-20250110-84730: NORMAL (shadow) - Còn active
#LST-20250115-93847: VIP Vàng (per-post) - Còn active (hết 14/02)
```

---

## VI. TỔNG KẾT

### Chi tiêu qua VNPay
```
01/01: Membership          1,400,000 VND
05/01: Push per-post          40,000 VND
15/01: VIP Vàng per-post   2,689,500 VND
-------------------------------------------
TOTAL:                     4,129,500 VND
```

### Quota sử dụng
```
VIP Bạc:   1/10 (tiết kiệm 1,222,500) - MẤT 9
VIP Vàng:  5/5  (tiết kiệm 13,447,500)
VIP KC:    1/2  (tiết kiệm 6,846,000) - MẤT 1
Push:     20/20 (tiết kiệm 800,000)
```

### Listings đã tạo
```
1. VIP Bạc (quota) - Hết hạn
2. VIP KC (quota) - Còn active
3. NORMAL shadow - Còn active
4. VIP Vàng (per-post) - Còn active
```

### ROI Analysis
```
Từ Membership (1,400,000):
- 1 VIP Bạc × 1.2M = 1,222,500
- 5 VIP Vàng × 2.7M = 13,447,500
- 1 VIP KC × 6.8M = 6,846,000
- 1 NORMAL shadow = 66,000
- 20 Push × 40k = 800,000
Subtotal: 22,382,000 VND

Từ Per-post (2,729,500):
- 1 VIP Vàng = 2,689,500
- 1 Push = 40,000
Subtotal: 2,729,500 VND

GRAND TOTAL VALUE: 25,111,500 VND
Chi: 4,129,500 VND
ROI: 608%
```

---

## VII. DATABASE SCHEMA

### 1. users
```sql
CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email)
);
```

### 2. benefits
```sql
CREATE TABLE benefits (
    benefit_id VARCHAR(50) PRIMARY KEY,
    benefit_name VARCHAR(255) NOT NULL,
    benefit_type ENUM('POST_SILVER', 'POST_GOLD', 'POST_DIAMOND', 'PUSH', 'AUTO_APPROVE', 'BADGE') NOT NULL,
    description TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_benefit_type (benefit_type)
);
```

### 3. membership_packages
```sql
CREATE TABLE membership_packages (
    membership_id VARCHAR(50) PRIMARY KEY,
    package_name VARCHAR(255) NOT NULL,
    package_level ENUM('BASIC', 'STANDARD', 'ADVANCED') NOT NULL,
    duration_months INT NOT NULL,
    original_price DECIMAL(15,2) NOT NULL,
    sale_price DECIMAL(15,2) NOT NULL,
    discount_percentage DECIMAL(5,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_package_level (package_level),
    INDEX idx_active (is_active)
);
```

### 4. membership_package_benefits
```sql
CREATE TABLE membership_package_benefits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    benefit_id VARCHAR(50) NOT NULL,
    membership_id VARCHAR(50) NOT NULL,
    benefit_name_display VARCHAR(255) NOT NULL,
    quantity_per_month INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (benefit_id) REFERENCES benefits(benefit_id),
    FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id),
    UNIQUE KEY unique_benefit_package (benefit_id, membership_id),
    INDEX idx_membership (membership_id)
);
```

### 5. transactions
```sql
CREATE TABLE transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type ENUM('MEMBERSHIP', 'POST_FEE', 'PUSH_FEE') NOT NULL,
    reference_type ENUM('MEMBERSHIP', 'LISTING', 'PUSH') NOT NULL,
    reference_id VARCHAR(50),
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL,
    payment_provider VARCHAR(20) NOT NULL DEFAULT 'VNPAY',
    provider_tx_id VARCHAR(100),
    additional_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_user_transactions (user_id, created_at),
    INDEX idx_provider_tx (provider_tx_id),
    INDEX idx_status (status)
);
```

### 6. user_memberships
```sql
CREATE TABLE user_memberships (
    user_membership_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    membership_id VARCHAR(50) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    duration INT NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    total_paid DECIMAL(15,2) NOT NULL,
    transaction_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (membership_id) REFERENCES membership_packages(membership_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_user_membership (user_id, status),
    INDEX idx_status_dates (status, start_date, end_date)
);
```

### 7. user_membership_benefits
```sql
CREATE TABLE user_membership_benefits (
    user_benefit_id VARCHAR(50) PRIMARY KEY,
    user_membership_id VARCHAR(50) NOT NULL,
    benefit_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    total_quantity INT NOT NULL,
    quantity_used INT DEFAULT 0,
    status ENUM('ACTIVE', 'FULLY_USED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_membership_id) REFERENCES user_memberships(user_membership_id),
    FOREIGN KEY (benefit_id) REFERENCES benefits(benefit_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_user_benefits (user_id, status),
    INDEX idx_benefit_status (benefit_id, status)
);
```

### 8. listings
```sql
CREATE TABLE listings (
    listing_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    vip_type ENUM('NORMAL', 'SILVER', 'GOLD', 'DIAMOND') NOT NULL DEFAULT 'NORMAL',
    price DECIMAL(15,2),
    duration_days INT NOT NULL DEFAULT 30,
    is_verified BOOLEAN DEFAULT FALSE,
    is_shadow BOOLEAN DEFAULT FALSE,
    parent_listing_id VARCHAR(50),
    post_source ENUM('QUOTA', 'DIRECT_PAYMENT') NOT NULL,
    transaction_id VARCHAR(50),
    expired BOOLEAN DEFAULT FALSE,
    post_date TIMESTAMP,
    pushed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (parent_listing_id) REFERENCES listings(listing_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_user_listings (user_id, created_at),
    INDEX idx_post_date (post_date),
    INDEX idx_vip_type (vip_type, post_date),
    INDEX idx_expired (expired, vip_type)
);
```

### 9. push_history
```sql
CREATE TABLE push_history (
    push_id VARCHAR(50) PRIMARY KEY,
    listing_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    push_source ENUM('MEMBERSHIP_QUOTA', 'DIRECT_PAYMENT') NOT NULL,
    user_benefit_id VARCHAR(50),
    transaction_id VARCHAR(50),
    pushed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (listing_id) REFERENCES listings(listing_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (user_benefit_id) REFERENCES user_membership_benefits(user_benefit_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_listing_push (listing_id, pushed_at),
    INDEX idx_user_push (user_id, pushed_at)
);
```

---

## VIII. BUSINESS RULES

### 1. Payment Rules
- Mọi thanh toán phải qua VNPay
- Không lưu trữ số dư (no wallet)
- Mỗi transaction phải có provider_tx_id
- Transaction status: PENDING → COMPLETED/FAILED

### 2. Posting Rules
- Có quota → Dùng quota (post_source = QUOTA)
- Hết quota → Pay per-post (post_source = DIRECT_PAYMENT)
- Per-post KHÔNG tạo quota mới
- VIP Kim Cương tự động tạo shadow listing (NORMAL)

### 3. Quota Rules
- Tính theo: quantity_per_month × duration_months
- Quota không dùng hết → MẤT khi membership hết hạn
- Không tự động gia hạn
- Status progression: ACTIVE → FULLY_USED/EXPIRED

### 4. Pricing Rules (30 ngày)
```
NORMAL:    66,000 VND
VIP BẠC:   1,222,500 VND
VIP VÀNG:  2,689,500 VND
VIP KC:    6,846,000 VND
PUSH:      40,000 VND/lần
```

### 5. Duration Discount
```
10 ngày: Giá gốc (100%)
15 ngày: Discount ~11%
30 ngày: Discount ~18.5%
```

### 6. Display Priority
```
1. VIP KIM CƯƠNG (DIAMOND) - Cao nhất
2. VIP VÀNG (GOLD)
3. VIP BẠC (SILVER)
4. TIN THƯỜNG (NORMAL) - Thấp nhất

Trong cùng tier → Sort by post_date DESC
```

---

## IX. KEY DIFFERENCES (vs Old Version)

### 1. Số loại tin
```
CŨ: 3 loại (NORMAL, VIP, PREMIUM)
MỚI: 4 loại (NORMAL, SILVER, GOLD, DIAMOND)
```

### 2. Pricing
```
CŨ:
- NORMAL: 3k/ngày
- VIP: 20k/ngày
- PREMIUM: 60k/ngày

MỚI:
- NORMAL: 2.7k/ngày
- SILVER: 50k/ngày
- GOLD: 110k/ngày
- DIAMOND: 280k/ngày
```

### 3. ID System
```
CŨ: Auto-increment integers
MỚI: Pattern-based strings với prefix
```

### 4. Flow chọn tin
```
CŨ: Simple form selection
MỚI: Card-based với quota indicator real-time
```

### 5. Shadow Listing
```
CŨ: PREMIUM có tin NORMAL đi kèm
MỚI: DIAMOND có tin NORMAL đi kèm
```

---

## X. UI/UX NOTES

### Màn hình "Chọn loại tin"
- Hiển thị 4 cards theo thứ tự: DIAMOND → GOLD → SILVER → NORMAL
- Mỗi card show:
   - Tên loại tin + Badge preview
   - Giá per day
   - Quota status (nếu có membership):
      - "Còn X/Y" → Button "DÙNG QUOTA"
      - "Hết quota" → Button "THANH TOÁN + Giá"
   - Nếu không có membership → Chỉ show "THANH TOÁN"

### Popup hết quota
- Clear comparison giữa 2 options
- Highlight membership package benefits
- Show ROI calculation
- Easy navigation: [MUA GÓI] [THANH TOÁN] [HUỶ]

### Quota indicator
- Real-time update
- Visual progress bar (optional)
- Color coding:
   - Green: >50% remaining
   - Yellow: 20-50% remaining
   - Red: <20% remaining
   - Gray: 0% (hết quota)

---

## XI. API ENDPOINTS (Suggested)

### POST /api/listings/check-quota
```json
Request:
{
  "user_id": "USR-20250101-84729",
  "vip_type": "GOLD"
}

Response:
{
  "has_quota": true,
  "remaining": 3,
  "total": 5,
  "expires_at": "2025-02-01T10:00:00Z"
}
```

### POST /api/listings/create-with-payment
```json
Request:
{
  "user_id": "USR-20250101-84729",
  "vip_type": "GOLD",
  "duration_days": 30,
  "listing_data": {...}
}

Response:
{
  "payment_url": "https://vnpay.vn/payment/...",
  "transaction_id": "TXN-20250115-PST-8392",
  "amount": 2689500
}
```

### POST /api/push/push-listing
```json
Request:
{
  "user_id": "USR-20250101-84729",
  "listing_id": "LST-20250102-19284",
  "use_quota": false
}

Response:
{
  "success": true,
  "payment_required": true,
  "payment_url": "https://vnpay.vn/payment/...",
  "transaction_id": "TXN-20250105-BST-4729"
}
```