package com.nongquangvinh.airshare;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URL;
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
import java.util.concurrent.atomic.AtomicLong;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    private static final int PICK_FILES_REQUEST = 4102;
    private static final int SERVER_PORT = 8765;
    private static final long LOCAL_SESSION_MS = 30L * 60L * 1000L;
    private static final long MAX_REMOTE_FILE_BYTES = 5L * 1024L * 1024L * 1024L;
    private static final String PREFS = "nqv_airshare_settings";
    private static final String PREF_WORKER_URL = "worker_url";

    private final ArrayList<SharedFile> selectedFiles = new ArrayList<>();
    private final ArrayList<String> remoteLinks = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicLong localBytesSent = new AtomicLong(0L);

    private LinearLayout fileList;
    private LinearLayout resultList;
    private LinearLayout directPanel;
    private LinearLayout remotePanel;
    private LinearLayout qrCard;
    private TextView selectedCount;
    private TextView statusText;
    private TextView addressText;
    private TextView speedText;
    private TextView progressText;
    private TextView expiryText;
    private ImageView qrImage;
    private ProgressBar transferProgress;
    private EditText workerInput;
    private Button directTab;
    private Button remoteTab;
    private Button startDirectButton;
    private Button startRemoteButton;
    private Button stopButton;
    private Button copyButton;

    private ShareServer shareServer;
    private String shareUrl = "";
    private String sessionToken = "";
    private long localSessionExpiresAt = 0L;
    private long localTotalBytes = 0L;
    private long lastLocalByteSample = 0L;
    private long lastLocalSampleTime = 0L;
    private volatile boolean remoteBusy = false;
    private volatile long remoteUploadedBytes = 0L;
    private volatile long remoteTotalBytes = 0L;
    private volatile long remoteStartedAt = 0L;

    private final Runnable localStatsTicker = new Runnable() {
        @Override
        public void run() {
            if (shareServer == null || shareUrl.isEmpty()) return;
            long now = System.currentTimeMillis();
            if (now >= localSessionExpiresAt) {
                stopSharing(true);
                updateStatus("Phiên chia sẻ đã tự hết hạn sau 30 phút.");
                return;
            }
            long bytes = localBytesSent.get();
            long elapsed = Math.max(1L, now - lastLocalSampleTime);
            long delta = Math.max(0L, bytes - lastLocalByteSample);
            double speed = delta * 1000.0 / elapsed;
            lastLocalByteSample = bytes;
            lastLocalSampleTime = now;
            long shown = Math.min(bytes, Math.max(1L, localTotalBytes));
            int percent = localTotalBytes > 0
                    ? (int) Math.min(100L, bytes * 100L / localTotalBytes) : 0;
            transferProgress.setProgress(percent);
            progressText.setText("Đã truyền " + formatBytes(bytes) + " · " +
                    (localTotalBytes > 0 ? percent + "%" : "đang truyền"));
            speedText.setText("Tốc độ hiện tại  " + formatBytesPerSecond(speed));
            mainHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildScreen();
        refreshFileList();
        updateStatus("Sẵn sàng chia sẻ. Chọn ảnh hoặc video để bắt đầu.");
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(8, 17, 38), Color.rgb(15, 37, 70), Color.rgb(20, 29, 56)});
        scroll.setBackground(background);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(30));
        scroll.addView(page);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.nongquangvinh.airshare.R.drawable.nqv_logo);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        logoLp.rightMargin = dp(12);
        header.addView(logo, logoLp);

        LinearLayout brandBlock = new LinearLayout(this);
        brandBlock.setOrientation(LinearLayout.VERTICAL);
        header.addView(brandBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView brand = text("NQV AIRSHARE", 17, Color.WHITE, true);
        brandBlock.addView(brand);
        TextView copyrightTop = text("BẢN QUYỀN NÔNG QUANG VINH", 10,
                Color.rgb(160, 202, 255), true);
        LinearLayout.LayoutParams copyTopLp = params(-1, -2);
        copyTopLp.topMargin = dp(4);
        brandBlock.addView(copyrightTop, copyTopLp);

        TextView version = text("0.2.0", 11, Color.rgb(221, 238, 255), true);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(12), dp(8), dp(12), dp(8));
        version.setBackground(pillBackground(Color.argb(52, 139, 197, 255),
                Color.argb(105, 180, 220, 255)));
        header.addView(version);

        TextView title = text("Chia sẻ mọi nơi.", 30, Color.WHITE, true);
        LinearLayout.LayoutParams titleLp = params(-1, -2);
        titleLp.topMargin = dp(24);
        page.addView(title, titleLp);

        TextView subtitle = text(
                "Gửi ảnh và video gốc từ Vivo đến iPhone. iPhone chỉ cần Camera và Safari.",
                14, Color.rgb(192, 210, 234), false);
        LinearLayout.LayoutParams subtitleLp = params(-1, -2);
        subtitleLp.topMargin = dp(7);
        page.addView(subtitle, subtitleLp);

        LinearLayout modeCard = glassCard();
        LinearLayout.LayoutParams modeCardLp = params(-1, -2);
        modeCardLp.topMargin = dp(22);
        page.addView(modeCard, modeCardLp);

        TextView modeLabel = text("CHỌN PHƯƠNG THỨC", 11,
                Color.rgb(160, 202, 255), true);
        modeCard.addView(modeLabel);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabsLp = params(-1, dp(48));
        tabsLp.topMargin = dp(11);
        modeCard.addView(tabs, tabsLp);

        directTab = button("Wi-Fi trực tiếp", true);
        remoteTab = button("Qua Internet", false);
        tabs.addView(directTab, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams remoteTabLp = new LinearLayout.LayoutParams(0, -1, 1f);
        remoteTabLp.leftMargin = dp(8);
        tabs.addView(remoteTab, remoteTabLp);
        directTab.setOnClickListener(v -> selectMode(false));
        remoteTab.setOnClickListener(v -> selectMode(true));

        directPanel = new LinearLayout(this);
        directPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams directLp = params(-1, -2);
        directLp.topMargin = dp(14);
        page.addView(directPanel, directLp);

        LinearLayout localInfo = glassCard();
        directPanel.addView(localInfo);
        TextView localHeading = text("TRUYỀN TRỰC TIẾP", 12,
                Color.rgb(160, 202, 255), true);
        localInfo.addView(localHeading);
        TextView localDescription = text(
                "Không cần Internet. Bật điểm phát sóng trên Vivo, kết nối iPhone vào Wi-Fi đó, rồi quét mã QR.",
                14, Color.rgb(228, 238, 251), false);
        LinearLayout.LayoutParams localDescLp = params(-1, -2);
        localDescLp.topMargin = dp(8);
        localInfo.addView(localDescription, localDescLp);

        Button wifiButton = button("Mở cài đặt Wi-Fi / điểm phát sóng", false);
        LinearLayout.LayoutParams wifiLp = params(-1, dp(46));
        wifiLp.topMargin = dp(12);
        localInfo.addView(wifiButton, wifiLp);
        wifiButton.setOnClickListener(v -> openWirelessSettings());

        remotePanel = new LinearLayout(this);
        remotePanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams remoteLp = params(-1, -2);
        remoteLp.topMargin = dp(14);
        page.addView(remotePanel, remoteLp);

        LinearLayout remoteInfo = glassCard();
        remotePanel.addView(remoteInfo);
        TextView remoteHeading = text("TRUYỀN QUA INTERNET", 12,
                Color.rgb(160, 202, 255), true);
        remoteInfo.addView(remoteHeading);
        TextView remoteDescription = text(
                "Hai điện thoại có thể ở hai mạng khác nhau. Tệp được tải lên kho lưu trữ riêng tư qua HTTPS, sau đó iPhone mở liên kết bằng Safari.",
                14, Color.rgb(228, 238, 251), false);
        LinearLayout.LayoutParams remoteDescLp = params(-1, -2);
        remoteDescLp.topMargin = dp(8);
        remoteInfo.addView(remoteDescription, remoteDescLp);

        TextView workerLabel = text("ĐỊA CHỈ CLOUDFLARE WORKER", 10,
                Color.rgb(178, 207, 239), true);
        LinearLayout.LayoutParams workerLabelLp = params(-1, -2);
        workerLabelLp.topMargin = dp(15);
        remoteInfo.addView(workerLabel, workerLabelLp);

        workerInput = new EditText(this);
        workerInput.setSingleLine(true);
        workerInput.setTextSize(13);
        workerInput.setTextColor(Color.WHITE);
        workerInput.setHintTextColor(Color.rgb(145, 165, 193));
        workerInput.setHint("https://ten-worker.ten-tai-khoan.workers.dev");
        workerInput.setPadding(dp(13), dp(10), dp(13), dp(10));
        workerInput.setBackground(glassInputBackground());
        workerInput.setInputType(0x00000011);
        String savedWorker = getPreferences(MODE_PRIVATE).getString(PREF_WORKER_URL, "");
        workerInput.setText(savedWorker);
        LinearLayout.LayoutParams workerInputLp = params(-1, dp(48));
        workerInputLp.topMargin = dp(8);
        remoteInfo.addView(workerInput, workerInputLp);

        Button saveWorkerButton = button("LƯU ĐỊA CHỈ MÁY CHỦ", false);
        LinearLayout.LayoutParams saveWorkerLp = params(-1, dp(44));
        saveWorkerLp.topMargin = dp(8);
        remoteInfo.addView(saveWorkerButton, saveWorkerLp);
        saveWorkerButton.setOnClickListener(v -> saveWorkerAddress());

        TextView remoteNote = text(
                "Mỗi tệp tối đa 5 GiB theo cấu hình máy chủ hiện có. Liên kết mặc định tự hết hạn sau 24 giờ.",
                11, Color.rgb(178, 199, 224), false);
        LinearLayout.LayoutParams remoteNoteLp = params(-1, -2);
        remoteNoteLp.topMargin = dp(10);
        remoteInfo.addView(remoteNote, remoteNoteLp);

        Button pickButton = button("＋   Chọn ảnh và video", true);
        LinearLayout.LayoutParams pickLp = params(-1, dp(56));
        pickLp.topMargin = dp(18);
        page.addView(pickButton, pickLp);
        pickButton.setOnClickListener(v -> openPicker());

        selectedCount = text("Chưa chọn tệp", 13, Color.WHITE, true);
        LinearLayout.LayoutParams countLp = params(-1, -2);
        countLp.topMargin = dp(18);
        page.addView(selectedCount, countLp);

        fileList = new LinearLayout(this);
        fileList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fileListLp = params(-1, -2);
        fileListLp.topMargin = dp(9);
        page.addView(fileList, fileListLp);

        startDirectButton = button("TẠO MÃ QR · WIFI TRỰC TIẾP", true);
        LinearLayout.LayoutParams directButtonLp = params(-1, dp(54));
        directButtonLp.topMargin = dp(16);
        page.addView(startDirectButton, directButtonLp);
        startDirectButton.setOnClickListener(v -> startDirectSharing());

        startRemoteButton = button("GỬI QUA INTERNET", true);
        LinearLayout.LayoutParams remoteButtonLp = params(-1, dp(54));
        remoteButtonLp.topMargin = dp(16);
        page.addView(startRemoteButton, remoteButtonLp);
        startRemoteButton.setOnClickListener(v -> startRemoteUpload());

        qrCard = glassCard();
        LinearLayout.LayoutParams qrCardLp = params(-1, -2);
        qrCardLp.topMargin = dp(16);
        page.addView(qrCard, qrCardLp);

        TextView qrHeading = text("MÃ KẾT NỐI CỦA BẠN", 11,
                Color.rgb(160, 202, 255), true);
        qrCard.addView(qrHeading);

        qrImage = new ImageView(this);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qrImage.setBackgroundColor(Color.WHITE);
        qrImage.setPadding(dp(10), dp(10), dp(10), dp(10));
        qrImage.setVisibility(View.GONE);
        LinearLayout.LayoutParams qrImageLp = new LinearLayout.LayoutParams(dp(230), dp(230));
        qrImageLp.gravity = Gravity.CENTER_HORIZONTAL;
        qrImageLp.topMargin = dp(14);
        qrCard.addView(qrImage, qrImageLp);

        addressText = text("", 12, Color.WHITE, false);
        addressText.setTextIsSelectable(true);
        addressText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams addressLp = params(-1, -2);
        addressLp.topMargin = dp(12);
        qrCard.addView(addressText, addressLp);

        expiryText = text("", 11, Color.rgb(177, 201, 230), false);
        expiryText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams expiryLp = params(-1, -2);
        expiryLp.topMargin = dp(6);
        qrCard.addView(expiryText, expiryLp);

        copyButton = button("SAO CHÉP LIÊN KẾT", false);
        LinearLayout.LayoutParams copyLp = params(-1, dp(44));
        copyLp.topMargin = dp(10);
        qrCard.addView(copyButton, copyLp);
        copyButton.setOnClickListener(v -> copyAddress());

        stopButton = button("DỪNG CHIA SẺ", false);
        LinearLayout.LayoutParams stopLp = params(-1, dp(44));
        stopLp.topMargin = dp(8);
        qrCard.addView(stopButton, stopLp);
        stopButton.setOnClickListener(v -> stopSharing(true));

        LinearLayout progressCard = glassCard();
        LinearLayout.LayoutParams progressCardLp = params(-1, -2);
        progressCardLp.topMargin = dp(16);
        page.addView(progressCard, progressCardLp);

        TextView progressHeading = text("TRẠNG THÁI TRUYỀN", 11,
                Color.rgb(160, 202, 255), true);
        progressCard.addView(progressHeading);

        transferProgress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        transferProgress.setMax(100);
        transferProgress.setProgress(0);
        transferProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(112, 190, 255)));
        transferProgress.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Color.argb(70, 220, 235, 255)));
        LinearLayout.LayoutParams progressBarLp = params(-1, dp(8));
        progressBarLp.topMargin = dp(13);
        progressCard.addView(transferProgress, progressBarLp);

        progressText = text("Chưa có dữ liệu truyền", 12,
                Color.rgb(221, 233, 249), false);
        LinearLayout.LayoutParams progressTextLp = params(-1, -2);
        progressTextLp.topMargin = dp(9);
        progressCard.addView(progressText, progressTextLp);

        speedText = text("Tốc độ hiện tại  —", 12,
                Color.rgb(164, 205, 245), true);
        LinearLayout.LayoutParams speedLp = params(-1, -2);
        speedLp.topMargin = dp(5);
        progressCard.addView(speedText, speedLp);

        LinearLayout resultsCard = glassCard();
        LinearLayout.LayoutParams resultsLp = params(-1, -2);
        resultsLp.topMargin = dp(16);
        page.addView(resultsCard, resultsLp);
        TextView resultsHeading = text("LIÊN KẾT ĐÃ TẠO", 11,
                Color.rgb(160, 202, 255), true);
        resultsCard.addView(resultsHeading);
        resultList = new LinearLayout(this);
        resultList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resultListLp = params(-1, -2);
        resultListLp.topMargin = dp(8);
        resultsCard.addView(resultList, resultListLp);
        resultsCard.setVisibility(View.GONE);

        LinearLayout statusCard = glassCard();
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(16);
        page.addView(statusCard, statusLp);
        TextView statusHeading = text("THÔNG BÁO", 10,
                Color.rgb(160, 202, 255), true);
        statusCard.addView(statusHeading);
        statusText = text("Sẵn sàng", 13,
                Color.rgb(220, 234, 250), false);
        LinearLayout.LayoutParams statusTextLp = params(-1, -2);
        statusTextLp.topMargin = dp(7);
        statusCard.addView(statusText, statusTextLp);

        TextView footer = text("© Nông Quang Vinh  ·  NQV AirShare 0.2.0", 11,
                Color.rgb(153, 177, 208), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = params(-1, -2);
        footerLp.topMargin = dp(25);
        page.addView(footer, footerLp);

        setContentView(scroll);
        qrCard.setVisibility(View.GONE);
        selectMode(false);
    }

    private void selectMode(boolean remote) {
        directPanel.setVisibility(remote ? View.GONE : View.VISIBLE);
        remotePanel.setVisibility(remote ? View.VISIBLE : View.GONE);
        startDirectButton.setVisibility(remote ? View.GONE : View.VISIBLE);
        startRemoteButton.setVisibility(remote ? View.VISIBLE : View.GONE);
        directTab.setBackground(buttonBackground(!remote));
        directTab.setTextColor(!remote ? Color.rgb(8, 25, 48) : Color.rgb(227, 239, 255));
        remoteTab.setBackground(buttonBackground(remote));
        remoteTab.setTextColor(remote ? Color.rgb(8, 25, 48) : Color.rgb(227, 239, 255));
        if (remote) updateStatus("Chế độ Internet: cần địa chỉ Worker đang hoạt động.");
        else updateStatus("Chế độ Wi-Fi trực tiếp: hai điện thoại cần cùng mạng Wi-Fi.");
    }

    private void openWirelessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
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
            }

            String mime = getContentResolver().getType(uri);
            if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/"))) {
                continue;
            }
            selectedFiles.add(new SharedFile(uri, queryName(uri), mime, querySize(uri)));
            existing.add(uri.toString());
            added++;
        }

        stopSharing(false);
        refreshFileList();
        updateStatus(added > 0
                ? "Đã thêm " + added + " tệp. Dữ liệu gốc sẽ được truyền, không nén."
                : "Không có ảnh hoặc video mới được chọn.");
    }

    private void refreshFileList() {
        if (fileList == null || selectedCount == null) return;
        fileList.removeAllViews();

        selectedCount.setText(selectedFiles.isEmpty()
                ? "CHƯA CHỌN TỆP"
                : "ĐÃ CHỌN " + selectedFiles.size() + " TỆP");

        for (int i = 0; i < selectedFiles.size(); i++) {
            final int index = i;
            SharedFile file = selectedFiles.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(13), dp(11), dp(9), dp(11));
            row.setBackground(glassRowBackground());

            TextView symbol = text(file.mimeType.startsWith("video/") ? "▶" : "▧",
                    18, Color.rgb(150, 204, 255), true);
            symbol.setGravity(Gravity.CENTER);
            row.addView(symbol, new LinearLayout.LayoutParams(dp(34), dp(38)));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
            infoLp.leftMargin = dp(7);
            row.addView(info, infoLp);

            TextView name = text(file.name, 13, Color.WHITE, true);
            name.setMaxLines(2);
            info.addView(name);
            TextView meta = text(file.mimeType + "  ·  " + formatBytes(file.size),
                    10, Color.rgb(174, 195, 223), false);
            LinearLayout.LayoutParams metaLp = params(-1, -2);
            metaLp.topMargin = dp(4);
            info.addView(meta, metaLp);

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
            rowLp.bottomMargin = dp(8);
            fileList.addView(row, rowLp);
        }

        boolean hasFiles = !selectedFiles.isEmpty();
        startDirectButton.setEnabled(hasFiles && !remoteBusy);
        startRemoteButton.setEnabled(hasFiles && !remoteBusy);
    }

    private void startDirectSharing() {
        if (selectedFiles.isEmpty()) {
            updateStatus("Hãy chọn ít nhất một ảnh hoặc video trước.");
            return;
        }
        stopSharing(false);
        String ip = findLocalIpv4();
        if (ip == null) {
            updateStatus("Không tìm thấy địa chỉ Wi-Fi. Bật điểm phát sóng Vivo hoặc kết nối Wi-Fi rồi thử lại.");
            return;
        }

        sessionToken = createToken();
        shareUrl = "http://" + ip + ":" + SERVER_PORT + "/" + sessionToken + "/";
        localSessionExpiresAt = System.currentTimeMillis() + LOCAL_SESSION_MS;
        localTotalBytes = totalSelectedBytes();
        localBytesSent.set(0L);
        lastLocalByteSample = 0L;
        lastLocalSampleTime = System.currentTimeMillis();

        try {
            shareServer = new ShareServer(SERVER_PORT, sessionToken,
                    new ArrayList<>(selectedFiles), localSessionExpiresAt);
            shareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            showQr(shareUrl, "Mạng nội bộ · Tự hết hạn sau 30 phút");
            qrCard.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.VISIBLE);
            copyButton.setVisibility(View.VISIBLE);
            transferProgress.setProgress(0);
            progressText.setText("Chờ iPhone quét mã và tải tệp");
            speedText.setText("Tốc độ hiện tại  —");
            updateStatus("Đang chia sẻ trong mạng riêng. Giữ ứng dụng Vivo mở cho đến khi tải xong.");
            mainHandler.removeCallbacks(localStatsTicker);
            mainHandler.postDelayed(localStatsTicker, 1000L);
            mainHandler.postDelayed(() -> {
                if (shareServer != null && System.currentTimeMillis() >= localSessionExpiresAt) {
                    stopSharing(true);
                    updateStatus("Phiên chia sẻ đã tự hết hạn sau 30 phút.");
                }
            }, LOCAL_SESSION_MS);
        } catch (Exception e) {
            shareServer = null;
            updateStatus("Không thể mở máy chủ truyền tệp: " + e.getMessage());
        }
    }

    private void stopSharing(boolean showMessage) {
        mainHandler.removeCallbacks(localStatsTicker);
        if (shareServer != null) {
            try {
                shareServer.stop();
            } catch (Exception ignored) {
            }
            shareServer = null;
        }
        shareUrl = "";
        sessionToken = "";
        localSessionExpiresAt = 0L;
        if (qrImage != null) qrImage.setVisibility(View.GONE);
        if (addressText != null) addressText.setText("");
        if (expiryText != null) expiryText.setText("");
        if (stopButton != null) stopButton.setVisibility(View.GONE);
        if (copyButton != null) copyButton.setVisibility(View.GONE);
        if (showMessage) updateStatus("Đã dừng chia sẻ. Mã QR cũ không còn hiệu lực.");
    }

    private void saveWorkerAddress() {
        String value = workerInput.getText().toString().trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (!value.startsWith("https://") || value.length() < 15) {
            updateStatus("Địa chỉ Worker phải bắt đầu bằng https:// và không thêm /health.");
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putString(PREF_WORKER_URL, value).apply();
        workerInput.setText(value);
        updateStatus("Đã lưu địa chỉ máy chủ. Có thể gửi tệp qua Internet.");
    }

    private void startRemoteUpload() {
        if (remoteBusy) return;
        if (selectedFiles.isEmpty()) {
            updateStatus("Hãy chọn ít nhất một ảnh hoặc video trước.");
            return;
        }

        String configuredBase = workerInput.getText().toString().trim();
        while (configuredBase.endsWith("/")) {
            configuredBase = configuredBase.substring(0, configuredBase.length() - 1);
        }
        if (!configuredBase.startsWith("https://")) {
            updateStatus("Nhập địa chỉ HTTPS của Cloudflare Worker rồi nhấn Lưu địa chỉ máy chủ.");
            return;
        }
        final String base = configuredBase;
        getPreferences(MODE_PRIVATE).edit().putString(PREF_WORKER_URL, base).apply();

        final long total = totalSelectedBytes();
        for (SharedFile file : selectedFiles) {
            if (file.size < 0) {
                updateStatus("Không đọc được dung lượng của " + file.name + ". Hãy chọn lại tệp từ Thư viện hoặc Tệp.");
                return;
            }
            if (file.size > MAX_REMOTE_FILE_BYTES) {
                updateStatus("Tệp " + file.name + " vượt giới hạn 5 GiB mỗi tệp của máy chủ hiện tại.");
                return;
            }
        }

        remoteBusy = true;
        remoteUploadedBytes = 0L;
        remoteTotalBytes = total;
        remoteStartedAt = System.currentTimeMillis();
        remoteLinks.clear();
        resultList.removeAllViews();
        qrCard.setVisibility(View.GONE);
        transferProgress.setProgress(0);
        progressText.setText("Đang chuẩn bị tải lên...");
        speedText.setText("Tốc độ hiện tại  —");
        startRemoteButton.setEnabled(false);
        startDirectButton.setEnabled(false);
        updateStatus("Đang kết nối Worker và tạo liên kết riêng tư...");

        Thread uploader = new Thread(() -> {
            ArrayList<String[]> completed = new ArrayList<>();
            long completedBytes = 0L;
            try {
                for (SharedFile file : selectedFiles) {
                    JSONObject ticket = requestUploadTicket(base, file);
                    String uploadUrl = ticket.getString("uploadUrl");
                    String shareLink = ticket.getString("shareUrl");
                    JSONObject headers = ticket.getJSONObject("uploadHeaders");
                    uploadToStorage(uploadUrl, headers, file, completedBytes, total);
                    completedBytes += file.size;
                    completed.add(new String[]{file.name, shareLink});
                    runOnUiThread(() -> addRemoteResult(file.name, shareLink));
                }

                remoteLinks.clear();
                for (String[] entry : completed) remoteLinks.add(entry[1]);
                runOnUiThread(() -> {
                    remoteBusy = false;
                    startRemoteButton.setEnabled(!selectedFiles.isEmpty());
                    startDirectButton.setEnabled(!selectedFiles.isEmpty());
                    if (!completed.isEmpty()) {
                        String firstLink = completed.get(0)[1];
                        showQr(firstLink, "Liên kết HTTPS · Tự hết hạn sau 24 giờ");
                        qrCard.setVisibility(View.VISIBLE);
                        stopButton.setVisibility(View.GONE);
                        copyButton.setVisibility(View.VISIBLE);
                        resultList.getParent().requestChildFocus(resultList, resultList);
                        updateStatus("Đã tải lên " + completed.size() + " tệp. Gửi mã QR hoặc từng liên kết cho iPhone.");
                    } else {
                        updateStatus("Không có tệp nào được tải lên.");
                    }
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Lỗi không xác định" : e.getMessage();
                runOnUiThread(() -> {
                    remoteBusy = false;
                    startRemoteButton.setEnabled(!selectedFiles.isEmpty());
                    startDirectButton.setEnabled(!selectedFiles.isEmpty());
                    updateStatus("Tải lên chưa hoàn tất: " + message);
                });
            }
        }, "NQV-Remote-Uploader");
        uploader.start();
    }

    private JSONObject requestUploadTicket(String base, SharedFile file) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(base + "/api/create-upload");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject body = new JSONObject();
            body.put("filename", file.name);
            body.put("contentType", file.mimeType);
            body.put("size", file.size);
            body.put("expiresInDays", 1);
            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(data.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(data);
            }

            int code = connection.getResponseCode();
            InputStream responseStream = code >= 400
                    ? connection.getErrorStream() : connection.getInputStream();
            String response = readText(responseStream);
            if (code < 200 || code >= 300) {
                throw new IOException("Worker phản hồi HTTP " + code + ": " + response);
            }
            JSONObject json = new JSONObject(response);
            if (!json.has("uploadUrl") || !json.has("shareUrl")) {
                throw new IOException("Worker trả về dữ liệu không đầy đủ.");
            }
            return json;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void uploadToStorage(String uploadUrl, JSONObject headers,
                                 SharedFile file, long bytesBefore, long total) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setFixedLengthStreamingMode(file.size);

            java.util.Iterator<String> names = headers.keys();
            while (names.hasNext()) {
                String key = names.next();
                connection.setRequestProperty(key, headers.getString(key));
            }

            try (InputStream input = new BufferedInputStream(
                    getContentResolver().openInputStream(file.uri));
                 OutputStream output = new BufferedOutputStream(connection.getOutputStream())) {
                if (input == null) throw new IOException("Không mở được tệp " + file.name);
                byte[] buffer = new byte[256 * 1024];
                long sentForFile = 0L;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    sentForFile += read;
                    remoteUploadedBytes = bytesBefore + sentForFile;
                    updateRemoteProgress(remoteUploadedBytes, total);
                }
                output.flush();
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                String response = readText(connection.getErrorStream());
                throw new IOException("Kho lưu trữ phản hồi HTTP " + code + ": " + response);
            }
            remoteUploadedBytes = bytesBefore + file.size;
            updateRemoteProgress(remoteUploadedBytes, total);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void updateRemoteProgress(long sent, long total) {
        long elapsed = Math.max(1L, System.currentTimeMillis() - remoteStartedAt);
        double speed = sent * 1000.0 / elapsed;
        int percent = total > 0 ? (int) Math.min(100L, sent * 100L / total) : 0;
        runOnUiThread(() -> {
            transferProgress.setProgress(percent);
            progressText.setText("Đã tải lên " + formatBytes(sent) + " / " + formatBytes(total)
                    + " · " + percent + "%");
            speedText.setText("Tốc độ trung bình  " + formatBytesPerSecond(speed));
        });
    }

    private void addRemoteResult(String filename, String link) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(10), dp(8), dp(10));
        row.setBackground(glassRowBackground());

        TextView name = text(filename, 12, Color.WHITE, true);
        name.setMaxLines(2);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

        Button copy = button("Sao chép", false);
        LinearLayout.LayoutParams copyLp = params(dp(94), dp(40));
        copyLp.leftMargin = dp(8);
        row.addView(copy, copyLp);
        copy.setOnClickListener(v -> copyText(link, "Đã sao chép liên kết"));
        LinearLayout.LayoutParams rowLp = params(-1, -2);
        rowLp.bottomMargin = dp(7);
        resultList.addView(row, rowLp);
        View parent = (View) resultList.getParent();
        parent.setVisibility(View.VISIBLE);
    }

    private void showQr(String value, String expiry) {
        try {
            qrImage.setImageBitmap(createQr(value));
            qrImage.setVisibility(View.VISIBLE);
            addressText.setText(value);
            expiryText.setText(expiry);
            qrCard.setVisibility(View.VISIBLE);
            if (copyButton != null) copyButton.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            updateStatus("Không tạo được mã QR: " + e.getMessage());
        }
    }

    private void copyAddress() {
        if (shareUrl.isEmpty()) {
            if (!remoteLinks.isEmpty()) {
                copyText(remoteLinks.get(0), "Đã sao chép liên kết đầu tiên");
            } else {
                updateStatus("Chưa có liên kết để sao chép.");
            }
            return;
        }
        copyText(shareUrl, "Đã sao chép liên kết");
    }

    private void copyText(String value, String message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("NQV AirShare", value));
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                for (int i = 1; i < uris.size(); i++) clip.addItem(new ClipData.Item(uris.get(i)));
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

    private long totalSelectedBytes() {
        long total = 0L;
        for (SharedFile file : selectedFiles) {
            if (file.size > 0 && Long.MAX_VALUE - total > file.size) total += file.size;
        }
        return total;
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
                            || address.isLinkLocalAddress() || !address.isSiteLocalAddress()) continue;
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
        byte[] bytes = new byte[32];
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

    private String readText(InputStream input) throws IOException {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString("UTF-8");
        }
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

    private LinearLayout glassCard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(glassBackground());
        layout.setElevation(dp(4));
        return layout;
    }

    private GradientDrawable glassBackground() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(54, 255, 255, 255),
                        Color.argb(26, 194, 220, 255),
                        Color.argb(39, 255, 255, 255)});
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), Color.argb(72, 225, 240, 255));
        return bg;
    }

    private GradientDrawable glassRowBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(43, 240, 247, 255));
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(1), Color.argb(48, 230, 241, 255));
        return bg;
    }

    private GradientDrawable glassInputBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(45, 255, 255, 255));
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1), Color.argb(80, 220, 236, 255));
        return bg;
    }

    private GradientDrawable pillBackground(int fill, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(30));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private GradientDrawable buttonBackground(boolean primary) {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                primary
                        ? new int[]{Color.rgb(151, 211, 255), Color.rgb(110, 174, 247)}
                        : new int[]{Color.argb(54, 255, 255, 255), Color.argb(32, 255, 255, 255)});
        bg.setCornerRadius(dp(15));
        bg.setStroke(dp(1), primary
                ? Color.argb(150, 213, 238, 255)
                : Color.argb(70, 225, 240, 255));
        return bg;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.rgb(7, 25, 48) : Color.rgb(225, 238, 255));
        button.setBackground(buttonBackground(primary));
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        return button;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Không rõ";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L)
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatBytesPerSecond(double bytesPerSecond) {
        if (bytesPerSecond <= 0) return "—";
        return formatBytes((long) bytesPerSecond) + "/s";
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

    private class CountingInputStream extends FilterInputStream {
        private final AtomicLong counter;

        CountingInputStream(InputStream input, AtomicLong counter) {
            super(input);
            this.counter = counter;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) counter.incrementAndGet();
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0) counter.addAndGet(count);
            return count;
        }
    }

    private class ShareServer extends NanoHTTPD {
        private final String token;
        private final ArrayList<SharedFile> files;
        private final long expiresAt;

        ShareServer(int port, String token, ArrayList<SharedFile> files, long expiresAt) {
            super(port);
            this.token = token;
            this.files = files;
            this.expiresAt = expiresAt;
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (System.currentTimeMillis() >= expiresAt) {
                return newFixedLengthResponse(Response.Status.GONE,
                        "text/plain; charset=utf-8", "Phiên chia sẻ đã hết hạn.");
            }
            if (session.getMethod() != Method.GET && session.getMethod() != Method.HEAD) {
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                        "text/plain; charset=utf-8", "Method not allowed");
            }

            String path = session.getUri();
            String base = "/" + token;
            if (path.equals(base) || path.equals(base + "/")) return pageResponse();

            String filePrefix = base + "/file/";
            if (path.startsWith(filePrefix)) {
                String rawIndex = path.substring(filePrefix.length());
                int slash = rawIndex.indexOf('/');
                if (slash >= 0) rawIndex = rawIndex.substring(0, slash);
                try {
                    int index = Integer.parseInt(rawIndex);
                    if (index >= 0 && index < files.size()) {
                        return fileResponse(files.get(index));
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
                    .append("*{box-sizing:border-box}body{margin:0;background:linear-gradient(145deg,#edf5ff,#f6f8fc);color:#14233a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}")
                    .append(".wrap{max-width:720px;margin:auto;padding:25px 16px 45px}.hero{background:linear-gradient(135deg,#12233e,#244c7c);color:white;border-radius:24px;padding:24px;box-shadow:0 12px 35px #19385c25}")
                    .append(".tag{color:#a9d5ff;font-size:11px;font-weight:800;letter-spacing:2px}.hero h1{font-size:27px;margin:10px 0}.hero p{color:#d6e6f8;line-height:1.55;margin:0}")
                    .append(".item{background:#ffffffdd;border:1px solid #dfe9f5;border-radius:18px;padding:16px;margin-top:13px;box-shadow:0 7px 24px #19385c0d}")
                    .append(".name{font-weight:750;overflow-wrap:anywhere}.meta{font-size:12px;color:#687991;margin-top:6px}.actions{display:flex;gap:9px;flex-wrap:wrap;margin-top:13px}")
                    .append(".btn{display:inline-block;padding:12px 16px;border-radius:12px;text-decoration:none;background:linear-gradient(135deg,#317fd0,#5aa7f3);color:white;font-weight:700;font-size:13px}")
                    .append(".note{background:#e8f1fc;border:1px solid #d5e5f7;border-radius:16px;padding:15px;margin-top:18px;line-height:1.6;font-size:13px;color:#29415e}")
                    .append(".foot{text-align:center;color:#718096;font-size:12px;margin-top:24px}")
                    .append("</style></head><body><main class='wrap'><section class='hero'><div class='tag'>NQV  /  AIRSHARE</div>")
                    .append("<h1>Tệp gốc đã sẵn sàng</h1><p>Được truyền trực tiếp trong mạng Wi-Fi riêng. Chọn tệp để tải xuống iPhone.</p></section>");

            for (int i = 0; i < files.size(); i++) {
                SharedFile file = files.get(i);
                String fileUrl = "/" + token + "/file/" + i;
                html.append("<section class='item'><div class='name'>")
                        .append(htmlEscape(file.name)).append("</div><div class='meta'>")
                        .append(htmlEscape(file.mimeType)).append(" · ")
                        .append(htmlEscape(formatBytes(file.size))).append("</div>")
                        .append("<div class='actions'><a class='btn' href='")
                        .append(fileUrl).append("'>Tải tệp gốc</a></div></section>");
            }

            html.append("<section class='note'><b>Lưu vào iPhone</b><br>")
                    .append("Sau khi tải xong, mở ứng dụng Tệp (Files). Bạn có thể dùng Chia sẻ để lưu ảnh hoặc video vào thư viện Ảnh.<br><br>")
                    .append("Giữ NQV AirShare mở trên Vivo cho đến khi tải hoàn tất.</section>")
                    .append("<div class='foot'>© Nông Quang Vinh · NQV AirShare 0.2.0</div></main></body></html>");

            Response response = newFixedLengthResponse(Response.Status.OK,
                    "text/html; charset=utf-8", html.toString());
            addCommonHeaders(response);
            return response;
        }

        private Response fileResponse(SharedFile file) {
            try {
                InputStream raw = getContentResolver().openInputStream(file.uri);
                if (raw == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND,
                            "text/plain; charset=utf-8", "Không mở được tệp.");
                }
                CountingInputStream input = new CountingInputStream(raw, localBytesSent);
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
                        "attachment; filename=\"" + safeName +
                                "\"; filename*=UTF-8''" + encodedName);
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
                    "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'");
        }
    }
}
