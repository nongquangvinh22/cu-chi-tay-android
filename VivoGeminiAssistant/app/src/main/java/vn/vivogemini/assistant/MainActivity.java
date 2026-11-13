package vn.vivogemini.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Privacy-first Gemini launcher for Vivo OriginOS. */
public class MainActivity extends Activity {
    public static final String ACTION_OPEN_GEMINI = "vn.vivogemini.assistant.OPEN_GEMINI";
    private static final int REQUEST_ASSISTANT_ROLE = 4101;
    private static final String GEMINI_PACKAGE = "com.google.android.apps.bard";
    private static final String GEMINI_URL = "https://gemini.google.com/app";
    private static final String LENS_URL = "https://lens.google.com/";
    private TextView assistantStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        applySystemBars();
        if (isAssistantLaunch(getIntent())) {
            openGemini();
            finish();
            return;
        }
        setContentView(buildScreen());
        refreshAssistantStatus();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (isAssistantLaunch(intent)) openGemini();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ASSISTANT_ROLE) {
            refreshAssistantStatus();
            toast(isAssistantRoleHeld() ? "Đã đặt làm trợ lý mặc định." : "Chưa thay đổi trợ lý mặc định.");
        }
    }

    private boolean isAssistantLaunch(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        return ACTION_OPEN_GEMINI.equals(action) || Intent.ACTION_ASSIST.equals(action)
                || Intent.ACTION_VOICE_ASSIST.equals(action);
    }

    private void applySystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(245, 247, 252));
        w.setNavigationBarColor(Color.rgb(245, 247, 252));
        if (Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
        }
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 252));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView brand = text("VIVO GEMINI ASSISTANT", 12, 0xFF4778E8, true);
        brand.setLetterSpacing(0.12f);
        page.addView(brand);
        gap(page, 4);
        TextView copyright = text("Bản quyền của Nông Quang Vinh", 11, 0xFF98A2B3, false);
        page.addView(copyright);
        gap(page, 10);
        page.addView(text("Trợ lý Google\ncho OriginOS", 29, 0xFF17233B, true));
        gap(page, 8);
        page.addView(text("Trung tâm mở Gemini và thiết lập các lối tắt cho Vivo X200 Pro.",
                15, 0xFF667085, false));
        gap(page, 20);

        LinearLayout gemini = card(page);
        gemini.addView(text("TRỢ LÝ AI", 11, 0xFF5676C8, true));
        gap(gemini, 7);
        gemini.addView(text("Gemini", 25, 0xFF18233A, true));
        gap(gemini, 5);
        gemini.addView(text("Mở ứng dụng Gemini hoặc trang Gemini trên web.", 14, 0xFF667085, false));
        gap(gemini, 14);
        gemini.addView(button("Mở Gemini  ✦", 0xFF4778E8, Color.WHITE, v -> openGemini()));

        LinearLayout assistant = card(page);
        assistant.addView(text("THAO TÁC HỆ THỐNG", 11, 0xFF5676C8, true));
        gap(assistant, 7);
        assistant.addView(text("Trợ lý mặc định", 18, 0xFF18233A, true));
        gap(5);
        assistant.addView(text("Ứng dụng yêu cầu vai trò trợ lý Android. Giữ nút nguồn vẫn phụ thuộc OriginOS.",
                13, 0xFF667085, false));
        gap(9);
        assistantStatus = text("Đang kiểm tra trạng thái…", 13, 0xFF4778E8, true);
        assistant.addView(assistantStatus);
        gap(10);
        assistant.addView(button("Đặt làm trợ lý mặc định", 0xFFEAF0FF, 0xFF315FC7,
                v -> requestAssistantRole()));
        gap(8);
        assistant.addView(button("Thêm nút Gemini vào Cài đặt nhanh", 0xFFF0F3F8,
                0xFF27364F, v -> requestQuickSettingsTile()));

        LinearLayout search = card(page);
        search.addView(text("TÌM KIẾM NỘI DUNG MÀN HÌNH", 11, 0xFF5676C8, true));
        gap(search, 7);
        search.addView(text("Dùng Google Lens", 18, 0xFF18233A, true));
        gap(search, 5);
        search.addView(text("Bản đầu tiên hướng dẫn chụp ảnh màn hình rồi chia sẻ thủ công sang Lens; chưa thay thế Circle to Search nguyên bản.",
                13, 0xFF667085, false));
        gap(search, 10);
        search.addView(button("Xem hướng dẫn tìm kiếm màn hình", 0xFFF0F3F8,
                0xFF27364F, v -> showLensGuide()));
        gap(search, 8);
        search.addView(button("Mở Google Lens", 0xFFF0F3F8, 0xFF27364F, v -> openLens()));

        LinearLayout privacy = card(page);
        privacy.addView(text("PIN & QUYỀN RIÊNG TƯ", 11, 0xFF5676C8, true));
        gap(privacy, 7);
        privacy.addView(text("Không chạy nền liên tục", 18, 0xFF18233A, true));
        gap(privacy, 5);
        privacy.addView(text("Không xin quyền micro, Trợ năng, overlay, VPN hoặc chụp màn hình tự động.",
                13, 0xFF667085, false));
        gap(privacy, 10);
        privacy.addView(button("Mở thông tin ứng dụng & pin", 0xFFF0F3F8, 0xFF27364F,
                v -> openAppSettings()));

        gap(page, 18);
        TextView footer = text("BẢN 0.1.0 ALPHA  •  ƯU TIÊN AN TOÀN\n"
                + "Bản quyền của Nông Quang Vinh\n"
                + "Không phải ứng dụng chính thức của Google hoặc vivo.", 11, 0xFF98A2B3, false);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(3), 1f);
        page.addView(footer);
        return scroll;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(17), dp(17), dp(17), dp(17));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0xFFE8ECF3);
        box.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) box.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        parent.addView(box, lp);
        return box;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private View button(String label, int bg, int fg, View.OnClickListener listener) {
        TextView view = text(label, 14, fg, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(bg);
        shape.setCornerRadius(dp(14));
        view.setBackground(shape);
        view.setMinHeight(dp(48));
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(listener);
        return view;
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openGemini() {
        try {
            Intent app = getPackageManager().getLaunchIntentForPackage(GEMINI_PACKAGE);
            if (app != null) startActivity(app);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GEMINI_URL)));
        } catch (ActivityNotFoundException e) {
            toast("Không tìm thấy ứng dụng hoặc trình duyệt để mở Gemini.");
        } catch (Exception e) {
            toast("Không thể mở Gemini: " + e.getClass().getSimpleName());
        }
    }

    private void requestAssistantRole() {
        if (Build.VERSION.SDK_INT < 29) {
            try { startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)); }
            catch (ActivityNotFoundException e) { toast("Không có trang cài đặt trợ lý tương thích."); }
            return;
        }
        RoleManager roles = getSystemService(RoleManager.class);
        if (roles == null || !roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
            toast("OriginOS không cho ứng dụng bên thứ ba đăng ký trợ lý mặc định.");
            return;
        }
        if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
            toast("Ứng dụng này đã là trợ lý mặc định.");
            refreshAssistantStatus();
            return;
        }
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                REQUEST_ASSISTANT_ROLE);
    }

    private boolean isAssistantRoleHeld() {
        if (Build.VERSION.SDK_INT < 29) return false;
        RoleManager roles = getSystemService(RoleManager.class);
        return roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)
                && roles.isRoleHeld(RoleManager.ROLE_ASSISTANT);
    }

    private void refreshAssistantStatus() {
        if (assistantStatus == null) return;
        if (Build.VERSION.SDK_INT < 29) {
            assistantStatus.setText("Kiểm tra trợ lý trong phần Cài đặt Android.");
        } else {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles == null || !roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT))
                assistantStatus.setText("Thiết bị không cho đăng ký vai trò trợ lý Android.");
            else assistantStatus.setText(isAssistantRoleHeld()
                    ? "Đã là trợ lý mặc định trên Android." : "Chưa phải trợ lý mặc định.");
        }
    }

    private void requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < 33) {
            new AlertDialog.Builder(this).setTitle("Thêm nút Gemini")
                    .setMessage("Vuốt bảng Cài đặt nhanh xuống hai lần, chọn Sửa, tìm ô Gemini rồi kéo vào khu vực đang dùng.")
                    .setPositiveButton("Đã hiểu", null).show();
            return;
        }
        try {
            StatusBarManager manager = getSystemService(StatusBarManager.class);
            if (manager == null) { toast("Không truy cập được Cài đặt nhanh."); return; }
            manager.requestAddTileService(new ComponentName(this, GeminiTileService.class),
                    "Gemini", android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile_gemini),
                    getMainExecutor(), result -> toast("Kết quả thêm nút Gemini: " + result));
        } catch (Exception e) {
            toast("OriginOS có thể giới hạn thêm nút tự động. Hãy thêm thủ công trong Sửa Cài đặt nhanh.");
        }
    }

    private void showLensGuide() {
        String message = "1. Mở nội dung bạn muốn tìm.\n\n"
                + "2. Chụp ảnh màn hình bằng nút nguồn + giảm âm lượng.\n\n"
                + "3. Mở ảnh trong Thư viện và chọn Chia sẻ.\n\n"
                + "4. Chọn Google Lens hoặc Google.\n\n"
                + "Đây là cách thủ công, không phải Circle to Search nguyên bản.";
        new AlertDialog.Builder(this).setTitle("Tìm kiếm nội dung trên màn hình")
                .setMessage(message).setPositiveButton("Mở Lens", (d, w) -> openLens())
                .setNegativeButton("Đóng", null).show();
    }

    private void openLens() {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LENS_URL))); }
        catch (ActivityNotFoundException e) { toast("Không tìm thấy trình duyệt để mở Google Lens."); }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            toast("Không mở được thông tin ứng dụng.");
        }
    }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
