# NQV AirShare 0.3.0

Ứng dụng Android gửi ảnh/video từ Vivo sang iPhone. iPhone không cần cài app: mở Camera để quét QR và dùng Safari/Files để nhận.

## Hai chế độ

### Wi-Fi trực tiếp
- Hai thiết bị kết nối cùng Wi-Fi hoặc iPhone kết nối hotspot do Vivo phát.
- Vivo tạo phiên chia sẻ nội bộ với token ngẫu nhiên 256-bit.
- Phiên tự hết hạn sau 30 phút.
- File được truyền trực tiếp từ Vivo, không qua Internet hoặc kho lưu trữ.
- NQV AirShare hiển thị lưu lượng đã gửi và tốc độ đọc dữ liệu ở phía Vivo.

Lưu ý: kết nối trực tiếp dùng HTTP trong mạng cục bộ để tránh cảnh báo chứng chỉ tự ký của Safari. Token ngẫu nhiên hạn chế người không có QR truy cập, nhưng đây chưa phải mã hóa đầu-cuối của mạng Wi-Fi. Chỉ dùng mạng riêng đáng tin cậy.

### Qua Internet
Dùng lại Cloudflare Worker + Backblaze B2 của dự án NQV Share:
- Trong app nhập URL Worker gốc, dạng https://ten-worker.workers.dev (không thêm /health).
- Worker cần có route POST /api/create-upload và các route chia sẻ /s/:id như trong NQVShare/backend.
- App xin URL PUT có chữ ký, rồi tải dữ liệu gốc trực tiếp lên bucket riêng tư qua HTTPS.
- iPhone nhận bằng trang chia sẻ Safari, không cần cài app.
- Mỗi file tối đa 5 GiB theo cấu hình Worker hiện tại.
- Phiên bản này yêu cầu link mặc định hết hạn sau 1 ngày (Worker phải giữ cấu hình create-upload cho phép expiresInDays=1).
- URL upload ký trước có hiệu lực 15 phút; link chia sẻ có thời hạn 1 ngày.

## Chất lượng
App không resize, nén hoặc chuyển mã ảnh/video. Dữ liệu truyền là byte gốc từ Uri của Android. Khả năng xem định dạng cụ thể trên iPhone phụ thuộc iOS; tải file vào Files giữ nguyên định dạng gốc.

## Lưu ý máy chủ
Mã Android không chứa khóa Backblaze. Cloudflare Worker cần được triển khai và cấu hình B2 secrets riêng theo hướng dẫn tại NQVShare/backend/README.md. Bản APK không tự triển khai Worker và không tự biết URL tài khoản của bạn.

## Build
GitHub Actions build bản release APK từ NQV-AirShare/app.


## Giao diện 0.3.0
- Nền aurora chuyển động nhẹ, thẻ kính nhiều lớp và đường phản chiếu.
- Header thương hiệu và thông tin bản quyền ở cả đầu/cuối màn hình.
- Phân cấp typography, nút bo tròn, segmented selector và hiệu ứng chuyển cảnh.
- Không thêm dữ liệu rác chỉ để làm APK lớn; ưu tiên hiệu năng và dung lượng hợp lý.
