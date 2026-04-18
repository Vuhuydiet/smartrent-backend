-- ============================================================
-- Populate ~1000 news rows for SmartRent
-- Categories: NEWS, BLOG, POLICY, MARKET, PROJECT, INVESTMENT, GUIDE
-- Run once; safe to re-run (DROP/CREATE procedure, truncate guarded by slug UNIQUE)
-- ============================================================

DROP PROCEDURE IF EXISTS populate_news;

DELIMITER $$

CREATE PROCEDURE populate_news()
BEGIN
    DECLARE i       INT DEFAULT 1;
    DECLARE cat     VARCHAR(20);
    DECLARE stat    VARCHAR(10);
    DECLARE pub_at  DATETIME;
    DECLARE v_title VARCHAR(255);
    DECLARE v_slug  VARCHAR(300);
    DECLARE v_summ  TEXT;
    DECLARE v_cont  LONGTEXT;
    DECLARE v_tags  VARCHAR(500);
    DECLARE v_views BIGINT;
    DECLARE v_auth  VARCHAR(100);
    DECLARE v_thumb VARCHAR(500);
    DECLARE v_meta_t  VARCHAR(255);
    DECLARE v_meta_d  VARCHAR(500);
    DECLARE v_meta_k  VARCHAR(500);

    WHILE i <= 1000 DO

        -- ── category (7 buckets, weighted toward NEWS & MARKET) ──────────
        SET cat = ELT(1 + (i % 7),
            'NEWS',       -- 0 → NEWS
            'MARKET',     -- 1 → MARKET
            'POLICY',     -- 2 → POLICY
            'BLOG',       -- 3 → BLOG
            'INVESTMENT', -- 4 → INVESTMENT
            'PROJECT',    -- 5 → PROJECT
            'GUIDE'       -- 6 → GUIDE
        );

        -- ── status (80 % PUBLISHED, 12 % ARCHIVED, 8 % DRAFT) ───────────
        SET stat = CASE
            WHEN (i % 25) = 0 THEN 'DRAFT'
            WHEN (i % 13) = 0 THEN 'ARCHIVED'
            ELSE 'PUBLISHED'
        END;

        -- ── published_at (spread across 2024-01-01 → 2026-03-30) ─────────
        SET pub_at = CASE
            WHEN stat = 'DRAFT'    THEN NULL
            WHEN stat = 'ARCHIVED' THEN DATE_ADD('2024-01-01', INTERVAL (i * 3)  % 365 DAY)
            ELSE                        DATE_ADD('2024-06-01', INTERVAL (i * 7)  % 670 DAY)
        END;

        -- ── view_count ────────────────────────────────────────────────────
        SET v_views = CASE
            WHEN stat = 'PUBLISHED' THEN FLOOR(ABS(SIN(i)) * 48000) + 100
            WHEN stat = 'ARCHIVED'  THEN FLOOR(ABS(SIN(i)) * 15000) + 50
            ELSE 0
        END;

        -- ── author ────────────────────────────────────────────────────────
        SET v_auth = ELT(1 + (i % 10),
            'Nguyễn Minh Tuấn', 'Trần Thị Lan', 'Lê Văn Hùng',
            'Phạm Thị Mai',     'Hoàng Anh Dũng', 'Võ Thị Thu',
            'Đặng Văn Khoa',    'Bùi Thị Hoa',    'Ngô Quốc Bảo',
            'Lý Thị Ngọc'
        );

        -- ── thumbnail — curated Unsplash IDs per category ────────────────
        -- Format: https://images.unsplash.com/photo-{ID}?w=800&h=450&fit=crop&auto=format
        SET v_thumb = CONCAT(
            'https://images.unsplash.com/photo-',
            CASE cat
                WHEN 'NEWS' THEN ELT(1 + (i % 4),
                    '1560518883-ce09059eeffa',  -- city skyline / real estate
                    '1486325212027-8081e485255e', -- modern apartment building
                    '1545324418-cc1a3fa12c98',   -- housing complex aerial
                    '1512917774080-9991f1c4c750'  -- property exterior
                )
                WHEN 'MARKET' THEN ELT(1 + (i % 4),
                    '1611348586804-61bf6c080437', -- financial chart / market data
                    '1560179406-1dd927abb465',    -- business analysis
                    '1466611653911-0628d5f2b9a3', -- market overview
                    '1551836022-4196f3d39c3e'     -- data analytics desk
                )
                WHEN 'POLICY' THEN ELT(1 + (i % 3),
                    '1589939705384-5185137a7f0f', -- government building
                    '1589939705384-5185137a7f0f', -- law / policy document
                    '1454165804606-c3d57bc86b40'  -- signing contract / legal
                )
                WHEN 'BLOG' THEN ELT(1 + (i % 4),
                    '1583608205776-bfd35f0d9f83', -- cozy living room interior
                    '1522708323590-d24dbb6b0267', -- modern bedroom
                    '1600585154340-be6161a56a0c', -- bright apartment interior
                    '1507209596998-b0b83e843df7'  -- coffee table / lifestyle
                )
                WHEN 'INVESTMENT' THEN ELT(1 + (i % 4),
                    '1611348586804-61bf6c080437', -- investment / finance
                    '1560179406-1dd927abb465',    -- calculator and money
                    '1486406146926-c627a92ad1ab', -- skyscraper investment
                    '1466611653911-0628d5f2b9a3'  -- growth chart
                )
                WHEN 'PROJECT' THEN ELT(1 + (i % 4),
                    '1486406146926-c627a92ad1ab', -- construction / new building
                    '1512917774080-9991f1c4c750', -- completed property
                    '1570129477492-45c003edd2be', -- modern tower
                    '1545324418-cc1a3fa12c98'     -- aerial project view
                )
                ELSE -- GUIDE
                    ELT(1 + (i % 3),
                    '1454165804606-c3d57bc86b40', -- reading contract / guide
                    '1507209596998-b0b83e843df7', -- notepad / checklist
                    '1583608205776-bfd35f0d9f83'  -- home consultation
                )
            END,
            '?w=800&h=450&fit=crop&auto=format'
        );

        -- ── title / slug / summary / content / tags / meta ───────────────
        CASE cat

            WHEN 'NEWS' THEN
                SET v_title = ELT(1 + (i % 20),
                    CONCAT('Thị trường bất động sản tháng ', (i % 12) + 1, ' năm 2025 có nhiều biến động'),
                    CONCAT('Giá nhà tại TP.HCM tăng ', (i % 15) + 5, '% so với cùng kỳ năm ngoái'),
                    CONCAT('Hà Nội: Nguồn cung căn hộ quý ', (i % 4) + 1, ' dồi dào hơn dự kiến'),
                    'Ngân hàng hạ lãi suất cho vay mua nhà xuống mức thấp kỷ lục',
                    'Chính phủ phê duyệt gói hỗ trợ nhà ở 120.000 tỷ đồng',
                    CONCAT('Đà Nẵng thu hút ', (i % 10) + 5, ' dự án BĐS nghỉ dưỡng mới'),
                    'Thị trường cho thuê căn hộ dịch vụ hồi phục mạnh sau dịch',
                    'Bộ Xây dựng công bố chỉ số giá nhà ở mới nhất',
                    CONCAT('Cần Thơ: Quỹ đất phát triển đô thị còn ', (i % 50) + 200, ' ha'),
                    CONCAT('Tỷ lệ hấp thụ căn hộ tại TP.HCM đạt ', (i % 30) + 60, '% trong quý vừa qua'),
                    'Xu hướng mua nhà xanh ngày càng được người trẻ ưa chuộng',
                    'Phân khúc nhà phố thương mại tiếp tục tăng trưởng ổn định',
                    CONCAT('Vĩnh Phúc: Giá đất khu công nghiệp tăng ', (i % 20) + 10, '% năm nay'),
                    'Thị trường condotel khởi sắc trở lại sau thời gian trầm lắng',
                    'Nhà đầu tư nước ngoài quan tâm nhiều hơn đến BĐS Việt Nam',
                    'Chung cư mini bị siết chặt quản lý trên toàn quốc',
                    CONCAT('Bình Dương: Giá nhà liền kề tăng bình quân ', (i % 12) + 8, '% mỗi năm'),
                    'Phân khúc nhà ở xã hội vẫn còn thiếu hụt nghiêm trọng',
                    'Các sàn giao dịch BĐS trực tuyến tăng mạnh lượng người dùng',
                    CONCAT('Long An: Thêm ', (i % 5) + 3, ' khu đô thị mới được phê duyệt')
                );
                SET v_slug = CONCAT('tin-bds-', i, '-', LOWER(REPLACE(SUBSTRING(v_title, 1, 40), ' ', '-')));
                SET v_summ = CONCAT('Cập nhật mới nhất về thị trường bất động sản Việt Nam. ', v_title, '. Phân tích chuyên sâu từ các chuyên gia hàng đầu trong ngành.');
                SET v_tags = 'bất động sản,thị trường,nhà đất,tin tức,2025';
                SET v_meta_t = CONCAT(v_title, ' | SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'bất động sản, nhà đất, thị trường BĐS, tin tức nhà đất';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Theo báo cáo mới nhất từ Hiệp hội Bất động sản Việt Nam (VNREA), thị trường bất động sản trong thời gian qua có nhiều diễn biến đáng chú ý, tạo ra cả cơ hội lẫn thách thức cho các nhà đầu tư và người mua nhà.</p>',
                    '<h3>Diễn biến thị trường</h3>',
                    '<p>Số liệu thống kê cho thấy nguồn cung mới tiếp tục được bổ sung tại nhiều thành phố lớn. Riêng tại TP.HCM và Hà Nội, lượng căn hộ được chào bán trong quý vừa qua tăng đáng kể so với cùng kỳ năm trước. Các dự án tập trung chủ yếu ở phân khúc trung cấp và cao cấp, trong khi nhà ở giá rẻ vẫn còn thiếu hụt.</p>',
                    '<h3>Phân tích chuyên gia</h3>',
                    '<p>Các chuyên gia nhận định rằng thị trường đang trong giai đoạn điều chỉnh lành mạnh. Lãi suất cho vay mua nhà duy trì ở mức thấp đã hỗ trợ tích cực cho cầu nhà ở thực. Bên cạnh đó, việc siết chặt tín dụng đối với đầu cơ giúp thị trường phát triển bền vững hơn.</p>',
                    '<h3>Dự báo</h3>',
                    '<p>Trong thời gian tới, thị trường dự kiến tiếp tục ổn định với thanh khoản cải thiện dần. Các nhà đầu tư được khuyến cáo nên tập trung vào các sản phẩm có pháp lý rõ ràng và vị trí thuận lợi về hạ tầng giao thông.</p>',
                    '<p><em>Nguồn: Ban biên tập SmartRent tổng hợp</em></p>'
                );

            WHEN 'MARKET' THEN
                SET v_title = ELT(1 + (i % 18),
                    CONCAT('Báo cáo thị trường BĐS quý ', (i % 4) + 1, '/2025: Phân tích toàn diện'),
                    CONCAT('Chỉ số giá nhà đất tại ', ELT(1 + (i % 5), 'TP.HCM', 'Hà Nội', 'Đà Nẵng', 'Bình Dương', 'Đồng Nai'), ' tháng ', (i % 12) + 1),
                    'Phân tích cung cầu căn hộ chung cư năm 2025',
                    CONCAT('Tăng trưởng giá đất nền khu vực phía ', ELT(1 + (i % 4), 'Đông', 'Tây', 'Nam', 'Bắc'), ' TP.HCM'),
                    'So sánh giá thuê văn phòng tại các trung tâm thương mại lớn',
                    CONCAT('Phân khúc nhà ở dưới ', (i % 5 + 2), ' tỷ đồng: Cơ hội và thách thức'),
                    'Thị trường bất động sản nghỉ dưỡng: Nhìn lại và triển vọng',
                    'Dòng tiền đổ vào BĐS công nghiệp tăng mạnh',
                    CONCAT('Lãi suất và tác động đến giá nhà: Kịch bản ', (i % 3) + 1),
                    'Tổng quan thị trường cho thuê căn hộ tại các khu đô thị mới',
                    'Phân tích vùng giá đất theo quy hoạch giao thông mới',
                    'Tỷ suất sinh lời BĐS cho thuê so với các kênh đầu tư khác',
                    CONCAT('Dân số tăng và áp lực lên thị trường nhà ở đô thị năm ', 2024 + (i % 3)),
                    'Xu hướng giá BĐS ven đô tại các tỉnh vệ tinh',
                    'Chỉ số thanh khoản thị trường nhà đất qua các quý',
                    CONCAT('Thị trường BĐS ', ELT(1 + (i % 6), 'Hải Phòng', 'Cần Thơ', 'Nha Trang', 'Vũng Tàu', 'Huế', 'Quy Nhơn'), ': Tiềm năng tăng trưởng'),
                    'Tác động của FDI đến giá đất khu công nghiệp',
                    'Chênh lệch giá sơ cấp và thứ cấp trên thị trường căn hộ'
                );
                SET v_slug = CONCAT('phan-tich-thi-truong-', i, '-', (i % 12) + 1, '-2025');
                SET v_summ = CONCAT('Phân tích chuyên sâu về diễn biến thị trường bất động sản. ', v_title, '. Dữ liệu cập nhật từ các nguồn uy tín.');
                SET v_tags = 'phân tích thị trường,giá nhà đất,BĐS,báo cáo,đầu tư';
                SET v_meta_t = CONCAT(v_title, ' - Phân tích SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'phân tích thị trường BĐS, giá nhà, báo cáo bất động sản';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Trên cơ sở dữ liệu thu thập từ hơn 50.000 giao dịch bất động sản thực tế, SmartRent công bố báo cáo phân tích thị trường chi tiết, cung cấp góc nhìn toàn diện cho các nhà đầu tư và người mua nhà.</p>',
                    '<h3>Diễn biến giá</h3>',
                    '<p>Giá bất động sản tại các thành phố lớn tiếp tục duy trì xu hướng tăng nhẹ nhưng ổn định. Mức tăng trung bình khoảng 5-8% mỗi năm, phản ánh nhu cầu thực tế từ dân số đô thị hóa ngày càng tăng. Phân khúc trung cấp ghi nhận giao dịch nhiều nhất với tỷ lệ hấp thụ cao.</p>',
                    '<h3>Nguồn cung và nhu cầu</h3>',
                    '<p>Nguồn cung mới trong năm nay dự kiến đạt khoảng 45.000 căn hộ trên toàn quốc. Trong đó, TP.HCM chiếm khoảng 40%, Hà Nội 35%, còn lại phân bổ ở các tỉnh thành khác. Nhu cầu nhà ở thực tiếp tục là động lực chính của thị trường.</p>',
                    '<h3>Triển vọng</h3>',
                    '<p>Các chuyên gia kinh tế dự báo thị trường sẽ tiếp tục ổn định trong năm tới với sự hỗ trợ từ chính sách tín dụng linh hoạt và đầu tư hạ tầng giao thông mạnh mẽ. Phân khúc nhà ở vừa túi tiền được kỳ vọng sẽ được bổ sung nguồn cung đáng kể.</p>',
                    '<p><em>Dữ liệu: SmartRent Research | Cập nhật: Tháng ', (i % 12) + 1, '/2025</em></p>'
                );

            WHEN 'POLICY' THEN
                SET v_title = ELT(1 + (i % 16),
                    CONCAT('Luật Đất đai sửa đổi năm 2025: ', ELT(1 + (i % 4), 'Những điểm mới quan trọng', 'Tác động đến thị trường', 'Quyền lợi người mua nhà', 'Hướng dẫn thực thi')),
                    'Quy định mới về cấp sổ đỏ cho nhà ở riêng lẻ',
                    'Chính sách hỗ trợ vay mua nhà ở xã hội được mở rộng',
                    CONCAT('Nghị định ', (i % 50) + 50, '/NĐ-CP về quản lý thị trường BĐS'),
                    'Thông tư hướng dẫn về thuế chuyển nhượng bất động sản',
                    'Quy hoạch sử dụng đất quốc gia đến năm 2030 được phê duyệt',
                    'Điều kiện kinh doanh bất động sản: Những thay đổi cần biết',
                    'Quy định về nhà ở cho người nước ngoài tại Việt Nam',
                    'Chính sách thuế BĐS mới: Siết đầu cơ, hỗ trợ mua nhà thực',
                    'Luật Nhà ở sửa đổi: Bảo vệ quyền lợi người mua nhà hình thành trong tương lai',
                    CONCAT('Quy định giãn nợ và cơ cấu lại nợ cho doanh nghiệp BĐS đến ', 2025 + (i % 2)),
                    'Điều chỉnh bảng giá đất tại các tỉnh thành lớn',
                    'Kiểm soát chặt hoạt động phân lô bán nền tự phát',
                    'Chính sách ưu đãi đầu tư vào dự án nhà ở xã hội',
                    'Quy định mới về đặt cọc mua bán bất động sản',
                    'Xử lý vi phạm xây dựng không phép: Khung pháp lý mới'
                );
                SET v_slug = CONCAT('chinh-sach-phap-luat-bds-', i);
                SET v_summ = CONCAT('Cập nhật chính sách, pháp luật về bất động sản. ', v_title, '. Giúp người mua nhà và nhà đầu tư nắm rõ quy định mới nhất.');
                SET v_tags = 'chính sách,pháp luật,luật đất đai,quy định,BĐS';
                SET v_meta_t = CONCAT(v_title, ' | Chính sách BĐS SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'luật đất đai, chính sách nhà ở, pháp luật BĐS';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Trong bối cảnh thị trường bất động sản đang trong giai đoạn phát triển quan trọng, các cơ quan nhà nước đã ban hành nhiều văn bản pháp luật mới nhằm tăng cường quản lý, minh bạch hóa thị trường và bảo vệ quyền lợi người dân.</p>',
                    '<h3>Nội dung chính sách</h3>',
                    '<p>Văn bản quy phạm pháp luật mới nhất quy định rõ ràng về quyền và nghĩa vụ của các bên tham gia giao dịch bất động sản. Đặc biệt, các quy định về tính minh bạch thông tin dự án, điều kiện bán nhà hình thành trong tương lai được thắt chặt hơn.</p>',
                    '<h3>Tác động đến thị trường</h3>',
                    '<p>Các chính sách mới được kỳ vọng sẽ lành mạnh hóa thị trường, giảm thiểu tình trạng đầu cơ và bảo vệ người mua nhà lần đầu. Doanh nghiệp bất động sản cần chuẩn bị kỹ lưỡng để tuân thủ các quy định mới trong thời gian sớm nhất.</p>',
                    '<h3>Khuyến nghị cho người mua nhà</h3>',
                    '<p>Người mua nhà cần tìm hiểu kỹ các quy định mới trước khi ký kết hợp đồng. Nên tham khảo ý kiến luật sư hoặc chuyên gia tư vấn BĐS để đảm bảo quyền lợi tối đa trong quá trình giao dịch.</p>',
                    '<p><em>Biên soạn: Bộ phận pháp lý SmartRent</em></p>'
                );

            WHEN 'BLOG' THEN
                SET v_title = ELT(1 + (i % 18),
                    CONCAT(i % 10 + 5, ' kinh nghiệm mua nhà lần đầu không nên bỏ qua'),
                    'Mua nhà hay thuê nhà: Đâu là lựa chọn thông minh?',
                    CONCAT('Cách chọn căn hộ phù hợp với ngân sách ', (i % 5 + 2), ' tỷ đồng'),
                    'Những sai lầm phổ biến khi đầu tư BĐS lần đầu',
                    'Bí quyết thương lượng giảm giá khi mua nhà',
                    'Kinh nghiệm thuê căn hộ dịch vụ cho người đi làm xa nhà',
                    'Cách đọc hợp đồng mua bán bất động sản không bị thiệt',
                    CONCAT(i % 7 + 3, ' câu hỏi cần hỏi trước khi mua căn hộ chung cư'),
                    'Làm thế nào để biết giá nhà có hợp lý không?',
                    'Phong thủy nhà ở: Nên xem xét đến đâu?',
                    'Kinh nghiệm chọn hướng nhà theo khí hậu Việt Nam',
                    'Quy trình mua nhà từ A đến Z cho người mới',
                    'Cách tính toán chi phí thực khi mua căn hộ chung cư',
                    'Sổ đỏ, sổ hồng: Phân biệt và những điều cần biết',
                    'Vay ngân hàng mua nhà: Lưu ý quan trọng',
                    'Kiểm tra pháp lý dự án BĐS trước khi xuống tiền',
                    'Đầu tư căn hộ cho thuê: Tính toán lợi nhuận thực tế',
                    'Nên mua nhà ở hay đất nền để đầu tư dài hạn?'
                );
                SET v_slug = CONCAT('blog-kinh-nghiem-', i);
                SET v_summ = CONCAT('Bài viết chia sẻ kinh nghiệm thực tế về mua bán, thuê nhà và đầu tư bất động sản. ', v_title, '.');
                SET v_tags = 'kinh nghiệm,mua nhà,tư vấn,blog,nhà đất';
                SET v_meta_t = CONCAT(v_title, ' - Blog SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'kinh nghiệm mua nhà, tư vấn BĐS, blog bất động sản';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Bất động sản là một trong những quyết định tài chính quan trọng nhất trong cuộc đời mỗi người. Bài viết này tổng hợp những kinh nghiệm thực tế từ hàng nghìn giao dịch thành công, giúp bạn đưa ra quyết định đúng đắn.</p>',
                    '<h3>Chuẩn bị trước khi mua</h3>',
                    '<p>Trước khi bắt đầu tìm kiếm, hãy xác định rõ nhu cầu thực sự của mình. Bạn cần nhà để ở hay để đầu tư? Ngân sách tối đa là bao nhiêu? Khu vực ưu tiên là đâu? Những câu hỏi này sẽ giúp thu hẹp phạm vi tìm kiếm và tiết kiệm thời gian đáng kể.</p>',
                    '<h3>Những lưu ý quan trọng</h3>',
                    '<p>Luôn kiểm tra pháp lý kỹ trước khi đặt cọc. Yêu cầu xem sổ đỏ/sổ hồng bản gốc, kiểm tra quy hoạch đất, xác nhận không có tranh chấp. Đừng để sức ép thời gian khiến bạn bỏ qua bước quan trọng này.</p>',
                    '<h3>Thương lượng và ký hợp đồng</h3>',
                    '<p>Đừng ngại thương lượng giá. Trong hầu hết giao dịch, người bán thường để khoảng 5-10% để đàm phán. Hãy tìm hiểu giá thị trường trước để có cơ sở thương lượng hợp lý. Khi ký hợp đồng, đọc kỹ từng điều khoản, đặc biệt là điều kiện phạt vi phạm và thời hạn thanh toán.</p>',
                    '<p><em>Bài viết: ', v_auth, ' | SmartRent Blog</em></p>'
                );

            WHEN 'INVESTMENT' THEN
                SET v_title = ELT(1 + (i % 16),
                    CONCAT('Đầu tư BĐS năm 2025: Kênh nào sinh lời cao nhất?'),
                    CONCAT('Phân tích tỷ suất lợi nhuận căn hộ cho thuê tại ', ELT(1 + (i % 4), 'TP.HCM', 'Hà Nội', 'Đà Nẵng', 'Bình Dương')),
                    'Đất nền hay căn hộ: Đâu là kênh đầu tư an toàn hơn?',
                    CONCAT('Chiến lược đầu tư BĐS với số vốn ', (i % 5 + 1), ' tỷ đồng'),
                    'Nhà phố thương mại: Cơ hội đầu tư dài hạn bền vững',
                    CONCAT('Lợi nhuận thực tế từ căn hộ mini cho thuê sau ', (i % 5) + 3, ' năm'),
                    'Đầu tư BĐS nghỉ dưỡng: Rủi ro và cơ hội',
                    'Cách phân tích dự án BĐS trước khi xuống tiền',
                    'Đòn bẩy tài chính trong đầu tư BĐS: Sử dụng đúng cách',
                    CONCAT('Top ', (i % 5) + 3, ' khu vực BĐS tiềm năng nhất năm 2025'),
                    'BĐS công nghiệp: Làn sóng đầu tư mới từ FDI',
                    'Đầu tư căn hộ officetel: Xu hướng và triển vọng',
                    'Phân tích dòng tiền khi đầu tư nhà cho thuê',
                    'Tránh bẫy thanh khoản khi đầu tư BĐS dài hạn',
                    'Đa dạng hóa danh mục đầu tư với bất động sản',
                    'Mô hình đầu tư BĐS thụ động qua quỹ REIT'
                );
                SET v_slug = CONCAT('dau-tu-bds-', i);
                SET v_summ = CONCAT('Phân tích cơ hội và chiến lược đầu tư bất động sản hiệu quả. ', v_title, '. Dành cho nhà đầu tư từ mới đến có kinh nghiệm.');
                SET v_tags = 'đầu tư,bất động sản,lợi nhuận,tài chính,chiến lược';
                SET v_meta_t = CONCAT(v_title, ' | Đầu tư BĐS SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'đầu tư bất động sản, lợi nhuận BĐS, chiến lược đầu tư';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Trong bối cảnh lãi suất tiết kiệm ngân hàng duy trì ở mức thấp, bất động sản vẫn là kênh đầu tư được nhiều người Việt Nam ưa chuộng nhất nhờ tính ổn định và khả năng sinh lời hấp dẫn trong dài hạn.</p>',
                    '<h3>Phân tích cơ hội</h3>',
                    '<p>Tỷ suất lợi nhuận cho thuê tại các thành phố lớn dao động từ 4-7% mỗi năm, cộng với mức tăng giá tài sản trung bình 8-12% mỗi năm, tổng lợi nhuận có thể đạt 12-19% nếu chọn đúng phân khúc và khu vực.</p>',
                    '<h3>Các yếu tố rủi ro</h3>',
                    '<p>Nhà đầu tư cần cẩn trọng với rủi ro thanh khoản khi thị trường giao dịch chậm. Việc sử dụng đòn bẩy tài chính quá mức có thể khiến dòng tiền âm nếu tỷ lệ lấp đầy thấp. Luôn duy trì quỹ dự phòng ít nhất 6 tháng chi phí vận hành.</p>',
                    '<h3>Khuyến nghị</h3>',
                    '<p>Bắt đầu với phân khúc phù hợp với vốn sẵn có và hiểu biết của mình. Ưu tiên các dự án có pháp lý hoàn chỉnh, chủ đầu tư uy tín và vị trí kết nối giao thông tốt. Kiên nhẫn là chìa khóa thành công trong đầu tư bất động sản dài hạn.</p>',
                    '<p><em>Phân tích: ', v_auth, ' | SmartRent Investment</em></p>'
                );

            WHEN 'PROJECT' THEN
                SET v_title = ELT(1 + (i % 18),
                    CONCAT('Dự án ', ELT(1 + (i % 8), 'The', 'Green', 'Golden', 'Smart', 'Park', 'City', 'Urban', 'Eco'), ' ', ELT(1 + (i % 6), 'Residence', 'Tower', 'Plaza', 'Heights', 'Gardens', 'Complex'), ' - Mở bán đợt ', (i % 5) + 1),
                    CONCAT('Giới thiệu dự án căn hộ cao cấp tại Q.', (i % 12) + 1, ' TP.HCM'),
                    CONCAT('Khu đô thị ', (i % 5000) + 100, ' ha tại ', ELT(1 + (i % 4), 'Bình Dương', 'Long An', 'Đồng Nai', 'Vĩnh Phúc')),
                    CONCAT('Dự án nhà ở xã hội ', (i % 500) + 200, ' căn hộ được phê duyệt'),
                    'Khu phức hợp thương mại - văn phòng - nhà ở mới nhất TP.HCM',
                    CONCAT(ELT(1 + (i % 5), 'Vinhomes', 'Novaland', 'Hưng Thịnh', 'Nam Long', 'Phát Đạt'), ' ra mắt dự án mới tại ', ELT(1 + (i % 4), 'Hà Nội', 'TP.HCM', 'Đà Nẵng', 'Cần Thơ')),
                    'Dự án căn hộ smart home tích hợp công nghệ 4.0',
                    CONCAT('Khu resort BĐS nghỉ dưỡng tại ', ELT(1 + (i % 5), 'Phú Quốc', 'Nha Trang', 'Đà Lạt', 'Hội An', 'Sầm Sơn')),
                    'Dự án nhà liền kề biệt thự ven sông được mong đợi',
                    CONCAT('Chung cư cao tầng chuẩn ', (i % 3) + 3, ' sao tại vị trí vàng'),
                    'Tiến độ xây dựng các dự án lớn cập nhật tháng này',
                    CONCAT('Dự án BĐS xanh đạt chứng nhận LEED tại Việt Nam'),
                    'Khu đô thị thông minh đầu tiên ứng dụng AI quản lý',
                    CONCAT('Mở bán ', (i % 200) + 50, ' căn hộ đợt cuối, cơ hội cuối cùng'),
                    'Dự án tích hợp trường học, bệnh viện và trung tâm thương mại',
                    CONCAT('Phê duyệt quy hoạch khu đô thị mới ', (i % 100) + 100, ' ha'),
                    'Dự án cải tạo chung cư cũ: Tiến độ và kế hoạch bàn giao',
                    'Khu căn hộ ven biển đẳng cấp quốc tế mới ra mắt'
                );
                SET v_slug = CONCAT('du-an-bds-', i);
                SET v_summ = CONCAT('Thông tin chi tiết về dự án bất động sản mới. ', v_title, '. Cập nhật tiến độ, giá bán và thông tin mở bán.');
                SET v_tags = 'dự án,BĐS,căn hộ,mở bán,đầu tư';
                SET v_meta_t = CONCAT(v_title, ' | Dự án BĐS SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'dự án bất động sản, căn hộ mới, mở bán';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Dự án được triển khai bởi chủ đầu tư uy tín với kinh nghiệm nhiều năm trong lĩnh vực phát triển bất động sản. Tọa lạc tại vị trí đắc địa với hệ thống tiện ích đồng bộ, dự án hứa hẹn mang lại không gian sống chất lượng cao cho cư dân.</p>',
                    '<h3>Tổng quan dự án</h3>',
                    '<p>Dự án bao gồm các loại hình sản phẩm đa dạng từ căn hộ 1-3 phòng ngủ đến penthouse và shophouse thương mại. Thiết kế hiện đại, tận dụng tối đa ánh sáng tự nhiên và tầm nhìn thành phố. Hệ thống bảo mật 24/7 và quản lý tòa nhà chuyên nghiệp.</p>',
                    '<h3>Tiện ích nội khu</h3>',
                    '<p>Hồ bơi Olympic, phòng gym đẳng cấp, công viên xanh, khu vui chơi trẻ em, siêu thị tiện lợi, nhà hàng và café. Tất cả tiện ích được thiết kế đồng bộ, tạo nên cuộc sống tiện nghi khép kín cho cư dân.</p>',
                    '<h3>Thông tin mở bán</h3>',
                    '<p>Giá bán từ ', (i % 50 + 20), ' triệu/m², nhiều chính sách thanh toán linh hoạt hỗ trợ khách hàng. Hỗ trợ vay ngân hàng đến 70% giá trị căn hộ với lãi suất ưu đãi. Liên hệ hotline để đặt lịch tham quan căn hộ mẫu.</p>',
                    '<p><em>Cập nhật: ', v_auth, ' | SmartRent Project</em></p>'
                );

            ELSE -- GUIDE
                SET v_title = ELT(1 + (i % 16),
                    'Hướng dẫn đọc hợp đồng mua bán bất động sản',
                    CONCAT('Các bước thủ tục sang tên sổ đỏ năm ', 2024 + (i % 2)),
                    'Hướng dẫn vay ngân hàng mua nhà từ A đến Z',
                    CONCAT('Cách tính thuế thu nhập cá nhân khi bán nhà đất năm ', 2025 + (i % 2)),
                    'Thủ tục đăng ký biến động đất đai: Hướng dẫn chi tiết',
                    'Quy trình công chứng hợp đồng mua bán nhà đất',
                    'Hướng dẫn kiểm tra quy hoạch đất trước khi mua',
                    'Cách tính diện tích thông thủy và diện tích tim tường',
                    'Thủ tục nhận bàn giao căn hộ từ chủ đầu tư',
                    'Hướng dẫn xử lý tranh chấp BĐS không qua tòa án',
                    'Các loại phí khi mua căn hộ chung cư: Tổng hợp đầy đủ',
                    'Hướng dẫn kiểm tra tình trạng pháp lý bất động sản online',
                    'Thủ tục mua nhà cho người Việt Nam định cư ở nước ngoài',
                    'Cách tính lãi suất và trả nợ vay mua nhà hiệu quả',
                    'Hướng dẫn lập hợp đồng thuê nhà bảo vệ quyền lợi đôi bên',
                    'Quy trình hoàn công nhà ở riêng lẻ mới nhất'
                );
                SET v_slug = CONCAT('huong-dan-bds-', i);
                SET v_summ = CONCAT('Hướng dẫn chi tiết các thủ tục và kiến thức cần thiết về bất động sản. ', v_title, '. Dễ hiểu, dễ thực hiện.');
                SET v_tags = 'hướng dẫn,thủ tục,pháp lý,kiến thức,nhà đất';
                SET v_meta_t = CONCAT(v_title, ' | Hướng dẫn SmartRent');
                SET v_meta_d = v_summ;
                SET v_meta_k = 'hướng dẫn mua nhà, thủ tục BĐS, pháp lý nhà đất';
                SET v_cont = CONCAT(
                    '<h2>', v_title, '</h2>',
                    '<p>Bài hướng dẫn này được biên soạn bởi đội ngũ chuyên gia pháp lý và tư vấn BĐS của SmartRent, nhằm giúp người mua nhà và nhà đầu tư thực hiện đúng các thủ tục, tránh rủi ro và tiết kiệm thời gian, chi phí.</p>',
                    '<h3>Bước 1: Chuẩn bị hồ sơ</h3>',
                    '<p>Cần chuẩn bị đầy đủ giấy tờ tùy thân, giấy tờ về bất động sản (sổ đỏ/sổ hồng bản gốc), hợp đồng mua bán và các giấy tờ liên quan. Kiểm tra kỹ tính hợp lệ của từng loại giấy tờ trước khi tiến hành các bước tiếp theo.</p>',
                    '<h3>Bước 2: Thực hiện thủ tục</h3>',
                    '<p>Nộp hồ sơ tại cơ quan có thẩm quyền (UBND cấp xã/phường hoặc Văn phòng đăng ký đất đai). Thời gian giải quyết thông thường từ 10-15 ngày làm việc. Theo dõi tiến độ và bổ sung hồ sơ nếu được yêu cầu.</p>',
                    '<h3>Lưu ý quan trọng</h3>',
                    '<p>Không nên ủy quyền toàn bộ thủ tục cho bên trung gian nếu không đáng tin cậy. Lưu giữ đầy đủ biên lai và giấy tờ trong suốt quá trình giao dịch. Nếu gặp khó khăn, nên tìm đến luật sư hoặc dịch vụ tư vấn pháp lý uy tín.</p>',
                    '<p><em>Biên soạn: Ban tư vấn pháp lý SmartRent | Cập nhật ', 2025 + (i % 2), '</em></p>'
                );

        END CASE;

        -- Truncate slug to 300 chars to satisfy column constraint
        SET v_slug = LEFT(REGEXP_REPLACE(v_slug, '[^a-z0-9\\-]', ''), 290);
        SET v_slug = CONCAT(v_slug, '-', i);

        INSERT IGNORE INTO news (
            title, slug, summary, content, category,
            tags, thumbnail_url, status, published_at,
            author_name, view_count,
            meta_title, meta_description, meta_keywords
        ) VALUES (
            v_title, v_slug, v_summ, v_cont, cat,
            v_tags, v_thumb, stat, pub_at,
            v_auth, v_views,
            LEFT(v_meta_t, 255), LEFT(v_meta_d, 500), LEFT(v_meta_k, 500)
        );

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL populate_news();
DROP PROCEDURE IF EXISTS populate_news;

-- Verify
SELECT category, status, COUNT(*) AS cnt
FROM news
GROUP BY category, status
ORDER BY category, status;
