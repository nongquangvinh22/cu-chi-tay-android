# NQV Share Worker - Backblaze B2

Worker tạo URL tải lên có chữ ký ở phía máy chủ. Ứng dụng Android chỉ nhận URL tạm thời; không chứa thông tin xác thực Backblaze.

## 1. Cấu hình Backblaze B2

Bucket đã dùng cho dự án:

- Bucket: `nqv-share-files`
- Region: `us-east-005`
- S3 endpoint: `https://s3.us-east-005.backblazeb2.com`
- Bucket access: Private

Tạo Application Key riêng cho Worker, giới hạn trong bucket `nqv-share-files`, quyền Read and Write và file name prefix `objects/`.

**Quyền List All Bucket Names không cần thiết**: Worker chỉ gọi API liệt kê object trong bucket đã cấu hình, không gọi S3 List Buckets. Hãy bỏ quyền này để giữ khóa ở mức tối thiểu. Nếu khóa hiện tại đã được cấp quyền này, nên thu hồi và tạo khóa khác không bật mục đó.

## 2. Cài Wrangler

Mở PowerShell tại thư mục `NQVShare/backend`:

```powershell
npm install
npx wrangler login
```

## 3. Thêm secrets cho Worker

Chạy từng lệnh trong PowerShell và nhập giá trị tương ứng từ Application Key vừa tạo:

```powershell
npx wrangler secret put B2_KEY_ID
npx wrangler secret put B2_APPLICATION_KEY
```

Hoặc trong Cloudflare Dashboard, mở Worker `nqv-share-api` → **Settings → Variables and Secrets** và thêm cả hai biến dưới dạng **Secret**:

- `B2_KEY_ID`
- `B2_APPLICATION_KEY`

Các biến không bí mật đã được khai báo trong `wrangler.toml`:

- `B2_BUCKET = nqv-share-files`
- `B2_ENDPOINT = https://s3.us-east-005.backblazeb2.com`
- `B2_REGION = us-east-005`

Không đưa Application Key vào mã nguồn, file cấu hình công khai, APK hoặc GitHub.

## 4. Deploy

Từ thư mục `NQVShare/backend`, chạy:

```powershell
npx wrangler deploy
```

Wrangler sẽ hiển thị URL dạng:

`https://nqv-share-api.<ten-tai-khoan>.workers.dev`

Mở URL đó thêm `/health`. Kết quả mong đợi có `"ok": true`.

## 5. Kết nối ứng dụng

1. Mở NQV Share trên Android.
2. Dán URL Worker vào ô **Địa chỉ máy chủ Cloudflare Worker**.
3. Nhấn **Lưu địa chỉ**, sau đó **Kiểm tra**.
4. Chọn tệp và nhấn **TẢI LÊN VÀ TẠO LINK**.

## Cách hoạt động và giới hạn

- Tệp được gửi trực tiếp từ điện thoại lên Backblaze B2 bằng presigned PUT URL; Worker không nhận toàn bộ nội dung tệp.
- Giới hạn mỗi tệp là 5 GiB cho một lần PUT.
- URL tải lên có hiệu lực 15 phút. Link chia sẻ có thời hạn 1, 7 hoặc 30 ngày.
- URL tải xuống được ký trong 10 phút khi người nhận bấm nút tải.
- Worker chạy tác vụ dọn tệp hết hạn mỗi giờ. Tác vụ này liệt kê và xóa các phiên bản của object, không chỉ tạo delete marker.
- Nếu upload bị gián đoạn, bản hiện tại cần tải lại từ đầu.

## An toàn và chi phí

- Giữ bucket ở chế độ Private.
- Link chia sẻ là thông tin riêng tư: ai có link đều có thể tải tệp trong thời hạn hiệu lực.
- Theo dõi mức sử dụng và giới hạn miễn phí của Backblaze B2; vượt hạn mức có thể phát sinh phí.
- Đây là bản thử nghiệm cá nhân, chưa có đăng nhập, quota theo người dùng hoặc chống lạm dụng đầy đủ. Không nên công khai dịch vụ cho nhiều người dùng trước khi bổ sung kiểm soát phù hợp.

---

**Bản quyền Nông Quang Vinh**
