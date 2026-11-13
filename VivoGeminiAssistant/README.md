# Vivo Gemini Assistant

**Bản quyền của Nông Quang Vinh.**

Ứng dụng Android thử nghiệm dành cho Vivo X200 Pro bản nội địa / OriginOS 5, theo hướng không root, ít quyền và không chạy nền thường trực.

## Tải APK

Khi GitHub Actions build thành công, APK sẽ được đính kèm trong mục Releases:

https://github.com/nongquangvinh22/cu-chi-tay-android/releases/latest/download/app-debug.apk

Đây là APK debug thử nghiệm, chưa phải bản phát hành chính thức. Hãy chỉ cài khi bạn tin tưởng mã nguồn trong kho. Các bản build sau có thể cần gỡ bản cũ trước khi cài bản mới vì khóa ký debug trên CI có thể thay đổi.

## Mở project

Mở thư mục VivoGeminiAssistant bằng Android Studio. Chọn JDK 17, Android SDK Platform 35 và Gradle 8.9.

Có thể build bằng lệnh gradle --no-daemon :app:assembleDebug.

APK đầu ra: app/build/outputs/apk/debug/app-debug.apk.

## Tính năng MVP

- Mở ứng dụng Gemini nếu đã cài; nếu không thì mở Gemini trên web.
- Đăng ký các intent trợ lý Android chuẩn và yêu cầu vai trò trợ lý mặc định.
- Quick Settings Tile mở Gemini khi người dùng chủ động chạm.
- Hướng dẫn chụp ảnh màn hình rồi chia sẻ thủ công sang Google Lens.
- Không yêu cầu root, Trợ năng, overlay, micro, VPN hoặc dịch vụ nền thường trực.
- Hiển thị dòng “Bản quyền của Nông Quang Vinh” ở phần đầu và cuối giao diện.

## Giới hạn

- Giữ nút nguồn do OriginOS quyết định; đăng ký trợ lý không đảm bảo chiếm được phím nguồn.
- Circle to Search nguyên bản không được triển khai trong APK không root này.
- Không thể bảo đảm mọi ứng dụng ngân hàng sẽ tương thích; app tránh can thiệp vào app khác và tránh quyền nhạy cảm.
- Chưa được xác nhận trên thiết bị thật. Cần kiểm tra trên Vivo X200 Pro / OriginOS 5.

Đây là dự án độc lập, không phải sản phẩm chính thức hoặc được bảo trợ bởi Google hay vivo.
