# Cử Chỉ Tay 3.0 — Liquid Glass Edition

Ứng dụng Android Kotlin cho điện thoại Android ARM64. GitHub Actions tự build APK nên không cần cài Android Studio trên máy cá nhân.

## Chức năng
- Giao diện Liquid Glass, dock điều hướng ba trang, hiệu ứng chạm/công tắc.
- Khung xem camera trực tiếp, vẽ 21 điểm landmark, nhận diện trạng thái năm ngón, tư thế bàn tay, độ ổn định và nhật ký.
- Chỉ mở thử cử chỉ sau khi tay được nhận diện ổn định; thử hất lên/xuống/trái/phải và xòe tay rồi nắm lại.
- Giữ lại GestureEngine, GestureService và GestureAccessibilityService Kotlin của bản 2.1 để vuốt toàn hệ thống và chụp màn hình qua Trợ năng.
- Game Khủng Long kiểu chạy vô tận và Block Blast bảng 10×10, có điểm/kỷ lục, combo và hoàn tác.

Camera xem trước và camera nền điều khiển toàn hệ thống dùng chung camera trước nên không hoạt động đồng thời. Khi bật điều khiển toàn hệ thống, app tự dừng camera xem trước rồi chạy dịch vụ Android. Khi muốn xem nhận diện, tắt điều khiển toàn hệ thống rồi bật camera trong trang Cử Chỉ.

Lần đầu sử dụng cần Internet để tải MediaPipe Tasks Vision và mô hình Hand Landmarker. Giao diện và hai game được đóng gói trong APK; khung xem trước xử lý hình ảnh trong thiết bị.

## Build APK không cần Android Studio
1. Mở repository nongquangvinh22/cu-chi-tay-android trên GitHub.
2. Vào Actions và chọn workflow Build Cử Chỉ Tay APK.
3. Mở lần chạy mới nhất có trạng thái thành công.
4. Tải artifact CuChiTay-apk-v3 ở cuối trang.
5. Giải nén ZIP để lấy app-debug.apk và cài trên Vivo.

Workflow tự cài Java/Gradle, tải mô hình MediaPipe, giải nén giao diện và build APK trên máy chủ GitHub. APK 3.0 dùng application ID com.cuchitay.app.v3 để có thể cài song song với bản 2.1. Đây là APK debug cá nhân; artifact được GitHub giữ trong thời hạn đã cấu hình.

## Quyền cần cấp
- Camera: xem bàn tay và nhận diện cử chỉ.
- Thông báo: trạng thái dịch vụ khi điều khiển toàn hệ thống.
- Trợ năng: vuốt màn hình và chụp màn hình theo cử chỉ đã bật. Người dùng tự xác nhận quyền trong Cài đặt Android.

Bản quyền: Nông Quang Vinh.
