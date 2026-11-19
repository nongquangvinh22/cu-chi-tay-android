# NQV Share

**NQV Share** is an Android file-sharing app by Nông Quang Vinh. Pick photos, videos, or documents on Android, upload the original bytes directly to Cloudflare R2 through a short-lived signed URL, then share a link or QR code. Recipients can open the link in Safari, Chrome, or a desktop browser; they do not need to install the APK.

## What this first version includes

- Select multiple files using Android's system file picker.
- Upload each file as-is; the app does not resize, recompress, or transcode it.
- Upload directly to a private R2 bucket using a short-lived presigned PUT URL.
- Generate one share link and QR code per uploaded file.
- Copy or share links with other apps.
- Download from a simple web page that works on iPhone and Windows.
- Share links expire after 7 days by default; the Worker also supports 1- and 30-day options.
- GitHub Actions builds a debug APK and publishes it as a GitHub prerelease.

## Important

This repository contains the app source and Cloudflare Worker source. **The online transfer service is not live until you deploy the Worker and configure your own Cloudflare R2 bucket and credentials.** The Android app asks for the Worker URL in Settings. Never put R2 secret keys in the Android app.

This initial version uses a single R2 PUT per file. It supports files up to 5 GiB. Very large uploads are not resumable yet, so an interrupted upload must be restarted. Password-protected links, multi-file ZIP bundles, and account sign-in are not included in v0.1.

## Build the APK

1. Open the `NQVShare` folder in Android Studio.
2. Use JDK 17 and Android SDK Platform 35.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Or push changes to GitHub and open **Actions > Build NQV Share APK**. The workflow uploads an artifact and publishes a prerelease containing `NQVShare.apk`.

## Configure the online service

Follow [backend/README.md](backend/README.md). After deployment, copy your Worker URL (for example `https://nqv-share-api.<your-subdomain>.workers.dev`) into the app's **Địa chỉ máy chủ** field and tap **Lưu**.

## Use

1. Open NQV Share on Android and save the Worker URL.
2. Tap **Chọn tệp**, select one or more files, then tap **Tải lên và tạo link**.
3. Wait for each upload to finish. A separate share link is created for each file.
4. Copy a link or show its QR code.
5. On iPhone, open the link in Safari and tap **Tải tệp gốc**.

NQV Share does not alter the original file. The actual downloaded file can still be affected by browser/iOS save-location behavior, but the bytes delivered by the service are the uploaded bytes.

---
© Bản quyền của Nông Quang Vinh
