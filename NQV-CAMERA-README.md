# NQV Camera — Android MVP

Ứng dụng camera Android độc lập, ưu tiên kiểm thử trên Vivo X200 Pro / OriginOS 5.

## MVP hiện có
- CameraX preview, chuyển camera trước/sau, bật flash, chạm lấy nét và pinch-to-zoom.
- 9 creative color looks có thanh chỉnh cường độ: Natural, Leica-inspired, Zeiss-inspired, X-MAGE-inspired, Portra-inspired, Classic Negative-inspired, Monochrome, Night Film, Golden Hour.
- Hiệu ứng GPU qua CameraX Media3Effect, gắn vào Preview, ImageCapture và VideoCapture.
- Chụp JPEG ưu tiên chất lượng, lưu DCIM/NQV Camera.
- Quay MP4 có bộ lọc trực tiếp, có thể quay không âm thanh nếu không cấp quyền micro.
- ML Kit Object Detection chạy trên thiết bị và vẽ lưới 1/3, khung chủ thể cùng gợi ý căn bố cục.
- Logo launcher tùy chỉnh: ống kính/aperture màu vàng champagne trên nền charcoal.

## Build
Mở thư mục NQV-Camera bằng Android Studio mới, hoặc dùng Gradle 8.13, JDK 17 và Android SDK Platform 36:
```bash
gradle assembleDebug
```
APK debug: app/build/outputs/apk/debug/app-debug.apk.

## Giới hạn của bản MVP
- Đây là các preset màu do dự án tự xây dựng, chỉ lấy cảm hứng phong cách; không phải profile, LUT hay thuật toán độc quyền của Leica, ZEISS, Huawei, Xiaomi, Kodak hoặc Fujifilm.
- Ứng dụng bên thứ ba không được tự động thừa hưởng toàn bộ ISP/HDR, night fusion, telephoto pipeline hay xử lý màu riêng của camera gốc Vivo.
- Chất lượng, ống kính, tỉ lệ khung hình và FPS phụ thuộc vào thiết bị/cấu hình CameraX. Chưa hứa hẹn 4K/120fps.
- Chưa có RAW/DNG, điều khiển ISO/màn trập thủ công, HDR fusion riêng hoặc chỉnh sửa hậu kỳ đầy đủ.
- AI hiện nhận diện vật thể và hướng dẫn bố cục; không tự xoay/di chuyển điện thoại.
- Cần kiểm thử trên máy thật để xác nhận khả năng tương thích, hiệu năng và tiêu thụ pin trước khi phát hành rộng rãi.

## Quyền riêng tư
AI Object Detection chạy trên thiết bị. Bản MVP không tải ảnh/video lên máy chủ.
