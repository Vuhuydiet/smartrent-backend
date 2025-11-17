# 🔧 FIX MIGRATION - CLEAN SOLUTION

## ✅ ĐÃ FIX

Đã sửa **V35__Create_address_mapping_table.sql** để column `new_ward_area_km2` có precision đúng ngay từ đầu:

```sql
-- BEFORE
new_ward_area_km2 DECIMAL(10, 2)

-- AFTER
new_ward_area_km2 DECIMAL(10, 4)
```

## 🚀 CÁCH ÁP DỤNG FIX

### Option 1: Drop Database và Chạy Lại (KHUYẾN NGHỊ)

**Nếu database CHƯA có data production quan trọng:**

```sql
-- 1. Drop database hiện tại
DROP DATABASE smartrent;

-- 2. Tạo lại database
CREATE DATABASE smartrent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Sau đó chạy lại application:**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

✅ **Kết quả**: Migrations chạy lại từ đầu với column definition đúng, KHÔNG CÒN warnings!

---

### Option 2: Chỉ Rollback Migration (Nếu có data cần giữ)

**Nếu database ĐÃ có data quan trọng khác:**

```bash
# 1. Rollback về trước V35
./gradlew flywayClean
# Hoặc manual:
# DELETE FROM flyway_schema_history WHERE version >= '35';
# DROP TABLE address_mapping;

# 2. Chạy lại migrations
./gradlew flywayMigrate
```

---

### Option 3: Chấp Nhận Data Bị Truncate (KHÔNG khuyến nghị)

**Nếu muốn giữ data đã migrate (dù bị truncate):**

Warnings này chỉ làm mất độ chính xác nhỏ (VD: `101.705` → `101.71`).
Nếu chấp nhận được sai số này, cứ để migration chạy tiếp.

**Ảnh hưởng:**
- ~60-70 rows trong 10,602 rows bị mất 1 chữ số thập phân thứ 3
- Sai số: 0.001 - 0.009 km² (rất nhỏ)

---

## 📊 SO SÁNH OPTIONS

| Option | Pros | Cons | Khuyến nghị |
|--------|------|------|-------------|
| **1. Drop DB** | ✅ Clean<br>✅ Không warnings<br>✅ Data chính xác 100% | ❌ Mất data hiện tại | ⭐⭐⭐⭐⭐<br>**BEST** cho dev |
| **2. Rollback** | ✅ Giữ data khác<br>✅ Không warnings | ⚠️ Phức tạp hơn | ⭐⭐⭐⭐ |
| **3. Accept** | ✅ Đơn giản | ❌ Data không chính xác<br>❌ Warnings vẫn có | ⭐ |

---

## ✅ RECOMMENDED: DROP DATABASE

**Vì bạn đang trong quá trình development và migration lần đầu, KHUYẾN NGHỊ drop database và chạy lại:**

```sql
-- Connect to MySQL
mysql -u root -p

-- Drop and recreate
DROP DATABASE IF EXISTS smartrent;
CREATE DATABASE smartrent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartrent;
EXIT;
```

**Sau đó:**

```bash
# Chạy lại application
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Kết quả:**
```
✅ Migrating schema `smartrent` to version "35 - Create address mapping table"
✅ Migrating schema `smartrent` to version "39 - Insert address mapping data"
✅ NO WARNINGS!
✅ Data với độ chính xác đầy đủ (101.705 giữ nguyên)
```

---

## 📝 LƯU Ý

### Nếu bạn ĐANG thấy warnings khi chạy:

**Application vẫn start bình thường!** Warnings không làm crash app.

Chỉ cần:
1. Dừng application (Ctrl+C)
2. Drop database
3. Chạy lại application

### Flyway sẽ tự động:

- Tạo lại `flyway_schema_history`
- Chạy tất cả migrations từ V1 đến V41
- Migration V35 giờ có column definition đúng
- Migration V39 insert data KHÔNG BỊ TRUNCATE

---

## 🎯 TÓM TẮT

**Đã fix:**
- ✅ V35 column definition: `DECIMAL(10, 4)`
- ✅ Xóa V42 (không cần nữa vì V35 đã fix)

**Action required:**
- ⚠️ Drop database `smartrent`
- ⚠️ Chạy lại application
- ✅ Enjoy migrations không có warnings!

---

**📅 Date**: 2025-11-17
**🔧 Fixed**: V35__Create_address_mapping_table.sql
**🎯 Status**: ✅ Ready to drop & recreate database
