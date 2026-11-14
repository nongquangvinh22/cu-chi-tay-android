# Vinh Circle

Phiên bản mã nguồn: 0.3.0

**Bản quyền của Nông Quang Vinh.**

Ứng dụng trợ lý thao tác nhanh cho Vivo X200 Pro / OriginOS, lấy cảm hứng từ giao diện Circle Bar trong tài liệu tham khảo. Không root. Không phải ứng dụng do Google hoặc vivo phát hành.

## Tính năng

- Thanh cử chỉ nổi, tùy chỉnh màu, kích thước, vị trí và độ trong suốt.
- Cử chỉ để mở Google/Circle to Search, Gemini và Google Dịch. Tính năng Circle to Search nguyên bản vẫn phụ thuộc vào bản Google app, firmware và việc bật tính năng hệ thống.
- **Vinh AI:** màn hình trò chuyện native gọi trực tiếp Gemini API qua HTTPS, không chuyển cuộc trò chuyện sang trình duyệt.
- Nhận yêu cầu bằng giọng nói tiếng Việt qua dịch vụ nhận dạng giọng nói Android; có thể đọc câu trả lời bằng TTS.
- Gemini function calling để đề xuất mở ứng dụng, đặt báo thức, hẹn giờ hoặc mở các ứng dụng Google. Người dùng phải xác nhận từng thao tác trước khi ứng dụng thực hiện.
- Đọc thông báo là tùy chọn, tắt sẵn; chỉ đọc app mà người dùng tự chọn trong giao diện hiện có.

## Cấu hình Vinh AI

1. Tạo khóa Gemini API trong Google AI Studio: https://aistudio.google.com/app/apikey
2. Mở Vinh Circle, chạm nút **AI TRỰC TIẾP**.
3. Dán khóa API và chọn **Lưu khóa an toàn**. Khóa được mã hóa bằng Android Keystore và lưu trên máy.
4. Nhập câu hỏi hoặc chạm **Nói** để yêu cầu bằng giọng tiếng Việt.

Vinh AI dùng model `gemini-3.8-flash` qua Gemini API. API key/API quota là dịch vụ riêng; quyền lợi Google One AI Premium hoặc ứng dụng Gemini không tự động biến thành hạn mức Gemini API. Google có thể tính phí khi vượt mức miễn phí theo tài khoản và khu vực; người dùng tự quản lý hạn mức của khóa.

Lưu ý bảo mật: khóa được mã hóa khi lưu trên thiết bị, nhưng bất kỳ khóa API nào được dùng trực tiếp trong ứng dụng người dùng cuối vẫn có rủi ro bị trích xuất từ thiết bị hoặc tiến trình đang chạy. Hãy đặt hạn mức và theo dõi usage trong Google AI Studio/Cloud.

## Các thao tác hệ thống

- Mở ứng dụng: Gemini phân tích tên ứng dụng, Vinh Circle tìm ứng dụng đã cài và mở sau khi người dùng xác nhận.
- Báo thức/hẹn giờ: ứng dụng gọi Android AlarmClock Intent và đưa người dùng tới giao diện Đồng hồ để xác nhận.
- Dịch văn bản: có thể chuyển văn bản được người dùng cung cấp sang Google Dịch.
- Circle to Search: Vinh Circle thử gọi điểm vào Google được hỗ trợ trên máy; nếu firmware không cung cấp điểm vào đó, hãy dùng cử chỉ Circle to Search nguyên bản do Google/OriginOS cung cấp. Ứng dụng bên thứ ba không thể tự cấp quyền hệ thống hoặc bảo đảm thay thế hoàn toàn giao diện Circle to Search của Google.
- Dịch toàn màn hình: tùy chọn dịch màn hình/overlay của Google phụ thuộc Google app, quyền và phiên bản OriginOS; Vinh Circle không âm thầm chụp hoặc tải màn hình lên máy chủ.

## Quyền cần cấp trên máy

1. Cài ứng dụng Google và Gemini, đăng nhập.
2. Đặt Google làm trợ lý mặc định nếu muốn gọi Google Assistant/Gemini qua phím nguồn.
3. Chỉ bật quyền Hiển thị trên ứng dụng khác nếu muốn dùng thanh nổi.
4. Quyền đọc thông báo là tùy chọn. Không chọn ứng dụng ngân hàng, ví hoặc OTP.
5. OriginOS có thể yêu cầu cho phép tự khởi động và bỏ giới hạn pin để thanh nổi duy trì ổn định.

Không có quyền nào được tự cấp bởi APK. Android/OriginOS sẽ hiển thị trang cài đặt để người dùng tự quyết định.

## Chế độ An toàn ngân hàng

- Thêm ô **Bank an toàn** trong Cài đặt nhanh: vuốt bảng xuống hai lần, chọn Sửa, rồi kéo ô Bank an toàn ra vùng đang dùng.
- Khi bật, ứng dụng tắt thanh nổi và tắt đọc thông báo. Khi tắt ô này, hai tính năng vẫn giữ trạng thái tắt; bạn phải chủ động bật lại trong ứng dụng.
- Bật chế độ này trước khi mở ứng dụng ngân hàng. Đây là công tắc tắt tính năng của Vinh Circle, không phải công cụ vượt qua kiểm tra bảo mật.
- Quyền hiển thị trên ứng dụng khác có thể vẫn được Android giữ. Một số ngân hàng có thể phát hiện quyền này hoặc ứng dụng đã cài; không thể bảo đảm mọi ngân hàng không cảnh báo.

## Build

JDK 17, Android SDK 35, Gradle 8.9.

```bash
gradle --no-daemon -p VivoGeminiAssistant :app:assembleDebug
```

APK: `VivoGeminiAssistant/app/build/outputs/apk/debug/app-debug.apk`.

Workflow GitHub Actions sẽ build APK, commit bản build vào `VivoGeminiAssistant/dist/VinhCircle.apk`, đồng thời đính kèm vào GitHub Release.
