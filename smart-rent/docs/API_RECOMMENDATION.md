# Báo cáo Logic Thuật Toán Hệ Thống Gợi Ý (Recommendation)

Hệ thống gợi ý của SmartRent sử dụng mô hình **Hybrid (Lai)**, kết hợp giữa **Lọc cộng tác (Collaborative Filtering - CF)** và **Lọc dựa trên nội dung (Content-Based Filtering - CBF)**. 

Tài liệu này giải thích chi tiết cách hệ thống tính toán điểm số cho 2 luồng tính năng chính:
1. **Gợi ý bài đăng tương tự (Similarity)**: Tính mức độ giống nhau giữa các bài đăng.
2. **Bảng tin cá nhân hóa (Recommendation)**: Đề xuất bài đăng phù hợp với sở thích của từng người dùng.

---

## 1. Cách tính điểm Tương đồng (Similarity Score)
*(Áp dụng cho API: `/v1/recommendations/similar/{listingId}`)*

Mục tiêu của luồng này là tìm ra các bài đăng có đặc điểm vật lý và vị trí giống với một bài đăng gốc nhất. Thuật toán cốt lõi được sử dụng là **Content-Based Filtering**.

### Bước 1: Xây dựng Vector Đặc Trưng (Feature Vector)
Hệ thống số hóa mỗi bài đăng thành một vector n-chiều. Mỗi chiều đại diện cho một đặc tính của bài đăng và được gán trọng số khác nhau dựa trên mức độ quan trọng:

| Đặc trưng | Cách xử lý (Mã hóa) | Trọng số |
|---|---|---|
| Loại hình (Phòng trọ, Căn hộ...) | One-hot encoding | **× 1.5** |
| Giá thuê | Chuẩn hóa Min-Max về `[0,1]` | **× 1.2** |
| Diện tích | Chuẩn hóa Min-Max về `[0,1]` | **× 1.0** |
| Số phòng ngủ | Chuẩn hóa Min-Max về `[0,1]` | **× 1.0** |
| Loại giao dịch (Thuê/Bán) | One-hot encoding | **× 1.0** |
| Tỉnh/Thành phố | One-hot encoding | **× 1.0** |

> *Giải thích: "Loại hình" và "Giá thuê" là hai yếu tố quyết định lớn nhất khi tìm nhà, do đó được AI nhân thêm trọng số cao hơn (1.5 và 1.2) trong không gian vector.*

### Bước 2: Lọc danh sách ứng viên (Candidate Retrieval)
Để tối ưu hiệu năng tính toán, Backend không so sánh vector với toàn bộ database mà chỉ lọc ra **tối đa 200 bài ứng viên** theo thứ tự ưu tiên khu vực:
1. Lấy các bài **cùng Phường/Xã**.
2. Nếu chưa đủ 200, lấy tiếp các bài **cùng Quận/Huyện**.
3. Nếu vẫn chưa đủ, lấy tiếp các bài **cùng Tỉnh/Thành phố**.
4. Lấy toàn quốc (nếu bài gốc không có địa chỉ).

### Bước 3: Tính Cosine Similarity & Phạt địa lý (Geo-Penalty)
AI tính góc lệch giữa vector của bài gốc (`A`) và bài ứng viên (`B`) bằng **Cosine Similarity**:
```
CBF_Score_Base = cos(θ) = (A · B) / (||A|| × ||B||)
```
Sau đó, để đảm bảo bài gợi ý càng ở gần bài gốc càng tốt, AI áp dụng **Hệ số phạt địa lý (Geo-Penalty)**:
- Cùng Phường: Không phạt (`CBF_Score = CBF_Score_Base × 1.0`)
- Cùng Quận, khác Phường: Bị phạt (`CBF_Score = CBF_Score_Base × 0.9`)
- Cùng Tỉnh, khác Quận: Bị phạt (`CBF_Score = CBF_Score_Base × 0.7`)
- Khác Tỉnh: Bị phạt nặng (`CBF_Score = CBF_Score_Base × 0.1`)

### Bước 4: Công thức tính Similarity Score cuối cùng
Sau khi có điểm `CBF_Score`, hệ thống tính điểm Similarity cuối cùng bằng cách cộng thêm **Điểm ưu tiên quảng cáo (VIP Boost)** và **Điểm mới (Freshness Boost)**:

```
Similarity_Score = CBF_Score × VIP_Boost × (1 + 0.1 × Freshness_Boost)
```
Trong đó:
- **VIP_Boost**: DIAMOND (×1.15), GOLD (×1.10), SILVER (×1.05), NORMAL (×1.0).
- **Freshness_Boost**: Bài càng mới điểm càng cao. Công thức: `max(0, 1 − số_ngày_đã_đăng × 0.01)`.

Hệ thống sắp xếp giảm dần theo `Similarity_Score` và trả về kết quả.

---

## 2. Cách tính điểm Bảng tin cá nhân (Recommendation Score)
*(Áp dụng cho API: `/v1/recommendations/personalized`)*

Mục tiêu là gợi ý danh sách bài đăng đúng "gu" của người dùng. Thuật toán sử dụng là **Hybrid** (kết hợp CBF ở trên và Collaborative Filtering - CF).

### Bước 1: Thu thập tín hiệu hành vi
Backend ghi nhận các hành vi của người dùng và chấm điểm trọng số sự quan tâm:
- **Lưu bài** (Saved): Trọng số **3.0** (quan tâm cao nhất).
- **Bấm xem SĐT** (Phone Click): Trọng số **2.5** (chuẩn bị giao dịch).
- **Xem bài** (Viewed): Trọng số **1.0** (quan tâm nhẹ).

*(Nếu người dùng chưa có hành vi nào, hệ thống rơi vào trạng thái **Cold Start** và trả về danh sách các bài viết mới nhất trên toàn quốc).*

### Bước 2: Thuật toán Lọc ứng viên đa tầng (Diversity Candidate Generation)
Hệ thống tính toán "Vị trí yêu thích nhất" của người dùng. Tuy nhiên, thay vì chỉ lấy bài ở đúng một khu vực (gây ra hiện tượng "Bong bóng lọc" - Filter Bubble), Backend được thiết kế để **cố tình lấy một tập ứng viên (300 bài) đa dạng** bằng cách cộng dồn từ các vòng tròn địa lý:
- **Tier 1 (Độ sát sao cao):** Lấy tối đa 50 bài cùng Phường/Xã yêu thích.
- **Tier 2 (Mở rộng khám phá):** Lấy thêm tối đa 100 bài cùng Quận/Huyện yêu thích.
- **Tier 4 (Phạm vi bao quát):** Lấy thêm các bài cùng Tỉnh cho đến khi tổng số đạt đủ 300 bài.
- *(Fallback: Nếu gom cả Tỉnh mà vẫn chưa đủ 150 bài, lấy thêm các bài mới nhất trên toàn quốc).*

> *Ý nghĩa thiết kế: Nhờ việc mix (trộn) ứng viên như vậy, AI có cơ hội đánh giá cả những bài ở Phường/Quận lân cận. Nếu một căn nhà ở phường bên cạnh có giá và tiện ích cực kỳ sát với sở thích người dùng, nó vẫn lọt được vào danh sách để AI chấm điểm thay vì bị loại bỏ ngay từ đầu.*

**Thuật toán chống bong bóng lọc (Discovery Shift):**
Nếu hệ thống nhận thấy người dùng đang bắt đầu xem bài ở một **khu vực hoàn toàn mới** (khác với vị trí yêu thích), nó sẽ kích hoạt "Discovery Zone" (Tier 3).
- Trích xuất 50 bài ở khu vực mới.
- **Ghim cố định 3 bài này vào vị trí top 8, 9, 10** trong kết quả trả về. Điều này giúp người dùng khám phá khu vực mới mà không phá hỏng 7 gợi ý cốt lõi đầu tiên.

### Bước 3: Tính điểm Lọc cộng tác (Collaborative Filtering - CF Score)
Thuật toán tính điểm CF được xây dựng dựa trên mô hình **Item-Item Co-occurrence** (Đồng xuất hiện giữa các Item). Hệ thống phân tích lịch sử tương tác của toàn bộ người dùng (500 lượt click & save gần nhất) để tính toán tần suất các bài đăng được xem/lưu cùng lúc với nhau. Dựa vào đó, nếu người dùng hiện tại quan tâm đến một bài đăng A, hệ thống sẽ đề xuất các bài đăng thường được tương tác chung với A bởi những người dùng khác.

### Bước 4: Công thức Recommendation Score cuối cùng (Re-rank)
Sau khi AI tính xong, Backend thực hiện việc xếp hạng lại (Re-rank) lần cuối cùng để cân bằng giữa sự phù hợp của AI và chính sách kinh doanh (bài quảng cáo VIP):

```
Recommendation_Score = 0.6 × AI_Score + 0.3 × VIP_Ad_Score + 0.1 × Freshness_Score
```

Trong đó:
1. **AI_Score (60% ảnh hưởng):**
   - Kết hợp giữa Nội dung (CBF) và Hành vi đám đông (CF).
   - Công thức AI_Score = `0.6 × CBF_Score + 0.4 × CF_Score`.
2. **VIP_Ad_Score (30% ảnh hưởng):**
   - Đảm bảo quyền lợi cho người mua gói quảng cáo.
   - Điểm: DIAMOND (1.0), GOLD (0.7), SILVER (0.4), NORMAL (0.2).
3. **Freshness_Score (10% ảnh hưởng):**
   - Đảm bảo Feed luôn tươi mới, không hiện bài cũ.
   - Điểm: `1 / (1 + số_ngày_đã_đăng)`.

Bài đăng nào có `Recommendation_Score` cao nhất sẽ nằm ở vị trí số 1 trên Bảng tin.



