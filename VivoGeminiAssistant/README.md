# Vinh Circle

Phiên bản mã nguồn: 0.4.2

**Bản quyền của Nông Quang Vinh.**

Trợ lý thao tác nhanh cho Vivo X200 Pro / OriginOS, lấy cảm hứng từ giao diện Circle Bar trong tài liệu tham khảo. Không root. Không phải ứng dụng do Google hoặc vivo phát hành.

## Thay đổi ở 0.4.2

- Đồng bộ thanh cử chỉ xem trước trong app với thanh nổi thật: chạm một lần chạy `singleAction`; nhấn đúp, nhấn ba và nhấn giữ tiếp tục hoạt động riêng.

## Thay đổi ở 0.4.1

- Bổ sung gán hành động cho **chạm một lần**; mặc định là Circle to Search. Có thể chọn Vinh AI nổi, Gemini, Dịch đoạn hoặc Tắt.
- Giữ nguyên nhấn giữ mở Vinh AI nổi, nhấn đúp mở Gemini và nhấn ba lần mở Dịch đoạn theo cấu hình.

## Thay đổi ở 0.4.0

- Giữ thanh cử chỉ để mở **Vinh AI dạng cửa sổ nổi trên ứng dụng hiện tại**, không chuyển sang màn hình ứng dụng AI riêng.
- Khung chat Gemini nổi hỗ trợ nhập yêu cầu trực tiếp; thu nhỏ thành bong bóng AI, đóng khi không dùng.
- Có xác nhận ngay trong khung nổi trước khi mở ứng dụng, đặt báo thức hoặc hẹn giờ.
- Cử chỉ nhấn giữ mặc định chuyển thành Vinh AI nổi. Có thể đổi lại Circle to Search/Gemini/Dịch trong phần Cử chỉ.
- Độ hiển thị thanh chỉnh từ 0–100%; mức 0 làm thanh trong suốt hoàn toàn nhưng vùng chạm vẫn hoạt động.
- Vị trí thanh ngang mặc định sát cạnh dưới (0 dp); chỉnh khoảng cách trong phần Thanh.

## Cấu hình Vinh AI

1. Tạo khóa Gemini API trong Google AI Studio: https://aistudio.google.com/app/apikey
2. Bật quyền **Hiển thị trên ứng dụng khác** cho Vinh Circle.
3. Trong tab Cử chỉ, chọn **Vinh AI nổi trên màn hình** cho thao tác nhấn giữ (đã chọn mặc định).
4. Giữ thanh cử chỉ ở cạnh dưới để hiện khung chat nổi.
5. Dán khóa API vào khung chat một lần, sau đó nhập câu hỏi. Khóa được mã hóa bằng Android Keystore.

Vinh AI gọi Gemini API trực tiếp qua HTTPS, không chuyển cuộc trò chuyện sang trình duyệt. API key/API quota là dịch vụ riêng; người dùng tự quản lý hạn mức của khóa trong Google AI Studio/Cloud.

Lưu ý: khóa được mã hóa khi lưu trên thiết bị, nhưng bất kỳ khóa API nào được dùng trực tiếp trong ứng dụng người dùng cuối vẫn có rủi ro bị trích xuất từ thiết bị hoặc tiến trình đang chạy. Hãy đặt hạn mức và theo dõi usage.

## Vị trí và độ trong suốt của thanh

- Mở tab **Thanh**.
- **Cách mép dưới = 0 dp** đặt thanh sát đáy màn hình; tăng số dp để đưa thanh lên cao hơn.
- **Độ hiển thị = 0%** làm thanh không nhìn thấy nhưng cửa sổ vẫn nhận thao tác chạm/nhấn giữ; tăng dần để nhìn thấy rõ hơn.
- Có thể bật kéo thả để đặt vị trí lệch ngang; tắt kéo thả để sử dụng cử chỉ.
- Khi thanh hoàn toàn trong suốt, bạn sẽ không nhìn thấy vị trí của nó. Dùng tab Thanh để đưa độ hiển thị lên lại.

## Giới hạn Android/OriginOS

- Android yêu cầu người dùng tự cấp quyền overlay; APK không thể tự cấp quyền.
- Vinh AI nổi là cửa sổ chat trên ứng dụng hiện tại, không tự đọc nội dung ứng dụng phía sau. Muốn AI đọc màn hình cần quyền Accessibility/Screen Capture riêng và xác nhận rõ ràng.
- Circle to Search nguyên bản vẫn phụ thuộc Google app, firmware và việc bật tính năng hệ thống.
- AI có thể yêu cầu mở ứng dụng, đặt báo thức hoặc hẹn giờ qua Android Intent; ứng dụng yêu cầu xác nhận trước khi thực hiện.
- Bản 0.4.1 chưa có quyền Trợ năng/MediaProjection để AI tự đọc mọi nội dung và bấm/cuộn tùy ý trong ứng dụng khác. Phần đó cần triển khai riêng và chỉ hoạt động sau khi người dùng cấp quyền.
- OriginOS có thể yêu cầu cho phép tự khởi động và bỏ giới hạn pin để thanh nổi duy trì ổn định.

## Build

JDK 17, Android SDK 35, Gradle 8.9.

```bash
gradle --no-daemon -p VivoGeminiAssistant :app:assembleDebug
```

APK: `VivoGeminiAssistant/app/build/outputs/apk/debug/app-debug.apk`.
