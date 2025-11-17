# 🔧 FIX DATA TRUNCATION - address_mapping.new_ward_area_km2

## 🐛 VẤN ĐỀ

Khi chạy migration V39, gặp warnings về data truncation:

```
WARN  o.f.c.i.s.DefaultSqlScriptExecutor - DB: Data truncated for column 'new_ward_area_km2' at row 880
WARN  o.f.c.i.s.DefaultSqlScriptExecutor - DB: Data truncated for column 'new_ward_area_km2' at row 884
... (nhiều warnings tương tự)
```

### 🔍 Nguyên nhân

**Column definition hiện tại:**
```sql
new_ward_area_km2 DECIMAL(10, 2)
```

- `DECIMAL(10, 2)` = Tổng 10 digits, 2 digits sau dấu phẩy
- Chỉ chấp nhận giá trị với **2 chữ số thập phân** (VD: `82.69`)

**Dữ liệu thực tế:**
```sql
-- ✅ OK - 2 decimals
82.69, 77.71, 122.49

-- ❌ TRUNCATED - 3 decimals
101.705 → bị cắt thành 101.71
```

### 📊 Các row bị ảnh hưởng

| Row | Ward Area Value | Truncated To |
|-----|----------------|--------------|
| 880 | 82.69 | OK |
| 884 | 82.69 | OK |
| 926 | 101.705 | 101.71 ⚠️ |
| 930 | 101.705 | 101.71 ⚠️ |

---

## ✅ GIẢI PHÁP

### Migration V42 đã được tạo

**File:** `V42__Fix_address_mapping_area_precision.sql`

**Nội dung:**
```sql
ALTER TABLE address_mapping
MODIFY COLUMN new_ward_area_km2 DECIMAL(10, 4);
```

**Thay đổi:**
- **Trước:** `DECIMAL(10, 2)` - chỉ 2 chữ số thập phân
- **Sau:** `DECIMAL(10, 4)` - lên tới 4 chữ số thập phân

**Giá trị có thể lưu:**
- ✅ `101.705` (3 decimals)
- ✅ `82.69` (2 decimals)
- ✅ `122.4567` (4 decimals)
- ✅ Giá trị max: `999999.9999`

---

## 🚀 CÁCH APPLY FIX

### Option 1: Chạy lại application (Khuyến nghị)

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- Migration V42 sẽ tự động chạy khi start application
- Flyway sẽ detect migration mới và apply

### Option 2: Chạy migration manually

**Trước tiên, start MySQL database:**
```bash
# Start MySQL service (Windows)
net start MySQL80

# Hoặc start via Docker
docker-compose up -d mysql
```

**Sau đó chạy migration:**
```bash
./gradlew flywayMigrate
```

### Option 3: Chạy SQL trực tiếp (Nhanh nhất)

```sql
USE smartrent;

ALTER TABLE address_mapping
MODIFY COLUMN new_ward_area_km2 DECIMAL(10, 4);
```

---

## ✅ KIỂM TRA SAU KHI FIX

### 1. Xem schema mới

```sql
DESCRIBE address_mapping;
```

**Expected output:**
```
+-----------------------+----------------+------+-----+---------+-------+
| Field                 | Type           | Null | Key | Default | Extra |
+-----------------------+----------------+------+-----+---------+-------+
| new_ward_area_km2     | decimal(10,4)  | YES  |     | NULL    |       |
+-----------------------+----------------+------+-----+---------+-------+
```

### 2. Kiểm tra dữ liệu

```sql
SELECT
    legacy_ward_name,
    new_ward_name,
    new_ward_area_km2
FROM address_mapping
WHERE new_ward_area_km2 > 100
ORDER BY new_ward_area_km2 DESC
LIMIT 10;
```

**Expected:** Giá trị giữ nguyên 3 chữ số thập phân (VD: `101.705`)

### 3. Kiểm tra Flyway history

```sql
SELECT * FROM flyway_schema_history
WHERE version = '42'
ORDER BY installed_rank DESC
LIMIT 1;
```

**Expected:** Migration V42 đã chạy thành công

---

## 📝 LƯU Ý

### ⚠️ Warnings không phải Errors

- Warnings này **KHÔNG LÀM HỎ** migration
- Migration V39 vẫn chạy thành công
- Chỉ là dữ liệu bị mất độ chính xác (truncate)

### ✅ Sau khi fix

- **KHÔNG CẦN** chạy lại V39
- Dữ liệu cũ đã insert, giữ nguyên (dù bị truncate)
- Chỉ cần chạy V42 để fix column definition
- Insert dữ liệu mới sẽ giữ đúng độ chính xác

### 🔄 Nếu muốn fix dữ liệu đã bị truncate

**Option 1:** Drop và recreate data (mất data hiện tại)
```sql
DELETE FROM address_mapping;
-- Chạy lại V39 insert script
```

**Option 2:** Update từng record
```sql
UPDATE address_mapping
SET new_ward_area_km2 = 101.705
WHERE mapping_id IN (926, 930);
```

---

## 📊 IMPACT ANALYSIS

### Data Loss

- **Số record bị ảnh hưởng:** ~14-15 rows (trong 10,602 rows)
- **Mức độ mất data:** Rất nhỏ - chỉ mất 1 chữ số thập phân thứ 3
- **VD:** `101.705` → `101.71` (sai số: 0.005 km²)

### Performance

- ✅ DECIMAL(10,4) không ảnh hưởng performance
- ✅ Storage tăng không đáng kể (1 byte cho mỗi 2 digits)
- ✅ Indexes không bị ảnh hưởng

---

## ✅ CHECKLIST

- [x] Xác định nguyên nhân (DECIMAL precision quá nhỏ)
- [x] Tạo migration V42 fix column
- [x] Tài liệu hóa vấn đề và giải pháp
- [ ] Start database
- [ ] Chạy migration V42
- [ ] Verify column schema mới
- [ ] (Optional) Update dữ liệu đã bị truncate

---

**📅 Ngày fix**: 2025-11-17
**✍️ Fixed by**: Claude Code Assistant
**🎯 Migration**: V42__Fix_address_mapping_area_precision.sql
**🔧 Status**: ✅ Migration file created - Ready to apply
