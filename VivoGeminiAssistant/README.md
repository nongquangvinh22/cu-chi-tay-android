# Vinh Circle

**Bản quyền của Nông Quang Vinh.**

Thanh cử chỉ cho Vivo X200 Pro (OriginOS): gọi Circle to Search, Gemini và Gemini Live, đọc thông báo bằng giọng trên máy. Không root. Không phải ứng dụng của Google hay vivo.

## Cài APK

Tệp đã build:

`VivoGeminiAssistant/dist/VinhCircle.apk`

Hoặc bản phát hành GitHub Actions (tệp `app-debug.apk`).

Trên máy: cho phép cài nguồn không xác định, cài đè được vì khóa ký gỡ lỗi nằm trong repo.

## Dùng trên X200 Pro

1. Cài ứng dụng Google và Gemini, đăng nhập.
2. Đặt **Google** làm trợ lý mặc định. Đừng chọn Vinh Circle.
3. Cấp quyền hiện trên ứng dụng khác, rồi bật thanh.
4. Giữ thanh để Circle to Search, chạm đúp để Gemini (đổi trong tab Cử chỉ).
5. Đọc thông báo là tuỳ chọn, tắt sẵn, chỉ đọc app bạn chọn, và không gửi nội dung đi đâu.

Phím nguồn vẫn do OriginOS quyết định. Circle to Search chỉ mở được khi ứng dụng Google trên máy có màn hình đó.

## Build

JDK 17, Android SDK 35, Gradle 8.9.

```
gradle --no-daemon -p VivoGeminiAssistant :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
