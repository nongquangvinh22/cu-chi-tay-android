# Cử Chỉ Tay 3.4 — Liquid Glass Web Edition

Ứng dụng Android ARM64; GitHub Actions tự tạo APK, không cần Android Studio trên máy cá nhân.

## Nâng cấp 3.4
- **Khủng long tự lái:** nhân vật 2D hoạt họa tự chạy vô tận và tự canh thời điểm nhảy qua xương rồng, đá, bẫy gai; tính điểm quãng đường, số chướng ngại vượt qua và kỷ lục.
- **Block Blast:** kéo khối trực tiếp bằng ngón tay đến bảng 10×10; vị trí hợp lệ mới được tô sáng và nhận khối. Vị trí không hợp lệ không làm mất khối, có thể kéo đến vị trí khác.
- **Trình duyệt NQV:** trình duyệt Android nhiều tab, ô địa chỉ kiêm tìm kiếm Google SafeSearch, quay lại/tiến, tải lại, tải tệp và phát nội dung web. Chỉ cho phép điều hướng HTTP/HTTPS, chặn quyền camera/microphone cho website và bật Safe Browsing của WebView.
- **Cử chỉ nhanh hơn:** hạ thời gian chờ mặc định xuống 220 ms, có thể điều chỉnh 120–800 ms; giảm thời gian vuốt và ngưỡng bắt chuyển động, cho phép tư thế bàn tay tự nhiên hơn.
- **Giao diện:** nền và thẻ kính mờ trong suốt, chuyển động chuyển trang kiểu spring, dock 5 mục, bản quyền Nông Quang Vinh trên cả năm trang và trong trình duyệt.

## Các trang
1. Cử Chỉ Tay — quan sát camera, 21 điểm khớp, thử từng cử chỉ và điều khiển toàn hệ thống.
2. Khủng Long — chế độ tự lái 2D.
3. Block Blast — kéo thả các khối.
4. Media — chọn và phát tệp nhạc/video từ máy; YouTube dùng trình phát chính thức và vẫn có thể hiển thị quảng cáo theo YouTube.
5. Trình duyệt — WebView nhiều thẻ, tìm kiếm, website, tải xuống.

## Thay đổi về cử chỉ
Bản 3.4 tự chuyển giá trị chờ mặc định cũ 700 ms về 220 ms trong lần mở đầu tiên sau khi cập nhật. Bạn vẫn có thể chỉnh từ 120 đến 800 ms trong mục Hiệu chỉnh. Camera xem trước và dịch vụ điều khiển nền vẫn dùng chung camera nên không chạy đồng thời.

## Build APK không cần Android Studio
1. Mở repository nongquangvinh22/cu-chi-tay-android.
2. Chọn Actions → Build Cử Chỉ Tay APK.
3. Mở lần chạy thành công mới nhất.
4. Tải artifact CuChiTay-apk-v3-4.
5. Giải nén ZIP để lấy app-debug.apk.

APK giữ nguyên application ID com.cuchitay.app.v32 để có thể cập nhật lên bản 3.3. Workflow dùng chữ ký debug ổn định và lưu artifact 14 ngày.

## Quyền
- Camera: nhận diện bàn tay và dùng điều khiển toàn hệ thống.
- Internet: trang Media, trình duyệt và tải model lần đầu.
- Trợ năng: thực hiện thao tác vuốt đã được người dùng bật.
- Tệp: chỉ truy cập tệp mà người dùng chủ động chọn.

Bản quyền: Nông Quang Vinh.
