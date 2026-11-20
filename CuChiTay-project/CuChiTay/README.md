# Cử Chỉ Tay 2.0 — Liquid Glass Edition

Ứng dụng Android viết bằng Kotlin, dùng MediaPipe Hand Landmarker và Android Accessibility Service để điều khiển điện thoại bằng cử chỉ trước camera.

## Phiên bản 2.0

Bản 2.0 nâng cấp toàn diện giao diện theo cảm hứng Liquid Glass trên iPhone: nền chuyển sắc xanh đêm, các lớp thẻ kính trong mờ, đường viền phản sáng, biểu tượng đồng bộ, công tắc có track/thumb riêng và hiệu ứng phản hồi khi gạt. Bố cục được tối ưu cho màn hình điện thoại, giữ nguyên các chức năng và thao tác của phiên bản 1.2.

## Chức năng

- **Hất tay lên để cuộn lên:** công tắc riêng.
- **Hất tay xuống để cuộn xuống:** công tắc riêng.
- **Hất tay sang trái:** công tắc riêng.
- **Hất tay sang phải:** công tắc riêng.
- **Xòe tay rồi nắm lại để chụp màn hình:** công tắc riêng.
- **Đảo chiều cuộn:** tùy chọn độc lập.
- **Độ nhạy nhận diện:** điều chỉnh ngưỡng bắt cử chỉ.
- **Tốc độ cuộn:** điều chỉnh thời lượng vuốt; kéo sang phải để vuốt nhanh hơn.

Ứng dụng tự chuyển thiết lập từ hai công tắc gộp của phiên bản 1.1 sang các công tắc riêng khi mở bản mới lần đầu. Các lựa chọn đã được lưu của người dùng được giữ nguyên khi nâng cấp.

Ảnh camera được xử lý tại chỗ trong bộ nhớ, không lưu và không gửi lên máy chủ. Dịch vụ chỉ thực hiện thao tác vuốt và chụp màn hình theo cử chỉ, không đọc nội dung màn hình.

## Tạo APK bằng GitHub Actions

1. Mở tab **Actions** của repository.
2. Chọn workflow **Build Cử Chỉ Tay APK**.
3. Các thay đổi trong thư mục dự án tự kích hoạt workflow build.
4. Mở lần chạy mới nhất. Khi trạng thái hoàn tất thành công, vào **Artifacts** và tải **CuChiTay-apk-v3**.
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
- Chỉ bật những hướng vuốt bạn muốn sử dụng. Mỗi hướng có thể hoạt động độc lập.
- Nếu cần đổi hướng cuộn, bật **Đảo chiều cuộn lên / xuống**. Khi một hướng đích đang tắt, đảo chiều cũng không thực hiện hướng đó.

## Lưu ý

- Khi app khác đang sử dụng camera trước, nhận diện có thể tạm ngừng.
- Camera chạy liên tục sẽ tiêu thụ pin; hãy tắt điều khiển khi không dùng.
- Sau khi khởi động lại điện thoại, mở ứng dụng và bật dịch vụ lại.
- Hiệu quả nhận diện còn phụ thuộc ánh sáng, góc camera và khoảng cách; cần thử trên thiết bị thật để chọn độ nhạy phù hợp.

---

**Bản quyền của Nông Quang Vinh**
