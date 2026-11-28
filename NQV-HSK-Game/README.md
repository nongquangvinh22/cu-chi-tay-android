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

## Tài khoản trực tuyến và đồng bộ nhiều thiết bị

Ứng dụng đã tích hợp Supabase Auth + PostgreSQL để đăng ký bằng email, đăng nhập nhiều thiết bị và đồng bộ hồ sơ/tiến trình. Để kích hoạt đồng bộ thật sự:

1. Tạo một project miễn phí tại https://supabase.com.
2. Mở **SQL Editor**, dán và chạy toàn bộ nội dung file `supabase-schema.sql`.
3. Vào **Project Settings → API**. Sao chép **Project URL** và **anon/public key** (không dùng service_role).
4. Mở `supabase-config.js`, thay hai giá trị `YOUR_SUPABASE_PROJECT_URL` và `YOUR_SUPABASE_ANON_KEY`.
5. Trong Supabase → **Authentication → Providers → Email**, bật Email provider. Để đăng nhập ngay sau đăng ký, có thể tắt xác nhận email; nếu bật xác nhận, người dùng phải xác nhận email trước khi đăng nhập.
6. Commit thay đổi và triển khai lại GitHub Pages. Tài khoản đăng ký sau khi cấu hình sẽ đồng bộ qua các thiết bị.

**Bảo mật:** bảng hồ sơ bật Row Level Security; mỗi người dùng chỉ được đọc/ghi dòng có `id = auth.uid()`. Chỉ đưa anon/public key vào trình duyệt, tuyệt đối không đưa service_role key vào repository.

Nếu chưa điền cấu hình Supabase, chế độ khách vẫn hoạt động; đăng ký/đăng nhập trực tuyến sẽ báo cần cấu hình máy chủ.

## Lưu trữ tài khoản và giới hạn quan trọng

Khi chưa cấu hình Supabase, game chỉ có chế độ khách và dữ liệu cục bộ. Khi Supabase đã cấu hình, xác thực và đồng bộ được xử lý bởi máy chủ; cookie không còn là nguồn xác thực chính. Vì vậy:

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
