# Vinh Circle

Phiên bản mã nguồn: 0.2.7

**Bản quyền của Nông Quang Vinh.**

Thanh cử chỉ cho Vivo X200 Pro (OriginOS): gọi Circle to Search, Gemini và Gemini Live, đọc thông báo bằng giọng trên máy. Không root. Không phải ứng dụng của Google hay vivo.

## Cài APK

Tải APK mới nhất tại trang [GitHub Releases](https://github.com/nongquangvinh22/cu-chi-tay-android/releases). Mỗi bản phát hành có tệp `VinhCircle.apk` riêng. Hãy chọn bản mới nhất theo ngày và phiên bản.

Trên máy: cho phép cài nguồn không xác định, cài đè được vì khóa ký gỡ lỗi nằm trong repo.

## Dùng trên X200 Pro

1. Cài ứng dụng Google và Gemini, đăng nhập.
2. Đặt **Google** làm trợ lý mặc định. Đừng chọn Vinh Circle.
3. Cấp quyền hiện trên ứng dụng khác, rồi bật thanh.
4. Giữ thanh để Circle to Search, chạm đúp để Gemini (đổi trong tab Cử chỉ).
5. Đọc thông báo là tuỳ chọn, tắt sẵn, chỉ đọc app bạn chọn, và không gửi nội dung đi đâu.

Phím nguồn vẫn do OriginOS quyết định. Circle to Search chỉ mở được khi ứng dụng Google trên máy có màn hình đó.

## Chế độ An toàn ngân hàng

- Thêm ô **Bank an toàn** trong Cài đặt nhanh: vuốt bảng xuống hai lần, chọn Sửa, rồi kéo ô Bank an toàn ra vùng đang dùng.
- Khi bật, ứng dụng tắt thanh nổi và tắt đọc thông báo. Khi tắt ô này, hai tính năng vẫn giữ trạng thái tắt; bạn phải chủ động bật lại trong ứng dụng.
- Bật chế độ này trước khi mở ứng dụng ngân hàng. Đây là công tắc tắt tính năng của Vinh Circle, không phải công cụ vượt qua kiểm tra bảo mật.
- Quyền “hiển thị trên ứng dụng khác” có thể vẫn được cấp trong Android. Một số ngân hàng có thể phát hiện quyền này hoặc ứng dụng đã cài; không thể bảo đảm mọi ngân hàng không cảnh báo. Để giảm quyền hơn nữa, hãy thu hồi quyền overlay trong Cài đặt Android khi không cần thanh nổi.
- Đọc thông báo tắt mặc định và danh sách ứng dụng được đọc để trống. Không chọn ứng dụng ngân hàng, ví hoặc OTP.

## Build

JDK 17, Android SDK 35, Gradle 8.9.

```
gradle --no-daemon -p VivoGeminiAssistant :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
