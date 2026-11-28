# NQV Khoa Cử Hán Ngữ

Game web học tiếng Trung theo hành trình khoa cử cổ phong, từ HSK 1 đến HSK 6. Dự án được tách riêng trong repository, không thay đổi các ứng dụng Android hiện có.

## Tính năng trong bản khởi tạo

- Giao diện responsive cho máy tính, máy tính bảng và điện thoại.
- Khu sân trường có nhân vật sĩ tử gấu trúc vẽ bằng SVG; di chuyển bằng phím mũi tên, WASD hoặc cụm phím cảm ứng.
- Thư viện bài học: 6 cấp HSK, 2 bài/cấp ở bộ học liệu ban đầu, có chữ Hán, pinyin, nghĩa, câu ví dụ và phát âm bằng Speech Synthesis của trình duyệt nếu có hỗ trợ.
- Khoa thi bắt buộc: cần hoàn thành đủ bài học của cấp hiện tại, làm bài thi 8 câu và đạt ít nhất 6/8 mới mở cấp tiếp theo.
- Danh vị tiến cấp: Học trò → Đồng sinh → Tú tài → Cử nhân → Tiến sĩ → Hoàng giáp → Trạng nguyên.
- Sau khi đỗ Trạng nguyên, người chơi mở chương quan lộ và chọn một chức vị khởi đầu.
- Bảng thành tích, điểm công danh, ngày học và tiến trình được tự động lưu.
- PWA cơ bản: có manifest và service worker để cache app shell khi chạy trên HTTPS.
- Dấu bản quyền `© Nông Quang Vinh` ở chân trang.

## Chạy thử trên máy tính

Có thể mở trực tiếp `index.html` để xem giao diện. Để thử đăng ký/đăng nhập, cookie, phát âm và PWA, nên chạy qua máy chủ HTTPS hoặc localhost:

```bash
python -m http.server 8080
```

Sau đó truy cập `http://localhost:8080/NQV-HSK-Game/` nếu chạy máy chủ ở thư mục cha, hoặc `http://localhost:8080/` nếu chạy bên trong thư mục này.

## Lưu trữ tài khoản và giới hạn quan trọng

Bản hiện tại là ứng dụng front-end tĩnh, chưa có máy chủ tài khoản. Đăng ký tạo hồ sơ cục bộ trong trình duyệt bằng PBKDF2 + salt để không lưu mật khẩu dạng văn bản; cookie `nqv_hsk_player` chỉ ghi nhớ ID hồ sơ trên thiết bị. Vì vậy:

- Có thể tiếp tục chơi mà không phải đăng nhập lại trên cùng trình duyệt/thiết bị nếu đã bật ghi nhớ.
- Xóa dữ liệu trang web, đổi trình duyệt hoặc đổi thiết bị có thể làm mất/không thấy hồ sơ.
- Tài khoản chưa đồng bộ qua Internet và chưa có khả năng khôi phục bằng email.
- Không dùng cơ chế này cho dữ liệu nhạy cảm hoặc tài khoản có giá trị. Muốn đồng bộ thật sự giữa điện thoại và máy tính cần backend, cơ sở dữ liệu và xác thực phía máy chủ.

## Ghi chú về học liệu

Đây là bộ khung game có dữ liệu từ vựng mẫu để chơi thử vòng lặp học → luyện → thi → tiến cấp. Mỗi HSK hiện có 2 bài với 4 từ/bài; chưa phải toàn bộ danh mục từ vựng hay đề thi chuẩn hóa HSK. Nội dung nên tiếp tục được mở rộng, biên tập và kiểm tra chuyên môn trước khi quảng bá như khóa học đầy đủ.

## Kiểm tra nhanh

- JavaScript thuần, không cần cài framework hoặc build tool.
- `data.js`, `auth.js`, `app.js` được nạp theo thứ tự trong `index.html`.
- Chạy kiểm tra cú pháp bằng `node --check data.js`, `node --check auth.js`, `node --check app.js`, `node --check sw.js`.

© Nông Quang Vinh

## Đưa game lên GitHub Pages

Repository đã có workflow kiểm tra cú pháp và triển khai thư mục game lên GitHub Pages sau khi có thay đổi. Trong GitHub, mở **Settings → Pages → Build and deployment → Source → GitHub Actions**. Sau đó vào **Actions** để theo dõi lần triển khai. Khi workflow thành công, GitHub cung cấp URL trang game trong kết quả deploy (thường có dạng `https://nongquangvinh22.github.io/cu-chi-tay-android/`). Việc bật Pages lần đầu cần thao tác trong phần Settings của repository.
