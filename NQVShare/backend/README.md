# NQV Share Worker · Cấu hình Cloudflare R2

APK cần một Worker đang hoạt động để tạo link và cấp quyền tải lên/tải xuống. Worker này ký URL ngắn hạn ở phía máy chủ; APK chỉ nhận URL tạm thời, **không chứa R2 Access Key hoặc Secret Key**.

## 1. Tạo bucket R2

1. Đăng nhập Cloudflare Dashboard.
2. Mở **R2 Object Storage** và tạo bucket tên `nqv-share-files` (hoặc đổi `R2_BUCKET` trong `wrangler.toml`).
3. Tạo R2 API token có quyền đọc/ghi object và liệt kê/xóa object trong bucket này.
4. Ghi lại Account ID, Access Key ID và Secret Access Key. Không gửi các khóa này cho người khác và không commit vào GitHub.

## 2. Cài và đăng nhập Wrangler

Cài Node.js LTS trên máy tính, mở PowerShell tại thư mục `NQVShare/backend`, rồi chạy:

```powershell
npm install
npx wrangler login
```

Nếu bucket chưa tồn tại, tạo bằng:

```powershell
npx wrangler r2 bucket create nqv-share-files
```

## 3. Thêm thông tin bí mật

Chạy lần lượt từng lệnh rồi dán giá trị tương ứng khi Wrangler yêu cầu:

```powershell
npx wrangler secret put R2_ACCOUNT_ID
npx wrangler secret put R2_ACCESS_KEY_ID
npx wrangler secret put R2_SECRET_ACCESS_KEY
```

Tên bucket đã khai báo trong `wrangler.toml`. Nếu đổi tên bucket, cập nhật `R2_BUCKET` trong file đó trước khi deploy.

## 4. Triển khai Worker

```powershell
npx wrangler deploy
```

Wrangler sẽ hiển thị URL dạng:

```
https://nqv-share-api.<ten-tai-khoan>.workers.dev
```

Mở URL đó thêm `/health`; kết quả đúng là JSON có `"ok": true`.

## 5. Kết nối APK

1. Mở NQV Share trên Vivo.
2. Dán URL Worker vừa nhận vào ô **Địa chỉ máy chủ Cloudflare Worker**.
3. Nhấn **Lưu địa chỉ máy chủ**.
4. Chọn tệp và nhấn **Tải lên và tạo link**.

Mỗi tệp có link riêng. Người nhận chỉ cần mở link trong Safari/Chrome và nhấn **Tải tệp gốc**. Mã QR trong APK trỏ tới đúng link đó.

## Cách lưu trữ và giới hạn hiện tại

- Tệp được tải trực tiếp từ điện thoại lên R2 bằng presigned PUT URL; nội dung không đi xuyên qua Worker.
- Mỗi lần tải lên đơn hỗ trợ tối đa **5 GiB**. Tệp lớn hơn cần nâng cấp sang multipart upload.
- Upload hiện chưa có khả năng tiếp tục sau khi mất mạng; cần tải lại từ đầu nếu bị gián đoạn.
- Link mặc định có hiệu lực 7 ngày. API cũng nhận 1 hoặc 30 ngày. Cron trong `wrangler.toml` quét và xóa object hết hạn mỗi giờ.
- Presigned URL tải lên chỉ có hiệu lực 15 phút; URL tải xuống do Worker tạo có hiệu lực 10 phút sau khi người nhận bấm nút.
- Dịch vụ sử dụng Cloudflare R2; phí lưu trữ, thao tác và truyền tải phụ thuộc gói/tài khoản Cloudflare.

## Lưu ý bảo mật

Bản đầu là MVP cá nhân, chưa có tài khoản người dùng, quota theo người dùng hoặc cơ chế chống lạm dụng hoàn chỉnh. Không nên công khai dịch vụ cho nhiều người dùng lạ trước khi thêm xác thực, giới hạn tốc độ và giới hạn dung lượng. Link chia sẻ là thông tin cần giữ riêng tư; bất kỳ ai có link đều có thể tải tệp cho tới khi hết hạn hoặc bị xóa.

---
© Bản quyền của Nông Quang Vinh
