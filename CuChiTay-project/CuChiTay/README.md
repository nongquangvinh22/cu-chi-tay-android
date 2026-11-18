# Cử Chỉ Tay – điều khiển điện thoại bằng cử chỉ trước camera

Ứng dụng Android viết bằng Kotlin, dùng MediaPipe Hand Landmarker và Android Accessibility Service.

## Chức năng

- **Hất tay lên / xuống:** vuốt màn hình để cuộn.
- **Xòe tay rồi nắm lại:** chụp màn hình.
- Có thể bật thêm hất tay trái / phải và đảo chiều cuộn.
- **Độ nhạy nhận diện:** điều chỉnh ngưỡng bắt cử chỉ.
- **Tốc độ cuộn:** điều chỉnh thời lượng vuốt; kéo về bên phải để vuốt nhanh hơn.
- Nhận diện được lọc bằng độ vươn và độ thẳng của các ngón, đồng thời làm mượt chuyển động lòng bàn tay để giảm nhận nhầm do rung tay.

Ảnh camera được xử lý tại chỗ trong bộ nhớ, không lưu và không gửi lên máy chủ. Dịch vụ chỉ thực hiện thao tác vuốt và chụp màn hình theo cử chỉ, không đọc nội dung màn hình.

## Tạo APK bằng GitHub Actions

Repository đã có workflow build riêng cho ứng dụng này.

1. Mở tab **Actions** của repository.
2. Chọn workflow **Build Cử Chỉ Tay APK**.
3. Chọn **Run workflow** nếu muốn chủ động build lại. Các thay đổi trong thư mục dự án cũng tự kích hoạt workflow.
4. Mở lần chạy mới nhất. Khi trạng thái hoàn tất thành công, vào **Artifacts** và tải **CuChiTay-apk-v2**.
5. Giải nén tệp ZIP để lấy app-debug.apk, sau đó chuyển sang điện thoại và cài đặt.

Workflow tự tải mô hình hand_landmarker.task của MediaPipe trước khi biên dịch.

## Build bằng Android Studio

1. Mở thư mục CuChiTay-project/CuChiTay trong Android Studio.
2. Để Gradle đồng bộ.
3. Tải mô hình từ đường dẫn trong app/src/main/assets/README.txt và đặt tên hand_landmarker.task trong thư mục app/src/main/assets/.
4. Chọn **Build → Build Bundle(s) / APK(s) → Build APK(s)**. APK debug nằm tại app/build/outputs/apk/debug/app-debug.apk.

## Cài đặt và cấp quyền

1. Cài APK và mở ứng dụng **Cử Chỉ Tay**.
2. Cấp quyền Camera và thông báo.
3. Mở **Cài đặt → Trợ năng → Dịch vụ đã tải xuống / Ứng dụng đã cài đặt → Cử Chỉ Tay** và bật dịch vụ.
4. Quay lại app và nhấn **BẬT ĐIỀU KHIỂN BẰNG CỬ CHỈ**.
5. Đặt điện thoại thẳng đứng, để camera trước nhìn rõ bàn tay ở khoảng 25–50 cm và nơi đủ sáng.

## Điều chỉnh

- Nếu app nhận nhầm khi tay đứng yên, giảm **Độ nhạy nhận diện**.
- Nếu khó nhận ra cú hất, tăng độ nhạy và đảm bảo đủ sáng.
- Kéo **Tốc độ cuộn** sang phải để thao tác vuốt nhanh hơn; sang trái để vuốt chậm hơn.
- Nếu cuộn ngược hướng, bật **Đảo chiều cuộn lên / xuống**.

## Lưu ý

- Khi app khác đang sử dụng camera trước, nhận diện có thể tạm ngừng.
- Camera chạy liên tục sẽ tiêu thụ pin; hãy tắt điều khiển khi không dùng.
- Sau khi khởi động lại điện thoại, mở ứng dụng và bật dịch vụ lại.
- Hiệu quả nhận diện còn phụ thuộc ánh sáng, góc camera và khoảng cách; cần thử trên thiết bị thật để chọn độ nhạy phù hợp.

---

**Bản quyền của Nông Quang Vinh**
