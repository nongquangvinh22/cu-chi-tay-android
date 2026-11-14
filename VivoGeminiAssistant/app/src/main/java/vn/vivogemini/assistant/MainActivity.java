package vn.vivogemini.assistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Native Android home and settings UI. No WebView or embedded HTML.
 */
public class MainActivity extends Activity {
    private static final int BG = 0xFF0B1020;
    private static final int SURFACE = 0xFF151E33;
    private static final int SURFACE_2 = 0xFF1C2942;
    private static final int PRIMARY = 0xFF83A9FF;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFFAAB7D0;
    private static final int GREEN = 0xFF6DD6B0;
    private static final int REQUEST_ASSISTANT_ROLE = 70;
    private static final int REQUEST_NOTIFICATIONS = 71;

    private LinearLayout root;
    private FrameLayout pageHost;
    private LinearLayout nav;
    private String page = "home";

    private static final String[] ACTION_VALUES =
            {"assistant", "circle", "gemini", "translate", "off"};
    private static final String[] ACTION_LABELS =
            {"Vinh AI nổi trên màn hình", "Circle to Search", "Gemini", "Google Dịch", "Không làm gì"};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        ensureDefaults();
        buildShell();
        showPage(page);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pageHost != null) showPage(page);
    }

    private void ensureDefaults() {
        if (getSharedPreferences("vinh_circle", MODE_PRIVATE).contains("json")) return;
        JSONObject s = new JSONObject();
        try {
            s.put("settingsVersion", 3);
            s.put("barEnabled", false);
            s.put("singleAction", "circle");
            s.put("holdAction", "assistant");
            s.put("doubleAction", "gemini");
            s.put("tripleAction", "translate");
            s.put("preferLive", true);
            s.put("dragMode", false);
            s.put("hideLandscape", true);
            s.put("hideKeyboard", true);
            s.put("axis", "horizontal");
            s.put("align", 50);
            s.put("widthDp", 120);
            s.put("thicknessDp", 18);
            s.put("opacity", 85);
            s.put("bottomDp", 0);
            s.put("holdMs", 500);
            s.put("theme", "breeze");
            s.put("readEnabled", false);
            s.put("readPackages", new JSONArray());
            s.put("days", new JSONArray().put(1).put(2).put(3).put(4).put(5).put(6).put(7));
            s.put("from", "00:00");
            s.put("to", "00:00");
            s.put("voice", "auto");
            s.put("bankSafeActive", false);
            Store.replace(this, s.toString());
        } catch (Exception ignored) {
            // Defaults will be applied by individual controls when the store is unavailable.
        }
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(12));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView mark = new TextView(this);
        mark.setText("VC");
        mark.setTextColor(BG);
        mark.setTextSize(17);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(PRIMARY, 16, PRIMARY));
        header.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(12);
        header.addView(titleBox, titleParams);
        TextView title = text("VINH CIRCLE", 16, true, TEXT);
        titleBox.addView(title);
        titleBox.addView(text("Trợ lý Android • Nông Quang Vinh", 11, false, MUTED));

        Button ai = button("✦ AI", true);
        ai.setMinWidth(dp(68));
        header.addView(ai);
        ai.setOnClickListener(v -> openVinhAi());

        View line = new View(this);
        line.setBackgroundColor(0x334B5F85);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        pageHost = new FrameLayout(this);
        root.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(8));
        nav.setBackgroundColor(0xFF10182A);
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));

        addNavItem("home", "⌂", "Trang chủ");
        addNavItem("gestures", "⌁", "Cử chỉ");
        addNavItem("bar", "━", "Thanh");
        addNavItem("settings", "⚙", "Cài đặt");

        setContentView(root);
    }

    private void addNavItem(String id, String icon, String title) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(3), dp(7), dp(3), dp(7));
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> {
            page = id;
            showPage(id);
        });
        TextView glyph = text(icon, 19, true, MUTED);
        glyph.setGravity(Gravity.CENTER);
        item.addView(glyph);
        TextView label = text(title, 10, true, MUTED);
        label.setGravity(Gravity.CENTER);
        item.addView(label);
        item.setTag(id);
        nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void showPage(String id) {
        if (pageHost == null) return;
        page = id;
        pageHost.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(16), dp(16), dp(16), dp(24));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        pageHost.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        switch (id) {
            case "gestures": buildGesturesPage(content); break;
            case "bar": buildBarPage(content); break;
            case "settings": buildSettingsPage(content); break;
            default: buildHomePage(content); break;
        }
        updateNavState();
    }

    private void updateNavState() {
        if (nav == null) return;
        for (int i = 0; i < nav.getChildCount(); i++) {
            View item = nav.getChildAt(i);
            boolean selected = page.equals(item.getTag());
            item.setBackground(round(selected ? 0xFF20365A : Color.TRANSPARENT, 13,
                    selected ? 0x554C78C5 : Color.TRANSPARENT));
            if (item instanceof LinearLayout) {
                LinearLayout box = (LinearLayout) item;
                for (int j = 0; j < box.getChildCount(); j++) {
                    if (box.getChildAt(j) instanceof TextView) {
                        ((TextView) box.getChildAt(j)).setTextColor(selected ? PRIMARY : MUTED);
                    }
                }
            }
        }
    }

    private void buildHomePage(LinearLayout content) {
        addText(content, "Trợ lý của bạn", 25, true, TEXT, 0, 2);
        addText(content, "Mở AI ngay trên ứng dụng đang dùng hoặc gọi trợ lý mặc định của Android.", 13, false, MUTED, 0, 15);

        LinearLayout hero = card(content);
        TextView badge = text("VINH AI  •  NATIVE ANDROID", 10, true, GREEN);
        hero.addView(badge);
        hero.addView(text("Một chạm để gọi trợ lý", 20, true, TEXT), margin(0, 7, 0, 2));
        hero.addView(text("Khung chat Android nổi trên màn hình hiện tại. Không cần mở trình duyệt hay trang web.", 12, false, MUTED), margin(0, 0, 0, 12));
        Button open = button("MỞ VINH AI NỔI  →", true);
        hero.addView(open, match());
        open.setOnClickListener(v -> openVinhAi());
        Button full = button("Mở giao diện AI toàn màn hình", false);
        hero.addView(full, margin(0, 8, 0, 0));
        full.setOnClickListener(v -> startActivity(new Intent(this, AssistantActivity.class)));

        LinearLayout quick = card(content);
        quick.addView(text("TRỢ LÝ HỆ THỐNG", 11, true, PRIMARY));
        quick.addView(text("Dùng AI mặc định của điện thoại", 17, true, TEXT), margin(0, 5, 0, 3));
        quick.addView(text("Nếu Google Gemini đã được đặt làm trợ lý mặc định, Android sẽ gọi trợ lý đó. Bạn cũng có thể đăng ký Vinh Circle làm trợ lý mặc định.", 12, false, MUTED));
        Button system = button("GỌI TRỢ LÝ MẶC ĐỊNH", true);
        quick.addView(system, margin(0, 11, 0, 0));
        system.setOnClickListener(v -> launchSystemAssistant());
        Button choose = button("Đặt Vinh Circle làm trợ lý mặc định", false);
        quick.addView(choose, margin(0, 7, 0, 0));
        choose.setOnClickListener(v -> requestAssistantRole());

        LinearLayout launchers = card(content);
        launchers.addView(text("ỨNG DỤNG AI", 11, true, PRIMARY));
        addRowButton(launchers, "Mở ứng dụng Gemini", "Chuyển sang ứng dụng Gemini đã cài trên máy", () -> Launcher.gemini(this));
        addRowButton(launchers, "Circle to Search", "Phụ thuộc Google app và hỗ trợ của OriginOS", () -> Launcher.circle(this));

        LinearLayout state = card(content);
        state.addView(text("TÌNH TRẠNG THIẾT BỊ", 11, true, PRIMARY));
        addStatusLine(state, "Hiển thị trên ứng dụng khác", Settings.canDrawOverlays(this));
        addStatusLine(state, "Khóa Gemini API", ApiKeyVault.hasKey(this));
        addStatusLine(state, "Google", hasPackage(Launcher.GOOGLE));
        addStatusLine(state, "Gemini", hasPackage(Launcher.BARD));
        addStatusLine(state, "Trợ lý Vinh Circle mặc định", isOurAssistantDefault());

        LinearLayout copyright = new LinearLayout(this);
        copyright.setGravity(Gravity.CENTER);
        copyright.setPadding(dp(5), dp(16), dp(5), dp(4));
        content.addView(copyright);
        copyright.addView(text("Bản quyền của Nông Quang Vinh", 11, true, MUTED));
        addText(content, "Ứng dụng độc lập, không phải sản phẩm của vivo hoặc Google.", 10, false, MUTED, 0, 0);
    }

    private void buildGesturesPage(LinearLayout content) {
        addText(content, "Thiết lập cử chỉ", 24, true, TEXT, 0, 3);
        addText(content, "Chọn hành động tương ứng với từng kiểu chạm lên thanh nổi.", 13, false, MUTED, 0, 13);
        LinearLayout enabled = card(content);
        addText(enabled, "BẬT / TẮT", 11, true, PRIMARY, 0, 8);
        addToggle(enabled, "Bật thanh cử chỉ", "Cho phép thanh nhận thao tác trên màn hình khác.", "barEnabled", false, true);
        LinearLayout actions = card(content);
        actions.addView(text("GÁN HÀNH ĐỘNG", 11, true, PRIMARY));
        addSelect(actions, "Chạm một lần", "singleAction", "circle");
        addSelect(actions, "Nhấn giữ", "holdAction", "assistant");
        addSelect(actions, "Chạm đúp", "doubleAction", "gemini");
        addSelect(actions, "Chạm ba lần", "tripleAction", "translate");

        LinearLayout notes = card(content);
        notes.addView(text("LƯU Ý", 11, true, PRIMARY));
        notes.addView(text("• Vinh AI nổi mở khung chat ngay trên ứng dụng hiện tại.\n• Gemini mở ứng dụng Gemini nếu đã cài.\n• Circle to Search cần Google app và hỗ trợ từ firmware.\n• Thanh phải được bật và có quyền hiển thị trên ứng dụng khác.", 12, false, MUTED), margin(0, 7, 0, 0));
    }

    private void buildBarPage(LinearLayout content) {
        addText(content, "Tùy chỉnh thanh", 24, true, TEXT, 0, 3);
        addText(content, "Đặt thanh sát cạnh dưới, chỉnh độ trong suốt và kích thước.", 13, false, MUTED, 0, 13);
        LinearLayout appearance = card(content);
        appearance.addView(text("HIỂN THỊ", 11, true, PRIMARY));
        addSeekSetting(appearance, "Độ hiển thị", "0% = trong suốt hoàn toàn nhưng vùng chạm vẫn hoạt động.", "opacity", 0, 100, 85, "%");
        addSeekSetting(appearance, "Chiều dài thanh", "Độ dài theo dp.", "widthDp", 50, 260, 120, " dp");
        addSeekSetting(appearance, "Độ dày thanh", "Độ dày theo dp.", "thicknessDp", 6, 40, 18, " dp");
        addSeekSetting(appearance, "Cách mép dưới", "0 dp để thanh nằm sát đáy màn hình.", "bottomDp", 0, 100, 0, " dp");

        LinearLayout placement = card(content);
        placement.addView(text("VỊ TRÍ VÀ HÀNH VI", 11, true, PRIMARY));
        addToggle(placement, "Cho phép kéo thanh", "Kéo thanh để thay đổi vị trí ngang/dọc.", "dragMode", false, false);
        addToggle(placement, "Ẩn khi xoay ngang", "Tự ẩn thanh trong chế độ ngang.", "hideLandscape", true, false);
        addToggle(placement, "Ẩn khi bàn phím mở", "Tránh thanh che vùng nhập văn bản.", "hideKeyboard", true, false);
        addToggle(placement, "Ưu tiên Gemini Live", "Khi thao tác cử chỉ gọi Gemini, ưu tiên mở chế độ Live nếu ứng dụng và tài khoản hỗ trợ.", "preferLive", true, false);
        addSelect(placement, "Hướng thanh", "axis", "horizontal",
                new String[]{"horizontal", "vertical"},
                new String[]{"Nằm ngang cạnh dưới", "Dọc mép phải"});
        addSelect(placement, "Màu sắc", "theme", "breeze",
                new String[]{"breeze", "google", "cyber", "glass"},
                new String[]{"Tím xanh", "Google", "Cyber", "Trong mờ"});
        addSeekSetting(placement, "Thời gian nhấn giữ", "Thời gian cần giữ trước khi kích hoạt.", "holdMs", 250, 1500, 500, " ms");

        LinearLayout reset = card(content);
        reset.addView(text("XEM TRƯỚC VÀ KHÔI PHỤC", 11, true, PRIMARY));
        Button resetButton = button("Đặt lại thông số thanh", false);
        reset.addView(resetButton, margin(0, 8, 0, 0));
        resetButton.setOnClickListener(v -> resetBarSettings());
    }

    private void buildSettingsPage(LinearLayout content) {
        addText(content, "Cài đặt & quyền", 24, true, TEXT, 0, 3);
        addText(content, "Kết nối AI, quyền Android và các thiết lập hệ thống.", 13, false, MUTED, 0, 13);

        LinearLayout ai = card(content);
        ai.addView(text("KẾT NỐI GEMINI", 11, true, PRIMARY));
        ai.addView(text(ApiKeyVault.hasKey(this) ? "✓ Đã có khóa API lưu an toàn trên máy." : "Chưa thiết lập khóa Gemini API.", 13, true, ApiKeyVault.hasKey(this) ? GREEN : TEXT), margin(0, 7, 0, 0));
        ai.addView(text("Vinh AI dùng Gemini Developer API. Khóa được mã hóa bằng Android Keystore trên thiết bị. API có thể áp dụng hạn mức hoặc chi phí tùy gói Google AI Studio.", 12, false, MUTED), margin(0, 5, 0, 8));
        Button key = button("Thiết lập / thay khóa API", true);
        ai.addView(key, match());
        key.setOnClickListener(v -> showApiKeyDialog());
        Button chat = button("Mở khung chat Vinh AI", false);
        ai.addView(chat, margin(0, 8, 0, 0));
        chat.setOnClickListener(v -> openVinhAi());

        LinearLayout permissions = card(content);
        permissions.addView(text("QUYỀN ANDROID", 11, true, PRIMARY));
        addPermissionRow(permissions, "Hiển thị trên ứng dụng khác",
                Settings.canDrawOverlays(this) ? "Đã cấp" : "Cần cấp để mở AI nổi và thanh cử chỉ",
                Settings.canDrawOverlays(this), () -> Launcher.overlaySettings(this));
        addPermissionRow(permissions, "Thông báo",
                notificationPermissionGranted() ? "Đã cấp" : "Cần cấp để giữ dịch vụ chạy ổn định",
                notificationPermissionGranted(), () -> requestNotificationPermission());
        addPermissionRow(permissions, "Đọc thông báo",
                isNotificationListenerEnabled() ? "Đã cấp" : "Tùy chọn, chỉ dùng khi bật tính năng đọc",
                isNotificationListenerEnabled(), () -> Launcher.listenerSettings(this));
        addPermissionRow(permissions, "Trợ lý mặc định",
                isOurAssistantDefault() ? "Vinh Circle đang được chọn" : "Chưa chọn Vinh Circle",
                isOurAssistantDefault(), () -> requestAssistantRole());
        Button auto = button("Cài đặt tự khởi động Vivo", false);
        permissions.addView(auto, margin(0, 8, 0, 0));
        auto.setOnClickListener(v -> Launcher.autostart(this));
        Button battery = button("Cài đặt tối ưu pin", false);
        permissions.addView(battery, margin(0, 8, 0, 0));
        battery.setOnClickListener(v -> Launcher.battery(this));

        LinearLayout privacy = card(content);
        privacy.addView(text("QUYỀN RIÊNG TƯ", 11, true, PRIMARY));
        privacy.addView(text("Ứng dụng không tự đọc màn hình nền. Chỉ khi bạn chủ động cấp quyền Trợ năng hoặc chia sẻ màn hình riêng, tính năng đọc nội dung ứng dụng khác mới có thể được triển khai. Không nhập mật khẩu, OTP hoặc thông tin thanh toán vào trợ lý.", 12, false, MUTED), margin(0, 7, 0, 0));

        LinearLayout about = card(content);
        about.addView(text("GIỚI THIỆU", 11, true, PRIMARY));
        about.addView(text("Vinh Circle • Trợ lý Android native", 15, true, TEXT), margin(0, 6, 0, 2));
        about.addView(text("Phiên bản " + BuildConfig.VERSION_NAME + "\nBản quyền của Nông Quang Vinh", 12, false, MUTED));
        Button release = button("Mã nguồn và bản phát hành", false);
        about.addView(release, margin(0, 8, 0, 0));
        release.setOnClickListener(v -> Launcher.release(this));
    }

    private void openVinhAi() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Hãy cấp quyền Hiển thị trên ứng dụng khác rồi nhấn mở Vinh AI.");
            Launcher.overlaySettings(this);
            return;
        }
        AssistantOverlayService.show(this);
    }

    private void launchSystemAssistant() {
        try {
            Intent assist = new Intent(Intent.ACTION_ASSIST);
            assist.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(assist);
        } catch (Exception ex) {
            toast("Chưa có trợ lý mặc định. Hãy chọn Google hoặc Vinh Circle trong cài đặt trợ lý.");
            requestAssistantRole();
        }
    }

    private void requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    toast("Ứng dụng này đã là trợ lý mặc định.");
                    return;
                }
                try {
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                            REQUEST_ASSISTANT_ROLE);
                    return;
                } catch (Exception ignored) {
                    // Open the system voice-assistant selector below.
                }
            }
        }
        Launcher.assistantSettings(this);
    }

    private boolean isOurAssistantDefault() {
        if (Build.VERSION.SDK_INT < 21) return false;
        try {
            return android.service.voice.VoiceInteractionService.isActiveService(
                    this, new ComponentName(this, VinhVoiceInteractionService.class));
        } catch (Exception ex) {
            return false;
        }
    }

    private void showApiKeyDialog() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(8), dp(18), dp(2));
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Dán Gemini API key");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(input, match());
        body.addView(text("Khóa chỉ được lưu mã hóa trên thiết bị. Không gửi khóa API cho người khác.", 11, false, MUTED), margin(0, 8, 0, 0));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kết nối Gemini")
                .setView(body)
                .setPositiveButton("Lưu khóa", null)
                .setNeutralButton("Xóa khóa", (d, w) -> {
                    ApiKeyVault.clear(this);
                    showPage("settings");
                    toast("Đã xóa khóa API khỏi thiết bị.");
                })
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                input.setError("Hãy nhập khóa API.");
                return;
            }
            try {
                ApiKeyVault.save(this, value);
                dialog.dismiss();
                showPage("settings");
                toast("Đã lưu khóa Gemini API.");
            } catch (Exception ex) {
                input.setError("Không lưu được khóa: " + ex.getMessage());
            }
        }));
        dialog.show();
    }

    private void addToggle(LinearLayout parent, String title, String summary,
                           String key, boolean fallback, boolean needsOverlay) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        copy.addView(text(title, 14, true, TEXT));
        copy.addView(text(summary, 11, false, MUTED), margin(0, 2, 8, 0));
        Switch sw = new Switch(this);
        sw.setChecked(Store.get(this).optBoolean(key, fallback));
        row.addView(sw);
        sw.setOnCheckedChangeListener((button, checked) -> {
            if (checked && needsOverlay && !Settings.canDrawOverlays(this)) {
                sw.setChecked(false);
                Launcher.overlaySettings(this);
                toast("Cần cấp quyền hiển thị trên ứng dụng khác trước.");
                return;
            }
            if (checked && needsOverlay && Build.VERSION.SDK_INT >= 33
                    && !notificationPermissionGranted()) {
                sw.setChecked(false);
                requestNotificationPermission();
                return;
            }
            saveSetting(key, checked);
        });
        parent.addView(row, match());
        divider(parent);
    }

    private void addSelect(LinearLayout parent, String title, String key, String fallback) {
        addSelect(parent, title, key, fallback, ACTION_VALUES, ACTION_LABELS);
    }

    private void addSelect(LinearLayout parent, String title, String key, String fallback,
                           String[] values, String[] labels) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        row.addView(text(title, 13, true, TEXT));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String current = Store.get(this).optString(key, fallback);
        int selected = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) selected = i;
        spinner.setSelection(selected, false);
        row.addView(spinner, margin(0, 5, 0, 0));
        parent.addView(row, match());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                if (first) {
                    first = false;
                    return;
                }
                saveSetting(key, values[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        divider(parent);
    }

    private void addSeekSetting(LinearLayout parent, String title, String summary,
                                String key, int min, int max, int fallback, String unit) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, dp(8), 0, dp(7));
        int current = Store.get(this).optInt(key, fallback);
        current = Math.max(min, Math.min(max, current));
        TextView value = text(title + "  •  " + current + unit, 13, true, TEXT);
        group.addView(value);
        group.addView(text(summary, 11, false, MUTED), margin(0, 2, 0, 2));
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(current - min);
        group.addView(seek);
        parent.addView(group, match());
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int latest = seek.getProgress() + min;
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                latest = progress + min;
                value.setText(title + "  •  " + latest + unit);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                saveSetting(key, latest);
            }
        });
        divider(parent);
    }

    private void addPermissionRow(LinearLayout parent, String title, String detail,
                                  boolean granted, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        info.addView(text(title, 13, true, TEXT));
        info.addView(text(detail, 11, false, MUTED), margin(0, 2, 8, 0));
        TextView status = text(granted ? "ĐÃ CẤP" : "MỞ", 10, true, granted ? GREEN : PRIMARY);
        status.setGravity(Gravity.CENTER);
        row.addView(status, new LinearLayout.LayoutParams(dp(65), dp(38)));
        row.setOnClickListener(v -> action.run());
        parent.addView(row, match());
        divider(parent);
    }

    private void addStatusLine(LinearLayout parent, String title, boolean ok) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView name = text(title, 12, false, MUTED);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(ok ? "ĐÃ SẴN SÀNG" : "CHƯA CẤU HÌNH", 10, true, ok ? GREEN : 0xFFFFC27A));
        parent.addView(row, match());
    }

    private void addRowButton(LinearLayout parent, String title, String desc, Runnable action) {
        Button b = button(title + "  ›", false);
        parent.addView(b, margin(0, 8, 0, 0));
        b.setOnClickListener(v -> action.run());
        parent.addView(text(desc, 11, false, MUTED), margin(2, 4, 2, 0));
    }

    private void resetBarSettings() {
        JSONObject s = Store.get(this);
        try {
            s.put("widthDp", 120);
            s.put("thicknessDp", 18);
            s.put("opacity", 85);
            s.put("bottomDp", 0);
            s.put("holdMs", 500);
            s.put("axis", "horizontal");
            s.put("dragMode", false);
            s.put("hideLandscape", true);
            s.put("hideKeyboard", true);
            s.put("theme", "breeze");
            s.put("align", 50);
            Store.replace(this, s.toString());
            BarController.sync(this);
            showPage("bar");
            toast("Đã khôi phục cài đặt thanh.");
        } catch (Exception ex) {
            toast("Không thể đặt lại thông số.");
        }
    }

    private void saveSetting(String key, Object value) {
        JSONObject s = Store.get(this);
        try {
            s.put(key, value);
            Store.replace(this, s.toString());
            if ("barEnabled".equals(key) || key.equals("dragMode") || key.equals("axis")
                    || key.equals("align") || key.equals("widthDp") || key.equals("thicknessDp")
                    || key.equals("opacity") || key.equals("bottomDp") || key.equals("holdMs")
                    || key.equals("theme") || key.equals("hideLandscape") || key.equals("hideKeyboard")
                    || key.endsWith("Action") || key.equals("preferLive")) {
                BarController.sync(this);
            }
        } catch (Exception ex) {
            toast("Không lưu được thiết lập: " + key);
        }
    }

    private boolean hasPackage(String packageName) {
        try {
            return getPackageManager().getLaunchIntentForPackage(packageName) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean notificationPermissionGranted() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isNotificationListenerEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        } else {
            toast("Quyền thông báo đã sẵn sàng.");
        }
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        box.setBackground(round(SURFACE, 18, 0x334B648F));
        box.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        parent.addView(box, params);
        return box;
    }

    private void addText(LinearLayout parent, String value, int size, boolean bold,
                         int color, int top, int bottom) {
        TextView t = text(value, size, bold, color);
        LinearLayout.LayoutParams lp = match();
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        parent.addView(t, lp);
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setLineSpacing(dp(2), 1f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? BG : TEXT);
        b.setMinHeight(dp(46));
        b.setMinimumHeight(dp(46));
        b.setPadding(dp(14), dp(7), dp(14), dp(7));
        b.setBackground(round(primary ? PRIMARY : SURFACE_2, 14,
                primary ? PRIMARY : 0x554D6B9C));
        return b;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private void divider(LinearLayout parent) {
        View v = new View(this);
        v.setBackgroundColor(0x334C6385);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(2);
        parent.addView(v, lp);
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return lp;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}
