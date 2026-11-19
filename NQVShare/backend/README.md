# NQV Share Worker - Cấu hình Cloudflare R2

Worker tạo URL tải lên có chữ ký ở phía máy chủ. Ứng dụng Android chỉ nhận URL tạm thời và không chứa thông tin xác thực R2.

## 1. Tạo bucket và token

1. Mở Cloudflare Dashboard → R2 Object Storage.
2. Tạo bucket tên `nqv-share-files` (hoặc sửa giá trị `R2_BUCKET` trong `wrangler.toml`).
3. Tạo R2 API token chỉ cấp quyền cần thiết cho bucket này: đọc, ghi và xóa object.
4. Lưu Account ID, Access Key ID và Secret Access Key ở nơi riêng tư.

## 2. Cài Wrangler

Mở PowerShell tại thư mục `NQVShare/backend`:

```powershell
npm install
npx wrangler login
```

## 3. Cấu hình secret

Chạy từng lệnh và nhập giá trị tương ứng:

```powershell
npx wrangler secret put R2_ACCOUNT_ID
npx wrangler secret put R2_ACCESS_KEY_ID
npx wrangler secret put R2_SECRET_ACCESS_KEY
```

Không đưa các giá trị này vào mã nguồn, file cấu hình công khai hoặc APK.

## 4. Deploy

```powershell
npx wrangler deploy
```

Wrangler sẽ hiển thị địa chỉ dạng:

`https://nqv-share-api.<ten-tai-khoan>.workers.dev`

Mở địa chỉ đó thêm `/health`. Kết quả thành công có `"ok": true`.

## 5. Kết nối ứng dụng

1. Mở NQV Share trên Android.
2. Dán URL Worker vào ô **Địa chỉ máy chủ Cloudflare Worker**.
3. Nhấn **Lưu địa chỉ**, sau đó **Kiểm tra**.
4. Chọn tệp và nhấn **TẢI LÊN VÀ TẠO LINK**.

## Cách hoạt động và giới hạn

- Tệp được gửi trực tiếp từ điện thoại lên R2 bằng presigned PUT URL; Worker không nhận toàn bộ nội dung tệp.
- Giới hạn mỗi tệp là 5 GiB cho một lần PUT.
- URL tải lên có hiệu lực 15 phút. Link chia sẻ mặc định 7 ngày; app hỗ trợ 1, 7 hoặc 30 ngày.
- URL tải xuống ký tạm thời trong 10 phút khi người nhận bấm nút tải.
- Worker chạy tác vụ dọn object hết hạn mỗi giờ.
- Nếu upload bị gián đoạn, bản hiện tại cần tải lại từ đầu.

## An toàn

- Giữ bucket ở chế độ riêng tư; không bật public access nếu không có nhu cầu rõ ràng.
- Link chia sẻ là thông tin riêng tư: ai có link đều có thể tải tệp trong thời hạn hiệu lực.
- Đây là bản thử nghiệm cá nhân, chưa có đăng nhập, quota theo người dùng hoặc chống lạm dụng đầy đủ. Không nên công khai dịch vụ cho người lạ trước khi bổ sung kiểm soát phù hợp.

---

**Bản quyền Nông Quang Vinh**
