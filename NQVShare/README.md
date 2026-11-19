# NQV Share V2

**NQV Share** là ứng dụng Android chia sẻ ảnh, video và tài liệu do Nông Quang Vinh phát triển. Ứng dụng tải tệp gốc lên Cloudflare R2, sau đó tạo liên kết và mã QR để người nhận mở bằng trình duyệt trên iPhone, Android hoặc Windows.

## Tính năng

- Chọn một hoặc nhiều ảnh, video và tài liệu bằng trình chọn tệp của Android.
- Giữ nguyên dữ liệu tệp; không resize, nén hoặc chuyển mã.
- Chọn thời hạn liên kết: 1, 7 hoặc 30 ngày.
- Chọn cách chia sẻ: sao chép liên kết, hiển thị mã QR hoặc mở bảng chia sẻ Android để gửi qua ứng dụng khác.
- Kiểm tra trạng thái kết nối Cloudflare Worker ngay trong ứng dụng.
- Hiển thị tiến trình tải lên và thông báo lỗi cụ thể; một tệp lỗi không làm mất các tệp đã tải lên thành công.
- Hỗ trợ cả tệp mà Android không cung cấp trước dung lượng bằng cách tạo bản tạm trong bộ nhớ cache để xác định kích thước.
- Trang tải tệp hoạt động trên Safari, Chrome và trình duyệt máy tính.
- Giao diện nhiều màu sắc, có dòng bản quyền Nông Quang Vinh.

## Quan trọng: cần triển khai máy chủ

Mã nguồn GitHub không tự cung cấp dịch vụ lưu trữ trực tuyến. Trước khi chia sẻ tệp qua Internet, bạn cần có Cloudflare Worker đang chạy và một bucket Cloudflare R2 đã cấu hình. APK chỉ chứa địa chỉ Worker; **không đưa R2 Access Key hoặc Secret Key vào ứng dụng hoặc GitHub**.

### Thiết lập Cloudflare R2 và Worker

Đọc hướng dẫn đầy đủ tại [backend/README.md](backend/README.md). Tóm tắt:

1. Tạo bucket R2 tên `nqv-share-files`.
2. Tạo R2 API token có quyền đọc/ghi object cho bucket.
3. Trong thư mục `NQVShare/backend`, chạy `npm install` rồi `npx wrangler login`.
4. Đặt ba secret bằng Wrangler:
   - `npx wrangler secret put R2_ACCOUNT_ID`
   - `npx wrangler secret put R2_ACCESS_KEY_ID`
   - `npx wrangler secret put R2_SECRET_ACCESS_KEY`
5. Chạy `npx wrangler deploy`.
6. Mở URL Worker kèm `/health`. Kết quả cần có `"ok": true`.
7. Mở NQV Share, nhập URL Worker, nhấn **Lưu địa chỉ**, sau đó nhấn **Kiểm tra**.

Không gửi khóa Cloudflare trong tin nhắn, ảnh chụp màn hình hoặc commit GitHub. Nếu từng lỡ công khai khóa, hãy thu hồi và tạo token mới.

## Cách sử dụng

1. Kết nối Worker bằng URL HTTPS.
2. Chọn tệp muốn gửi.
3. Chọn thời hạn link 1, 7 hoặc 30 ngày.
4. Nhấn **TẢI LÊN VÀ TẠO LINK**.
5. Ở mục **Chọn cách chia sẻ**, chọn Liên kết, Mã QR hoặc Ứng dụng khác.
6. Người nhận mở liên kết bằng Safari/Chrome rồi nhấn **Tải tệp gốc**.

Mỗi tệp có một liên kết riêng. Bất kỳ ai có liên kết đều có thể tải tệp cho đến khi liên kết hết hạn hoặc tệp bị xóa.

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
