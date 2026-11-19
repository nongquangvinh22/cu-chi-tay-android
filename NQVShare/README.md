# NQV Share V2

**NQV Share** là ứng dụng Android chia sẻ ảnh, video và tài liệu do Nông Quang Vinh phát triển. Ứng dụng tải tệp gốc lên Backblaze B2, sau đó tạo liên kết và mã QR để người nhận mở bằng trình duyệt trên iPhone, Android hoặc Windows.

## Tính năng

- Chọn một hoặc nhiều ảnh, video và tài liệu bằng trình chọn tệp của Android.
- Giữ nguyên dữ liệu tệp; không resize, nén hoặc chuyển mã.
- Chọn thời hạn liên kết: 1, 7 hoặc 30 ngày.
- Chọn cách chia sẻ: sao chép liên kết, hiển thị mã QR hoặc mở bảng chia sẻ Android để gửi qua ứng dụng khác.
- Kiểm tra trạng thái kết nối Worker ngay trong ứng dụng.
- Hiển thị tiến trình tải lên và thông báo lỗi cụ thể; một tệp lỗi không làm mất các tệp đã tải lên thành công.
- Trang tải tệp hoạt động trên Safari, Chrome và trình duyệt máy tính.

## Quan trọng: cần triển khai máy chủ

Mã nguồn GitHub không tự cung cấp dịch vụ lưu trữ trực tuyến. Trước khi chia sẻ tệp qua Internet, bạn cần có một bucket Backblaze B2 riêng tư và Cloudflare Worker đang chạy.

Đọc hướng dẫn đầy đủ tại [backend/README.md](backend/README.md). Tóm tắt:

1. Tạo bucket Backblaze B2 tên `nqv-share-files` tại region `us-east-005`.
2. Tạo Application Key giới hạn vào bucket và prefix `objects/`, có quyền đọc/ghi object.
3. Trong thư mục `NQVShare/backend`, chạy `npm install` rồi `npx wrangler login`.
4. Đặt hai Secret cho Worker: `B2_KEY_ID` và `B2_APPLICATION_KEY`.
5. Chạy `npx wrangler deploy`.
6. Mở URL Worker kèm `/health`. Kết quả cần có `"ok": true`.
7. Mở NQV Share, nhập URL Worker, nhấn **Lưu địa chỉ**, sau đó nhấn **Kiểm tra**.

Không gửi Application Key trong tin nhắn, ảnh chụp màn hình hoặc commit GitHub.

## Build APK

### Android Studio

- Mở thư mục `NQVShare`.
- Dùng JDK 17 và Android SDK Platform 35.
- Chạy tác vụ Gradle `assembleDebug`, hoặc chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
- APK debug nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions

Workflow `Build NQV Share APK` tự chạy khi có thay đổi trong mã nguồn NQV Share trên nhánh `main`, hoặc có thể chạy thủ công từ tab **Actions**. Workflow tạo APK, lưu bản build tại `NQVShare/dist/NQVShare.apk` và xuất GitHub prerelease.

## Giới hạn hiện tại

- Một lần PUT hỗ trợ tối đa 5 GiB cho mỗi tệp.
- Tải lên đang dùng một PUT đơn; nếu mất mạng giữa chừng, cần tải lại tệp đó.
- Chưa có tài khoản người dùng, mật khẩu cho link hoặc tải nhiều tệp dưới dạng ZIP.
- Đây là bản thử nghiệm cá nhân. Trước khi mở công khai cho nhiều người dùng, nên bổ sung xác thực, giới hạn tốc độ và hạn mức dung lượng.

---

**Bản quyền Nông Quang Vinh**
