# Vinh Circle Native

**Phiên bản mã nguồn: 1.0.0**

**Bản quyền của Nông Quang Vinh.**

Vinh Circle được viết lại bằng giao diện Android native. Màn hình chính và các trang thiết lập không còn dùng WebView/HTML.

## Chức năng

- Giao diện Android native gồm Trang chủ, Cử chỉ, Thanh và Cài đặt.
- Cửa sổ Vinh AI nổi trên ứng dụng đang mở.
- Chọn hành động cho chạm một lần, nhấn giữ, chạm đúp và chạm ba lần.
- Chỉnh độ dài, độ dày, độ trong suốt, vị trí, hướng và màu của thanh.
- Có thể đăng ký Vinh Circle làm trợ lý mặc định Android nếu hệ điều hành cho phép.
- Có thể gọi trợ lý hiện được đặt mặc định bằng hành động trợ lý của Android.
- Chat AI dùng Gemini Developer API; khóa được mã hóa bằng Android Keystore.
- AI có thể đề xuất mở ứng dụng, mở Google Dịch, đặt báo thức hoặc hẹn giờ. Ứng dụng hỏi xác nhận trước thao tác.

## Thiết lập lần đầu

1. Mở Vinh Circle.
2. Vào **Cài đặt & quyền** và cấp **Hiển thị trên ứng dụng khác**.
3. Vào **Cài đặt & quyền → Thiết lập / thay khóa API** để thêm khóa Gemini API.
4. Quay lại Trang chủ và nhấn **Mở Vinh AI nổi**.
5. Vào **Cử chỉ** để gán hành động cho từng kiểu chạm.
6. Vào **Thanh** để chỉnh vị trí, kích thước và độ trong suốt.
7. Nếu muốn gọi Vinh Circle bằng thao tác trợ lý hệ thống, nhấn **Đặt Vinh Circle làm trợ lý mặc định**. Việc chọn trợ lý do Android/OriginOS hiển thị và quyết định.

## AI và quyền hệ thống

- Mô hình API mặc định: \`gemini-3.8-flash\` qua Gemini Developer API.
- Người dùng tự quản lý khóa API, hạn mức và các điều khoản/dịch vụ của Google.
- Không chia sẻ khóa API, mật khẩu, mã OTP hoặc thông tin thanh toán.
- Vinh Circle không tự chụp màn hình hoặc đọc nội dung ứng dụng khác.
- Trợ lý mặc định được đăng ký qua Android \`VoiceInteractionService\`; khả năng chọn và gọi bằng nút/cử chỉ hệ thống phụ thuộc OriginOS, chính sách thiết bị và việc người dùng tự chọn Vinh Circle làm trợ lý mặc định.
- Overlay cần người dùng tự cấp quyền hiển thị trên ứng dụng khác.
- Circle to Search chính hãng cần Google app, firmware và hỗ trợ của thiết bị.
- Gemini Live cần ứng dụng Gemini, tài khoản và khu vực hỗ trợ.
- Ứng dụng bên thứ ba không thể sao chép quyền AI độc quyền của vivo hoặc bảo đảm tất cả khả năng như trợ lý hệ thống tích hợp sẵn.

## Build

Yêu cầu JDK 17, Android SDK 35 và Gradle 8.9.

\`\`\`bash
gradle --no-daemon -p VivoGeminiAssistant :app:assembleDebug
\`\`\`

APK được tạo tại \`VivoGeminiAssistant/app/build/outputs/apk/debug/app-debug.apk\`.
