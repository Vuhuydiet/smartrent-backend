# Hướng Dẫn: Quy Trình Duyệt & Cập Nhật Tin Đăng

---

## Dành Cho Chủ Nhà (Owner)

### Các Trạng Thái Tin Đăng

| Trạng thái | Ý nghĩa | Bạn cần làm gì? |
|------------|---------|-----------------|
| 🟡 **Đang chờ duyệt** | Admin đang xem xét tin đăng | Chờ kết quả |
| 🟢 **Đang hiển thị** | Tin đăng đã được duyệt, đang hiển thị | Không cần làm gì |
| 🔴 **Bị từ chối** | Admin yêu cầu chỉnh sửa | **Sửa → Gửi lại** |
| ⛔ **Tạm ngưng** | Tin đăng bị tạm ngưng (admin tạm ngưng thủ công) | Sửa & gửi lại được |
| ⛔ **Đã gỡ (vĩnh viễn)** | Admin xác nhận vi phạm nghiêm trọng qua báo cáo và gỡ tin | **Không thể sửa/gửi lại** — liên hệ hỗ trợ nếu cho rằng nhầm lẫn |

> Cả "Tạm ngưng" và "Đã gỡ (vĩnh viễn)" đều hiển thị chung trạng thái `SUSPENDED` ở backend — điểm khác biệt duy nhất là gỡ vĩnh viễn không cho resubmit. FE phân biệt 2 trường hợp này bằng field **`permanentlyRemoved`** (boolean) có sẵn trên response của `GET /v1/listings/{id}` (owner-facing) và các API admin xem tin đăng — `true` nghĩa là đã gỡ vĩnh viễn, không có "Sửa & Gửi lại"; `false`/`null` nghĩa là chỉ tạm ngưng, vẫn resubmit được. Server cũng chặn ở tầng API: cố resubmit một tin `permanentlyRemoved=true` sẽ nhận lỗi `RESUBMIT_NOT_ALLOWED` (16003).

### Khi Tin Đăng Bị Từ Chối

1. Bạn sẽ nhận **email thông báo** từ SmartRent
2. Email có ghi lý do từ chối hoặc yêu cầu sửa
3. Vào ứng dụng, mở **Tin đăng của tôi** → lọc **Bị từ chối**
4. Nhấn **Chỉnh sửa & Gửi lại**
5. Sửa các thông tin theo yêu cầu
6. Nhập ghi chú cho admin (ví dụ: "Đã sửa tiêu đề và thêm ảnh")
7. Nhấn **Gửi lại để duyệt**
8. Tin đăng chuyển sang **Đang chờ duyệt**

### Khi Nhận Email "Report on Your Listing"

Email này có nghĩa: có người báo cáo tin đăng của bạn và admin đã xử lý. Nếu admin yêu cầu bạn cập nhật:

1. Mở ứng dụng → **Tin đăng của tôi**
2. Tin đăng sẽ có trạng thái **Bị từ chối**
3. Làm theo bước 4-8 ở trên

> **Lưu ý:** Nếu bạn không thấy trạng thái "Bị từ chối" sau khi nhận email, có nghĩa admin đã xử lý xong và tin đăng của bạn không bị ảnh hưởng.

---

## Dành Cho Admin

### Hai Hàng Đợi Cần Xử Lý

| Hàng đợi | Nguồn | Xem ở đâu |
|----------|-------|-----------|
| **Duyệt tin đăng** | Tin mới + tin gửi lại | Tab "Chờ duyệt" |
| **Xử lý báo cáo** | Người dùng báo cáo | Tab "Báo cáo" |

### Quy Trình Duyệt Tin Đăng

```
Tin mới / Tin gửi lại
    ↓
┌──────────────┐
│  Xem xét     │
└──────┬───────┘
       │
   ┌───┼───────────────┐
   │   │               │
   ▼   ▼               ▼
 Duyệt  Yêu cầu sửa  Từ chối
   │        │           │
   ▼        ▼           ▼
 Hiển thị  Chủ nhà    Chủ nhà
           sửa & gửi  sửa & gửi
           lại         lại
```

**Khi duyệt tin mới:**
- ✅ **Duyệt**: Tin đăng hiển thị ngay
- ✏️ **Yêu cầu sửa**: Ghi rõ cần sửa gì → chủ nhà nhận email → sửa → gửi lại
- ❌ **Từ chối**: Ghi rõ lý do → chủ nhà nhận email → sửa → gửi lại

### Quy Trình Xử Lý Báo Cáo

Khi nhận 1 báo cáo từ người dùng:

| Quyết định | Khi nào dùng | Ảnh hưởng đến tin đăng? | Thông báo gửi đi? |
|-----------|-------------|------------------------|--------------------|
| ✅ **Chấp nhận + Yêu cầu chủ nhà sửa** | Vi phạm nhẹ, có thể sửa được | ✅ Tin đăng → **Cần sửa** (`REVISION_REQUIRED`), chủ nhà phải sửa & gửi lại | 📧 Email + in-app cho **cả người báo cáo lẫn chủ nhà**, kèm lý do admin nhập |
| ✅ **Chấp nhận + Gỡ bài đăng** | Vi phạm nghiêm trọng, phải gỡ hẳn | ✅ Tin đăng → **Đã gỡ vĩnh viễn** (`SUSPENDED` + không cho resubmit) | 📧 Email + in-app riêng cho chủ nhà báo tin đã bị gỡ + lý do; người báo cáo nhận thông báo kết quả như thường |
| ✅ **Chấp nhận + Không yêu cầu sửa** | Báo cáo đúng, nhưng admin tự xử lý ngoài hệ thống (vd đã nhắc nhở qua kênh khác) | ❌ Không đổi | 📧 Email + in-app cho cả 2 bên, kèm lý do admin nhập |
| ❌ **Từ chối báo cáo** | Báo cáo không hợp lệ | ❌ Không đổi | 📧 Email + in-app cho cả 2 bên, báo kết quả "Rejected" kèm lý do admin nhập |

> ✅ **Đã xác nhận có sẵn trong code** (`ListingReportServiceImpl.resolveReport`): tất cả các nhánh trên đều tự động gửi thông báo (email + realtime in-app) cho **người báo cáo và chủ nhà**, kể cả khi báo cáo bị **Từ chối**. Lý do hiển thị trong thông báo luôn lấy từ ghi chú admin nhập (`adminNotes`).

> ⚠️ **Quan trọng:** Nếu muốn chủ nhà sửa tin đăng, bạn PHẢI:
> - Chọn **Chấp nhận** (không phải Từ chối)
> - Bật **Yêu cầu chủ nhà hành động** (KHÔNG bật cùng lúc với "Gỡ bài đăng" — 2 cờ này loại trừ nhau, API sẽ báo lỗi nếu cả hai đều `true`)
>
> Nếu bạn **Từ chối báo cáo**, tin đăng KHÔNG bị ảnh hưởng và chủ nhà KHÔNG thể gửi lại.

> ✅ **Đã bổ sung "Gỡ bài đăng"** (trước đây là gap, nay đã implement): gửi `removeListing: true` trong request `resolve` khi vi phạm đủ nghiêm trọng để gỡ hẳn. Khác với "Yêu cầu chủ nhà sửa", tin đăng bị gỡ sẽ **không thể resubmit** — chủ nhà cố sửa & gửi lại sẽ nhận lỗi từ API. Chi tiết payload xem `ADMIN_LISTING_MODERATION_FRONTEND_GUIDE.md`. Vẫn CHƯA có "cảnh cáo user" (action ở cấp tài khoản) — chỉ mới có hành động ở cấp tin đăng.

### Duyệt Tin Gửi Lại

Khi chủ nhà sửa và gửi lại, tin đăng xuất hiện lại trong **hàng đợi Chờ duyệt**. Quy trình xử lý giống hệt tin mới:
- Duyệt → Hiển thị
- Từ chối → Chủ nhà sửa lại (vòng lặp)
