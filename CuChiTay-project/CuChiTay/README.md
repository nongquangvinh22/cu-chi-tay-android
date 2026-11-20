# Cử Chỉ Tay 2.1 — Gesture Precision Edition

Ứng dụng Android viết bằng Kotlin, dùng MediaPipe Hand Landmarker và Android Accessibility Service để điều khiển điện thoại bằng cử chỉ trước camera.

## Phiên bản 2.1

Bản 2.1 cải thiện độ ổn định nhận diện các cú hất lên, xuống, trái và phải bằng ngưỡng di chuyển thích ứng theo kích thước bàn tay, tốc độ và độ thẳng quỹ đạo, trung bình mẫu đầu/cuối và cơ chế chờ tay ổn định trước khi nhận cú hất tiếp theo. Biểu tượng ứng dụng cũng được thiết kế lại theo phong cách kính phát sáng xanh-tím.

Các tính năng, thiết lập, giao diện Liquid Glass, công tắc và thao tác của bản 2.0 được giữ nguyên.

## Chức năng

- Hất tay lên để cuộn lên: công tắc riêng.
- Hất tay xuống để cuộn xuống: công tắc riêng.
- Hất tay sang trái: công tắc riêng.
- Hất tay sang phải: công tắc riêng.
- Xòe tay rồi nắm lại để chụp màn hình: công tắc riêng.
- Đảo chiều cuộn: tùy chọn độc lập.
- Độ nhạy nhận diện: điều chỉnh ngưỡng bắt cử chỉ.
- Tốc độ cuộn: điều chỉnh thời lượng vuốt.

Các lựa chọn đã lưu của người dùng được giữ nguyên khi nâng cấp. Ảnh camera được xử lý tại chỗ trong bộ nhớ, không lưu và không gửi lên máy chủ.

## Tạo APK bằng GitHub Actions

1. Mở tab Actions của repository.
2. Chọn workflow Build Cử Chỉ Tay APK.
3. Các thay đổi trong thư mục dự án tự kích hoạt workflow.
4. Mở lần chạy mới nhất. Khi hoàn tất thành công, vào Artifacts và tải CuChiTay-apk-v3.
5. Giải nén ZIP để lấy app-debug.apk, sau đó chuyển sang điện thoại và cài đặt.

Workflow tự tải mô hình hand_landmarker.task của MediaPipe trước khi biên dịch.

## Cài đặt và cấp quyền

1. Cài APK và mở ứng dụng Cử Chỉ Tay.
2. Cấp quyền Camera và thông báo.
3. Mở Cài đặt → Trợ năng → Dịch vụ đã tải xuống / Ứng dụng đã cài đặt → Cử Chỉ Tay và bật dịch vụ.
4. Quay lại app và nhấn BẬT ĐIỀU KHIỂN BẰNG CỬ CHỈ.
5. Đặt điện thoại thẳng đứng, để camera trước nhìn rõ bàn tay ở khoảng 25–50 cm và nơi đủ sáng.

## Điều chỉnh

- Nếu app nhận nhầm khi tay đứng yên, giảm Độ nhạy nhận diện.
- Nếu khó nhận ra cú hất, tăng độ nhạy, thực hiện cú hất dứt khoát và đảm bảo đủ sáng.
- Kéo Tốc độ cuộn sang phải để thao tác vuốt nhanh hơn; sang trái để vuốt chậm hơn.
- Chỉ bật những hướng vuốt bạn muốn sử dụng. Mỗi hướng có thể hoạt động độc lập.
- Nếu cần đổi hướng cuộn, bật Đảo chiều cuộn lên / xuống.

## Lưu ý

- Sau mỗi cú hất, giữ bàn tay ổn định trong chốc lát để bộ nhận diện sẵn sàng cho cử chỉ kế tiếp.
- Khi app khác đang sử dụng camera trước, nhận diện có thể tạm ngừng.
- Camera chạy liên tục sẽ tiêu thụ pin; hãy tắt điều khiển khi không dùng.
- Sau khi khởi động lại điện thoại, mở ứng dụng và bật dịch vụ lại.
- Độ chính xác thực tế còn phụ thuộc ánh sáng, góc camera và khoảng cách; cần kiểm tra trên thiết bị thật.

---

Bản quyền của Nông Quang Vinh
