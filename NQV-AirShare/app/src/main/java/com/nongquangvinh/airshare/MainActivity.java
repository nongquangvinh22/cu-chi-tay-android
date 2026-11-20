package com.nongquangvinh.airshare;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    private static final int PICK_FILES_REQUEST = 4102;
    private static final int SERVER_PORT = 8765;

    private final ArrayList<SharedFile> selectedFiles = new ArrayList<>();
    private LinearLayout fileList;
    private TextView selectedCount;
    private TextView statusText;
    private TextView addressText;
    private ImageView qrImage;
    private Button startButton;
    private Button stopButton;
    private ShareServer shareServer;
    private String shareUrl = "";
    private String sessionToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildScreen();
        refreshFileList();
        updateStatus("Sẵn sàng. Chọn ảnh hoặc video để bắt đầu.");
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(8, 15, 29));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(28));
        scroll.addView(page);

        TextView brand = text("NQV  /  AIRSHARE", 13, Color.rgb(135, 188, 255), true);
        page.addView(brand);

        TextView title = text("Chia sẻ không chạm", 28, Color.WHITE, true);
        LinearLayout.LayoutParams titleLp = params(-1, -2);
        titleLp.topMargin = dp(9);
        page.addView(title, titleLp);

        TextView subtitle = text(
                "Gửi ảnh và video từ Vivo sang iPhone. iPhone chỉ cần Camera và Safari.",
                14, Color.rgb(184, 197, 216), false);
        LinearLayout.LayoutParams subLp = params(-1, -2);
        subLp.topMargin = dp(7);
        page.addView(subtitle, subLp);

        LinearLayout howCard = card();
        LinearLayout.LayoutParams cardLp = params(-1, -2);
        cardLp.topMargin = dp(22);
        page.addView(howCard, cardLp);

        TextView howTitle = text("CÁCH KẾT NỐI", 12, Color.rgb(135, 188, 255), true);
        howCard.addView(howTitle);
        TextView howText = text(
                "1. Bật điểm phát sóng trên Vivo, hoặc cho cả hai máy vào cùng Wi-Fi.\n" +
                "2. Kết nối iPhone vào cùng mạng Wi-Fi/điểm phát sóng đó.\n" +
                "3. Chọn tệp và tạo mã QR ở bên dưới.\n" +
                "4. Dùng Camera iPhone quét mã, mở liên kết trong Safari để xem hoặc tải tệp.",
                14, Color.rgb(226, 233, 244), false);
        LinearLayout.LayoutParams howTextLp = params(-1, -2);
        howTextLp.topMargin = dp(10);
        howCard.addView(howText, howTextLp);

        Button wifiButton = button("Mở cài đặt Wi-Fi / điểm phát sóng", false);
        LinearLayout.LayoutParams wifiLp = params(-1, -2);
        wifiLp.topMargin = dp(12);
        howCard.addView(wifiButton, wifiLp);
        wifiButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        Button pickButton = button("＋   Chọn ảnh và video", true);
        LinearLayout.LayoutParams pickLp = params(-1, dp(54));
        pickLp.topMargin = dp(22);
        page.addView(pickButton, pickLp);
        pickButton.setOnClickListener(v -> openPicker());

        selectedCount = text("Chưa chọn tệp", 14, Color.WHITE, true);
        LinearLayout.LayoutParams countLp = params(-1, -2);
        countLp.topMargin = dp(17);
        page.addView(selectedCount, countLp);

        fileList = new LinearLayout(this);
        fileList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = params(-1, -2);
        listLp.topMargin = dp(8);
        page.addView(fileList, listLp);

        startButton = button("TẠO MÃ QR ĐỂ NHẬN TRÊN IPHONE", true);
        LinearLayout.LayoutParams startLp = params(-1, dp(54));
        startLp.topMargin = dp(20);
        page.addView(startButton, startLp);
        startButton.setOnClickListener(v -> startSharing());

        LinearLayout qrCard = card();
        LinearLayout.LayoutParams qrCardLp = params(-1, -2);
        qrCardLp.topMargin = dp(16);
        page.addView(qrCard, qrCardLp);

        TextView qrHeading = text("QUÉT MÃ BẰNG CAMERA IPHONE", 12,
                Color.rgb(135, 188, 255), true);
        qrCard.addView(qrHeading);

        qrImage = new ImageView(this);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImage.setBackgroundColor(Color.WHITE);
        qrImage.setPadding(dp(10), dp(10), dp(10), dp(10));
        qrImage.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(dp(240), dp(240));
        qrLp.gravity = Gravity.CENTER_HORIZONTAL;
        qrLp.topMargin = dp(14);
        qrCard.addView(qrImage, qrLp);

        addressText = text("", 13, Color.WHITE, false);
        addressText.setTextIsSelectable(true);
        addressText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams addressLp = params(-1, -2);
        addressLp.topMargin = dp(12);
        qrCard.addView(addressText, addressLp);

        Button copyButton = button("SAO CHÉP ĐƯỜNG DẪN", false);
        LinearLayout.LayoutParams copyLp = params(-1, dp(46));
        copyLp.topMargin = dp(10);
        qrCard.addView(copyButton, copyLp);
        copyButton.setOnClickListener(v -> copyAddress());

        stopButton = button("DỪNG CHIA SẺ", false);
        LinearLayout.LayoutParams stopLp = params(-1, dp(46));
        stopLp.topMargin = dp(10);
        qrCard.addView(stopButton, stopLp);
        stopButton.setOnClickListener(v -> stopSharing(true));
        stopButton.setEnabled(false);

        Button nativeShare = button("THỬ CHIA SẺ BẰNG ỨNG DỤNG VIVO / ANDROID", false);
        LinearLayout.LayoutParams nativeLp = params(-1, dp(48));
        nativeLp.topMargin = dp(18);
        page.addView(nativeShare, nativeLp);
        nativeShare.setOnClickListener(v -> shareWithInstalledApps());

        LinearLayout statusCard = card();
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(16);
        page.addView(statusCard, statusLp);
        statusText = text("Sẵn sàng", 14, Color.rgb(190, 210, 235), false);
        statusCard.addView(statusText);

        TextView footer = text("© Nông Quang Vinh  •  NQV AirShare 0.1.0", 11,
                Color.rgb(115, 132, 155), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = params(-1, -2);
        footerLp.topMargin = dp(24);
        page.addView(footer, footerLp);

        setContentView(scroll);
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_FILES_REQUEST);
        } catch (Exception e) {
            updateStatus("Không mở được trình chọn tệp: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILES_REQUEST || resultCode != RESULT_OK || data == null) return;

        ArrayList<Uri> incoming = new ArrayList<>();
        if (data.getClipData() != null) {
            ClipData clip = data.getClipData();
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) incoming.add(uri);
            }
        } else if (data.getData() != null) {
            incoming.add(data.getData());
        }

        int added = 0;
        Set<String> existing = new HashSet<>();
        for (SharedFile file : selectedFiles) existing.add(file.uri.toString());

        for (Uri uri : incoming) {
            if (existing.contains(uri.toString())) continue;
            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // Some document providers do not offer persistable permissions.
            }

            String mime = getContentResolver().getType(uri);
            if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/"))) {
                continue;
            }
            selectedFiles.add(new SharedFile(uri, queryName(uri), mime, querySize(uri)));
            existing.add(uri.toString());
            added++;
        }

        refreshFileList();
        updateStatus(added > 0
                ? "Đã thêm " + added + " tệp. Có thể tạo mã QR để chia sẻ."
                : "Không có ảnh hoặc video mới được chọn.");
    }

    private void refreshFileList() {
        if (fileList == null || selectedCount == null) return;
        fileList.removeAllViews();

        selectedCount.setText(selectedFiles.size() == 0
                ? "Chưa chọn tệp"
                : "ĐÃ CHỌN " + selectedFiles.size() + " TỆP");

        for (int i = 0; i < selectedFiles.size(); i++) {
            final int index = i;
            SharedFile file = selectedFiles.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(19, 31, 50));
            bg.setCornerRadius(dp(12));
            row.setBackground(bg);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(file.name, 13, Color.WHITE, true);
            name.setMaxLines(2);
            TextView meta = text(file.mimeType + "  •  " + formatBytes(file.size),
                    11, Color.rgb(153, 172, 199), false);
            info.addView(name);
            info.addView(meta);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

            Button remove = button("×", false);
            LinearLayout.LayoutParams removeLp = params(dp(42), dp(40));
            removeLp.leftMargin = dp(8);
            row.addView(remove, removeLp);
            remove.setOnClickListener(v -> {
                selectedFiles.remove(index);
                stopSharing(false);
                refreshFileList();
                updateStatus("Đã cập nhật danh sách tệp.");
            });

            LinearLayout.LayoutParams rowLp = params(-1, -2);
            rowLp.bottomMargin = dp(7);
            fileList.addView(row, rowLp);
        }

        if (startButton != null) startButton.setEnabled(!selectedFiles.isEmpty());
    }

    private void startSharing() {
        if (selectedFiles.isEmpty()) {
            updateStatus("Hãy chọn ít nhất một ảnh hoặc video trước.");
            return;
        }

        stopSharing(false);
        String ip = findLocalIpv4();
        if (ip == null) {
            updateStatus("Chưa tìm thấy địa chỉ Wi-Fi. Hãy bật điểm phát sóng Vivo hoặc kết nối Wi-Fi rồi thử lại.");
            return;
        }

        sessionToken = createToken();
        shareUrl = "http://" + ip + ":" + SERVER_PORT + "/" + sessionToken + "/";
        try {
            shareServer = new ShareServer(SERVER_PORT, sessionToken,
                    new ArrayList<>(selectedFiles));
            shareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            Bitmap qr = createQr(shareUrl);
            qrImage.setImageBitmap(qr);
            qrImage.setVisibility(View.VISIBLE);
            addressText.setText(shareUrl);
            stopButton.setEnabled(true);
            updateStatus("Đang chia sẻ an toàn trong mạng nội bộ. Giữ NQV AirShare mở trong lúc nhận tệp.");
        } catch (Exception e) {
            shareServer = null;
            qrImage.setVisibility(View.GONE);
            addressText.setText("");
            updateStatus("Không thể mở máy chủ truyền tệp: " + e.getMessage());
        }
    }

    private void stopSharing(boolean showMessage) {
        if (shareServer != null) {
            try {
                shareServer.stop();
            } catch (Exception ignored) {
            }
            shareServer = null;
        }
        shareUrl = "";
        sessionToken = "";
        if (qrImage != null) qrImage.setVisibility(View.GONE);
        if (addressText != null) addressText.setText("");
        if (stopButton != null) stopButton.setEnabled(false);
        if (showMessage) updateStatus("Đã dừng chia sẻ. Mã QR cũ không còn hiệu lực.");
    }

    private void copyAddress() {
        if (shareUrl.isEmpty()) {
            updateStatus("Hãy tạo mã QR trước khi sao chép liên kết.");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("NQV AirShare", shareUrl));
        Toast.makeText(this, "Đã sao chép liên kết", Toast.LENGTH_SHORT).show();
    }

    private void shareWithInstalledApps() {
        if (selectedFiles.isEmpty()) {
            updateStatus("Hãy chọn ảnh hoặc video trước.");
            return;
        }

        try {
            ArrayList<Uri> uris = new ArrayList<>();
            for (SharedFile file : selectedFiles) uris.add(file.uri);

            Intent send;
            if (uris.size() == 1) {
                send = new Intent(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                send.setType(selectedFiles.get(0).mimeType);
                send.setClipData(ClipData.newUri(getContentResolver(), "NQV AirShare", uris.get(0)));
            } else {
                send = new Intent(Intent.ACTION_SEND_MULTIPLE);
                send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                send.setType("*/*");
                ClipData clip = ClipData.newUri(getContentResolver(), "NQV AirShare", uris.get(0));
                for (int i = 1; i < uris.size(); i++) {
                    clip.addItem(new ClipData.Item(uris.get(i)));
                }
                send.setClipData(clip);
            }
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Chia sẻ bằng ứng dụng tương thích"));
        } catch (Exception e) {
            updateStatus("Không mở được bảng chia sẻ: " + e.getMessage());
        }
    }

    private String queryName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) {
        }
        return "NQV-File";
    }

    private long querySize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (column >= 0 && !cursor.isNull(column)) return cursor.getLong(column);
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }

    private String findLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<String[]> candidates = new ArrayList<>();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                String name = networkInterface.getName().toLowerCase(Locale.ROOT);
                if (name.contains("tun") || name.contains("vpn") || name.contains("rmnet")
                        || name.contains("ccmni") || name.contains("ppp") || name.equals("lo")) {
                    continue;
                }
                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()
                            || address.isLinkLocalAddress() || !address.isSiteLocalAddress()) {
                        continue;
                    }
                    int priority = name.contains("ap") || name.contains("swlan") ? 0
                            : (name.equals("wlan0") || name.startsWith("wlan")) ? 1 : 2;
                    candidates.add(new String[]{String.valueOf(priority), address.getHostAddress()});
                }
            }
            Collections.sort(candidates, Comparator.comparingInt(a -> Integer.parseInt(a[0])));
            if (!candidates.isEmpty()) return candidates.get(0)[1];
        } catch (Exception ignored) {
        }
        return null;
    }

    private String createToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) builder.append(String.format(Locale.ROOT, "%02x", b));
        return builder.toString();
    }

    private Bitmap createQr(String value) throws Exception {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new MultiFormatWriter().encode(
                value, BarcodeFormat.QR_CODE, 720, 720, hints);
        Bitmap bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < 720; x++) {
            for (int y = 0; y < 720; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private void updateStatus(String message) {
        if (statusText != null) statusText.setText(message);
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(15, 26, 43));
        bg.setCornerRadius(dp(18));
        layout.setBackground(bg);
        return layout;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.rgb(8, 20, 37) : Color.rgb(220, 233, 251));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? Color.rgb(137, 194, 255) : Color.rgb(32, 49, 73));
        bg.setCornerRadius(dp(13));
        button.setBackground(bg);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        return button;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Dung lượng chưa xác định";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L)
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String htmlEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    protected void onDestroy() {
        stopSharing(false);
        super.onDestroy();
    }

    private static class SharedFile {
        final Uri uri;
        final String name;
        final String mimeType;
        final long size;

        SharedFile(Uri uri, String name, String mimeType, long size) {
            this.uri = uri;
            this.name = name;
            this.mimeType = mimeType;
            this.size = size;
        }
    }

    private class ShareServer extends NanoHTTPD {
        private final String token;
        private final ArrayList<SharedFile> files;

        ShareServer(int port, String token, ArrayList<SharedFile> files) {
            super(port);
            this.token = token;
            this.files = files;
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (session.getMethod() != Method.GET && session.getMethod() != Method.HEAD) {
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                        "text/plain; charset=utf-8", "Method not allowed");
            }

            String path = session.getUri();
            String base = "/" + token;
            if (path.equals(base) || path.equals(base + "/")) {
                return pageResponse();
            }

            String filePrefix = base + "/file/";
            if (path.startsWith(filePrefix)) {
                String rawIndex = path.substring(filePrefix.length());
                int slash = rawIndex.indexOf('/');
                if (slash >= 0) rawIndex = rawIndex.substring(0, slash);
                try {
                    int index = Integer.parseInt(rawIndex);
                    if (index >= 0 && index < files.size()) {
                        boolean download = session.getParms().containsKey("download");
                        return fileResponse(files.get(index), download);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "text/plain; charset=utf-8", "Liên kết không tồn tại hoặc phiên chia sẻ đã kết thúc.");
        }

        private Response pageResponse() {
            StringBuilder html = new StringBuilder();
            html.append("<!doctype html><html lang='vi'><head><meta charset='utf-8'>")
                    .append("<meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'>")
                    .append("<meta name='color-scheme' content='light'>")
                    .append("<title>NQV AirShare</title><style>")
                    .append("*{box-sizing:border-box}body{margin:0;background:#f3f6fb;color:#142033;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}")
                    .append(".wrap{max-width:720px;margin:auto;padding:24px 16px 48px}.hero{background:#10213a;color:white;border-radius:22px;padding:24px}")
                    .append(".tag{color:#9bc9ff;font-size:12px;font-weight:700;letter-spacing:2px}.hero h1{font-size:27px;margin:10px 0}.hero p{color:#d3deed;line-height:1.55;margin:0}")
                    .append(".item{background:white;border:1px solid #e0e7f0;border-radius:17px;padding:15px;margin-top:13px;box-shadow:0 5px 18px rgba(20,40,70,.05)}")
                    .append(".name{font-weight:700;overflow-wrap:anywhere}.meta{font-size:12px;color:#63748a;margin-top:6px}.preview{display:block;width:100%;max-height:360px;object-fit:contain;background:#eef2f7;border-radius:12px;margin-top:12px}")
                    .append(".actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:13px}.btn{display:inline-block;padding:11px 15px;border-radius:11px;text-decoration:none;background:#1769c2;color:white;font-weight:650;font-size:13px}.secondary{background:#e9f1fb;color:#174b82}")
                    .append(".note{background:#eaf2fc;border-radius:15px;padding:15px;margin-top:18px;line-height:1.55;font-size:13px;color:#29415e}.foot{text-align:center;color:#718096;font-size:12px;margin-top:24px}")
                    .append("</style></head><body><main class='wrap'><section class='hero'><div class='tag'>NQV  /  AIRSHARE</div>")
                    .append("<h1>Tệp đã sẵn sàng</h1><p>Được gửi trực tiếp từ thiết bị Vivo trong mạng nội bộ. Chọn tệp bên dưới để xem hoặc lưu vào iPhone.</p></section>");

            for (int i = 0; i < files.size(); i++) {
                SharedFile file = files.get(i);
                String fileUrl = "/" + token + "/file/" + i;
                html.append("<section class='item'><div class='name'>")
                        .append(htmlEscape(file.name)).append("</div><div class='meta'>")
                        .append(htmlEscape(file.mimeType)).append(" · ")
                        .append(htmlEscape(formatBytes(file.size))).append("</div>");

                if (file.mimeType.startsWith("image/")) {
                    html.append("<img class='preview' loading='lazy' src='")
                            .append(fileUrl).append("' alt='")
                            .append(htmlEscape(file.name)).append("'>");
                } else if (file.mimeType.startsWith("video/")) {
                    html.append("<video class='preview' controls playsinline preload='metadata' src='")
                            .append(fileUrl).append("'></video>");
                }

                html.append("<div class='actions'><a class='btn' href='")
                        .append(fileUrl).append("'>Mở tệp</a><a class='btn secondary' href='")
                        .append(fileUrl).append("?download=1'>Tải về</a></div></section>");
            }

            html.append("<section class='note'><b>Lưu vào iPhone</b><br>")
                    .append("• Ảnh: mở ảnh, dùng nút Chia sẻ của Safari rồi chọn Lưu hình ảnh.<br>")
                    .append("• Video: mở video, dùng nút Chia sẻ rồi chọn Lưu video nếu tùy chọn xuất hiện.<br>")
                    .append("• Tệp đã tải xuống có thể nằm trong ứng dụng Tệp (Files).<br><br>")
                    .append("Giữ điện thoại Vivo mở NQV AirShare cho đến khi tải xong.</section>")
                    .append("<div class='foot'>NQV AirShare · © Nông Quang Vinh</div></main></body></html>");

            Response response = newFixedLengthResponse(Response.Status.OK,
                    "text/html; charset=utf-8", html.toString());
            addCommonHeaders(response);
            return response;
        }

        private Response fileResponse(SharedFile file, boolean download) {
            try {
                InputStream input = getContentResolver().openInputStream(file.uri);
                if (input == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND,
                            "text/plain; charset=utf-8", "Không mở được tệp.");
                }

                Response response;
                if (file.size >= 0) {
                    response = newFixedLengthResponse(Response.Status.OK,
                            file.mimeType, input, file.size);
                } else {
                    response = newChunkedResponse(Response.Status.OK, file.mimeType, input);
                }

                String safeName = file.name.replaceAll("[^A-Za-z0-9._-]", "_");
                if (safeName.isEmpty()) safeName = "NQV-File";
                String encodedName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20");
                response.addHeader("Content-Disposition",
                        (download ? "attachment" : "inline") +
                                "; filename=\"" + safeName + "\"; filename*=UTF-8''" + encodedName);
                response.addHeader("X-Content-Type-Options", "nosniff");
                response.addHeader("Cache-Control", "no-store");
                response.addHeader("Accept-Ranges", "none");
                return response;
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                        "text/plain; charset=utf-8", "Không thể đọc tệp từ Vivo.");
            }
        }

        private void addCommonHeaders(Response response) {
            response.addHeader("Cache-Control", "no-store");
            response.addHeader("X-Content-Type-Options", "nosniff");
            response.addHeader("Referrer-Policy", "no-referrer");
            response.addHeader("Content-Security-Policy",
                    "default-src 'none'; style-src 'unsafe-inline'; img-src 'self'; media-src 'self'; base-uri 'none'");
        }
    }
}
