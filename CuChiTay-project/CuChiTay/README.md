# Cử Chỉ Tay – điều khiển điện thoại bằng cử chỉ tay qua camera trước

Ứng dụng Android (Kotlin) dạng dịch vụ Trợ năng:

- **Hất tay lên / xuống** (khi đang xòe bàn tay) → vuốt màn hình để cuộn, giống lướt TikTok.
- **Xòe 5 ngón rồi nắm lại** → chụp màn hình.
- Tùy chọn: hất sang trái/phải, đảo chiều lên/xuống, chỉnh độ nhạy.

Nhận diện bàn tay bằng Google MediaPipe Hand Landmarker, chạy hoàn toàn trên máy.
Ảnh camera chỉ xử lý trong bộ nhớ, không lưu và không gửi đi.

---

## Cách A (khuyên dùng, không cần cài phần mềm): để GitHub tự build ra APK

1. Tạo tài khoản miễn phí tại https://github.com, chọn **New repository** (đặt tên bất kỳ, để Private cũng được).
2. Giải nén tệp zip này trên máy tính. Trong repository vừa tạo, chọn **uploading an existing file**,
   kéo thả TOÀN BỘ nội dung thư mục dự án vào (bao gồm cả thư mục ẩn `.github`), rồi bấm **Commit changes**.
   - Nếu thư mục `.github` không lên: chọn **Add file → Create new file**, gõ tên
     `.github/workflows/build.yml` (gõ dấu `/` để tạo thư mục), dán nội dung tệp `build.yml` vào và commit.
3. Mở tab **Actions** → chọn **Build APK** → **Run workflow** (nếu nó chưa tự chạy). Đợi khoảng 5–10 phút đến khi có dấu ✓ xanh.
4. Bấm vào lần chạy đó → kéo xuống mục **Artifacts** → tải **CuChiTay-apk** → giải nén ra được `app-debug.apk`.
5. Chuyển `app-debug.apk` sang điện thoại (Zalo, cáp USB, Google Drive…) rồi cài.

## Cách B: dùng Android Studio

1. Mở thư mục dự án bằng Android Studio (bản mới nhất), đợi Gradle đồng bộ.
2. Tải mô hình về đặt vào `app/src/main/assets/hand_landmarker.task` (xem `app/src/main/assets/README.txt`).
3. Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**. APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

---

## Cài và thiết lập trên vivo X200 Pro

1. Mở tệp APK → cho phép **cài đặt từ nguồn này**. Nếu vivo hiện cảnh báo quét an toàn, chọn **Vẫn cài đặt**
   (đây là bản debug tự build, không qua cửa hàng nên máy sẽ cảnh báo).
2. Mở app **Cử Chỉ Tay** → bấm cấp quyền camera và thông báo.
3. Bật Trợ năng: **Cài đặt → Trợ năng → Dịch vụ đã tải xuống (hoặc Ứng dụng đã cài đặt) → Cử Chỉ Tay → Bật**.
   - Nếu công tắc bị mờ vì "Cài đặt bị hạn chế": **Cài đặt → Ứng dụng → Cử Chỉ Tay → dấu ⋮ góc trên phải →
     Cho phép cài đặt bị hạn chế**, rồi quay lại bật.
4. Giữ app sống ở chế độ nền: **Cài đặt → Pin → Mức tiêu thụ pin nền** (tên có thể khác theo bản OriginOS)
   → chọn **Không hạn chế** cho Cử Chỉ Tay; khóa app trong màn hình đa nhiệm (kéo thẻ app xuống hoặc bấm biểu tượng khóa).
5. Quay lại app, bấm **BẬT điều khiển bằng cử chỉ**. Chuyển sang TikTok và thử.

## Mẹo dùng

- Cầm máy thẳng đứng, camera trước thấy rõ bàn tay, cách khoảng 25–50 cm, đủ sáng.
- Hất tay **nhanh**; sau đó thả tay chậm lại hoặc rút ra khỏi khung hình để cú thu tay về không bị hiểu thành hất ngược.
- Cuộn bị nhầm khi cử động tay bình thường → kéo độ nhạy xuống. Hất mà không ăn → kéo độ nhạy lên.
- Nếu hướng cuộn ngược ý bạn → bật "Đảo chiều lên/xuống".

## Giới hạn cần biết

- Sau khi khởi động lại máy phải mở app và bấm BẬT lại (Android không cho dịch vụ camera tự chạy từ nền).
- Khi app khác đang dùng camera trước (gọi video, quay…) cử chỉ sẽ tạm không hoạt động.
- Chạy camera liên tục tốn pin; nên tắt khi không dùng.
- Một số app (ngân hàng, ví điện tử) chặn thao tác từ dịch vụ Trợ năng hoặc chặn chụp màn hình; đó là giới hạn của các app đó.
- Dự án chưa được thử trên máy thật; ngưỡng cử chỉ có thể cần chỉnh thêm theo tay bạn.
