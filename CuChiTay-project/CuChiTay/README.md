# Cử Chỉ Tay 3.3 — Liquid Glass Media Edition

Ứng dụng Android Kotlin ARM64. GitHub Actions tự build APK; không cần cài Android Studio trên máy cá nhân.

## Bản 3.3
- Nhận diện cú hất bằng tâm lòng bàn tay, ngưỡng thích ứng theo kích thước tay, vận tốc và cơ chế giữ tay ổn định trước lần nhận diện tiếp theo.
- Cử chỉ lên/xuống/trái/phải tiếp nhận bàn tay hơi co tự nhiên; vẫn phân biệt nắm tay cho thao tác xòe rồi nắm.
- Block Blast hỗ trợ kéo–thả khối trên màn hình cảm ứng, xem trước vị trí và giữ cách đặt bằng chạm.
- Khủng long endless runner: nhảy qua xương rồng, đá và gai; khoảng cách chướng ngại được cân bằng theo tốc độ, có điểm và kỷ lục.
- Media Studio: phát nhạc/video do người dùng chọn từ bộ nhớ điện thoại; phát video YouTube bằng trình phát nhúng chính thức.
- Dock bốn trang: Cử Chỉ, Khủng Long, Block Blast, Media.

YouTube sử dụng trình phát chính thức; quảng cáo do YouTube quyết định và có thể xuất hiện. Ứng dụng không chặn quảng cáo YouTube. Tệp âm thanh/video cục bộ được phát trực tiếp trong ứng dụng, không chèn quảng cáo.

Camera xem trước và camera nền điều khiển toàn hệ thống dùng chung camera trước nên không chạy đồng thời. Khi bật điều khiển toàn hệ thống, ứng dụng tự dừng camera xem trước. Để kiểm tra nhận diện, tắt điều khiển toàn hệ thống rồi bật camera trong trang Cử Chỉ.

Lần đầu sử dụng cần Internet để tải MediaPipe Tasks Vision và mô hình Hand Landmarker. Hình ảnh camera được xử lý trên thiết bị.

## Build APK không cần Android Studio
1. Mở repository nongquangvinh22/cu-chi-tay-android.
2. Chọn Actions → Build Cử Chỉ Tay APK.
3. Mở lần chạy mới nhất có trạng thái thành công.
4. Tải artifact CuChiTay-apk-v3-3.
5. Giải nén ZIP để lấy app-debug.apk.

APK dùng application ID com.cuchitay.app.v32 để cập nhật trực tiếp lên bản 3.2. Workflow giữ chữ ký debug ổn định qua các lần build; artifact lưu trong GitHub Actions 14 ngày.

## Quyền
- Camera: xem và nhận diện bàn tay.
- Trình chọn tệp: chỉ mở những tệp bạn chủ động chọn để phát.
- Thông báo và Trợ năng: cần cho điều khiển toàn hệ thống.

Bản quyền: Nông Quang Vinh.
