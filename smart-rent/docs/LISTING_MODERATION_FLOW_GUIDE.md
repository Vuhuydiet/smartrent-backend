# Hướng Dẫn: Quy Trình Duyệt & Cập Nhật Tin Đăng

---

## Dành Cho Chủ Nhà (Owner)

### Các Trạng Thái Tin Đăng

| Trạng thái | Ý nghĩa | Bạn cần làm gì? |
|------------|---------|-----------------|
| 🟡 **Đang chờ duyệt** | Admin đang xem xét tin đăng | Chờ kết quả |
| 🟢 **Đang hiển thị** | Tin đăng đã được duyệt, đang hiển thị | Không cần làm gì |
| 🔴 **Bị từ chối** | Admin yêu cầu chỉnh sửa | **Sửa → Gửi lại** |
| ⛔ **Tạm ngưng** | Tin đăng bị tạm ngưng | Liên hệ hỗ trợ |

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

| Quyết định | Khi nào dùng | Ảnh hưởng đến tin đăng? |
|-----------|-------------|------------------------|
| ✅ **Chấp nhận + Yêu cầu chủ nhà sửa** | Báo cáo đúng, cần chủ nhà fix | ✅ Tin đăng → Từ chối, chủ nhà phải sửa |
| ✅ **Chấp nhận + Không yêu cầu sửa** | Báo cáo đúng, nhưng admin tự xử lý | ❌ Không đổi |
| ❌ **Từ chối báo cáo** | Báo cáo không hợp lệ | ❌ Không đổi |

> ⚠️ **Quan trọng:** Nếu muốn chủ nhà sửa tin đăng, bạn PHẢI:
> - Chọn **Chấp nhận** (không phải Từ chối)
> - Bật **Yêu cầu chủ nhà hành động**
>
> Nếu bạn **Từ chối báo cáo**, tin đăng KHÔNG bị ảnh hưởng và chủ nhà KHÔNG thể gửi lại.

### Duyệt Tin Gửi Lại

Khi chủ nhà sửa và gửi lại, tin đăng xuất hiện lại trong **hàng đợi Chờ duyệt**. Quy trình xử lý giống hệt tin mới:
- Duyệt → Hiển thị
- Từ chối → Chủ nhà sửa lại (vòng lặp)
