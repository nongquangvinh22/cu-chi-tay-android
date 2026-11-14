package vn.vivogemini.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemini assistant shown as a real Android overlay over the currently-used app.
 * The overlay only draws its own panel; it does not capture the screen.
 */
public class AssistantOverlayService extends Service {
    private static final String CHANNEL = "vinh_ai_overlay";
    private static final int NOTIFICATION_ID = 62;
    private static final int BG = 0xF21A1D3D;
    private static final int CARD = 0xFF252B55;
    private static final int TEXT = 0xFFF4F5FF;
    private static final int MUTED = 0xFFB9C2E7;

    private WindowManager windows;
    private View panel;
    private View bubble;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams bubbleParams;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText prompt;
    private EditText keyInput;
    private LinearLayout keyBox;
    private final JSONArray history = new JSONArray();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean busy;

    public static void show(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            Launcher.overlaySettings(context);
            return;
        }
        Intent intent = new Intent(context, AssistantOverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windows = getSystemService(WindowManager.class);
        startInForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            Launcher.overlaySettings(this);
            return START_NOT_STICKY;
        }
        expandPanel();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        worker.shutdownNow();
        removeOverlay(panel);
        removeOverlay(bubble);
        panel = null;
        bubble = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void expandPanel() {
        if (panel != null) {
            prompt.requestFocus();
            return;
        }
        removeOverlay(bubble);
        bubble = null;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(13), dp(11), dp(13), dp(12));
        root.setBackground(round(BG, 22, 0xCC7F90FF));
        root.setElevation(dp(18));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header, matchWrap());

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        TextView title = label("✦  VINH AI", 18, true, TEXT);
        titleGroup.addView(title);
        TextView subtitle = label("Trợ lý nổi trên màn hình hiện tại", 10, false, MUTED);
        titleGroup.addView(subtitle);
        header.addView(titleGroup);

        Button minimize = miniButton("Ẩn");
        header.addView(minimize, margin(4, 0, 0, 0));
        minimize.setOnClickListener(v -> minimizePanel());
        Button close = miniButton("×");
        header.addView(close, margin(6, 0, 0, 0));
        close.setOnClickListener(v -> stopSelf());

        keyBox = new LinearLayout(this);
        keyBox.setOrientation(LinearLayout.HORIZONTAL);
        keyBox.setGravity(Gravity.CENTER_VERTICAL);
        keyBox.setPadding(dp(8), dp(6), dp(8), dp(6));
        keyBox.setBackground(round(0xFF171B39, 12, 0x665F75E8));
        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextColor(TEXT);
        keyInput.setHintTextColor(MUTED);
        keyInput.setTextSize(12);
        keyInput.setHint("Dán Gemini API key");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setBackgroundColor(Color.TRANSPARENT);
        keyBox.addView(keyInput, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button saveKey = miniButton("Lưu key");
        keyBox.addView(saveKey, margin(5, 0, 0, 0));
        saveKey.setOnClickListener(v -> saveApiKey());
        root.addView(keyBox, margin(0, 9, 0, 0));
        keyBox.setVisibility(ApiKeyVault.hasKey(this) ? View.GONE : View.VISIBLE);

        TextView keyHint = label("Khóa được mã hóa và lưu trong Android Keystore trên máy.", 10, false, MUTED);
        root.addView(keyHint, margin(2, 3, 2, 6));
        keyHint.setVisibility(ApiKeyVault.hasKey(this) ? View.GONE : View.VISIBLE);

        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(8);
        scrollParams.bottomMargin = dp(8);
        root.addView(scroll, scrollParams);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(2), dp(2), dp(2), dp(2));
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        if (history.length() == 0) {
            appendMessage("Vinh AI",
                    "Tôi đang nổi ngay trên ứng dụng bạn sử dụng. Hãy hỏi tôi, ví dụ: "
                    + "“Mở YouTube”, “Đặt báo thức 6 giờ 30” hoặc “Hẹn giờ 10 phút”. "
                    + "Tôi sẽ hỏi xác nhận trước khi thao tác.");
        }

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM);
        root.addView(composer, matchWrap());

        prompt = new EditText(this);
        prompt.setTextColor(TEXT);
        prompt.setHintTextColor(MUTED);
        prompt.setHint("Nhập yêu cầu cho Vinh AI…");
        prompt.setTextSize(14);
        prompt.setMinLines(1);
        prompt.setMaxLines(4);
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setImeOptions(EditorInfo.IME_ACTION_SEND);
        prompt.setPadding(dp(12), dp(10), dp(12), dp(10));
        prompt.setBackground(round(0xFF11152E, 14, 0x886E7DDE));
        composer.addView(prompt, new LinearLayout.LayoutParams(0, -2, 1));

        Button send = miniButton("Gửi");
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(66), dp(48));
        sendParams.leftMargin = dp(7);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> sendPrompt());
        prompt.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendPrompt();
                return true;
            }
            return false;
        });

        TextView footer = label("Bản quyền Nông Quang Vinh  •  Google Gemini API", 9, false, MUTED);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, margin(0, 6, 0, 0));

        panel = root;
        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight(),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        panelParams.y = dp(10);
        panelParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
        panelParams.setTitle("Vinh AI overlay");

        try {
            windows.addView(panel, panelParams);
            prompt.requestFocus();
        } catch (Exception ex) {
            panel = null;
            toast("Không thể hiển thị Vinh AI nổi. Hãy kiểm tra quyền Hiển thị trên ứng dụng khác.");
            stopSelf();
        }
    }

    private void minimizePanel() {
        removeOverlay(panel);
        panel = null;
        LinearLayout mini = new LinearLayout(this);
        mini.setGravity(Gravity.CENTER);
        mini.setBackground(round(0xF2244FC9, 28, 0xEEB5C3FF));
        mini.setElevation(dp(16));
        TextView title = label("✦ AI", 15, true, Color.WHITE);
        mini.addView(title);
        mini.setOnClickListener(v -> expandPanel());
        mini.setOnLongClickListener(v -> {
            stopSelf();
            return true;
        });
        bubble = mini;
        bubbleParams = new WindowManager.LayoutParams(
                dp(66), dp(54),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.BOTTOM | Gravity.END;
        bubbleParams.x = dp(14);
        bubbleParams.y = dp(95);
        try {
            windows.addView(bubble, bubbleParams);
        } catch (Exception ex) {
            bubble = null;
            stopSelf();
        }
    }

    private int panelHeight() {
        int display = getResources().getDisplayMetrics().heightPixels;
        return Math.min(dp(640), Math.max(dp(380), Math.round(display * 0.70f)));
    }

    private void saveApiKey() {
        String apiKey = keyInput.getText().toString().trim();
        if (apiKey.isEmpty()) {
            toast("Hãy dán khóa Gemini API.");
            return;
        }
        try {
            ApiKeyVault.save(this, apiKey);
            keyInput.setText("");
            keyBox.setVisibility(View.GONE);
            toast("Đã lưu khóa Gemini API an toàn.");
        } catch (Exception ex) {
            toast("Không lưu được khóa API: " + ex.getMessage());
        }
    }

    private void sendPrompt() {
        if (busy || prompt == null) return;
        String text = prompt.getText().toString().trim();
        if (text.isEmpty()) return;
        String apiKey;
        try {
            apiKey = ApiKeyVault.load(this);
        } catch (Exception ex) {
            appendMessage("Vinh AI", "Không đọc được khóa API. Hãy lưu lại khóa trong phần cài đặt.");
            return;
        }
        if (apiKey.isEmpty()) {
            if (keyBox != null) keyBox.setVisibility(View.VISIBLE);
            appendMessage("Vinh AI", "Bạn cần dán Gemini API key ở phía trên một lần để sử dụng AI.");
            return;
        }

        prompt.setText("");
        appendMessage("Bạn", text);
        try {
            history.put(new JSONObject().put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", text))));
            while (history.length() > 24) history.remove(0);
        } catch (Exception ignored) {}
        setBusy(true);
        final String credential = apiKey;
        worker.execute(() -> {
            try {
                JSONObject answer = GeminiApi.ask(credential, history);
                mainHandler.post(() -> handleAnswer(answer));
            } catch (Exception ex) {
                String detail = ex.getMessage() == null ? "Lỗi kết nối." : ex.getMessage();
                mainHandler.post(() -> appendMessage("Lỗi", friendlyError(detail)));
            } finally {
                mainHandler.post(() -> setBusy(false));
            }
        });
    }

    private void handleAnswer(JSONObject result) {
        if ("function".equals(result.optString("kind"))) {
            JSONObject call = result.optJSONObject("call");
            JSONObject args = call == null ? new JSONObject() : call.optJSONObject("args");
            if (args == null) args = new JSONObject();
            String name = call == null ? "" : call.optString("name", "");
            showActionConfirmation(name, args);
            return;
        }
        String answer = result.optString("text", "").trim();
        if (answer.isEmpty()) answer = "Gemini chưa trả lời. Bạn thử hỏi lại nhé.";
        appendMessage("Vinh AI", answer);
        try {
            history.put(new JSONObject().put("role", "model")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", answer))));
        } catch (Exception ignored) {}
    }

    private void showActionConfirmation(String name, JSONObject args) {
        String description = describeAction(name, args);
        if (description == null) {
            appendMessage("Vinh AI", "Lệnh này chưa được hỗ trợ nên tôi sẽ không thực hiện.");
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(11), dp(10), dp(11), dp(10));
        card.setBackground(round(CARD, 15, 0x886E7DDE));
        TextView detail = label("Cần bạn xác nhận\n" + description, 13, true, TEXT);
        card.addView(detail);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button allow = miniButton("Cho phép");
        Button cancel = miniButton("Hủy");
        controls.addView(allow, new LinearLayout.LayoutParams(0, dp(42), 1));
        controls.addView(cancel, margin(7, 0, 0, 0));
        card.addView(controls, margin(0, 8, 0, 0));
        messages.addView(card, margin(0, 0, 0, 8));
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        allow.setOnClickListener(v -> {
            allow.setEnabled(false);
            cancel.setEnabled(false);
            String outcome = executeAction(name, args);
            detail.setText("Vinh AI\n" + outcome);
            controls.setVisibility(View.GONE);
            try {
                history.put(new JSONObject().put("role", "model")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", outcome))));
            } catch (Exception ignored) {}
        });
        cancel.setOnClickListener(v -> {
            detail.setText("Đã hủy. Không có thao tác nào được thực hiện.");
            controls.setVisibility(View.GONE);
        });
    }

    private String describeAction(String name, JSONObject args) {
        switch (name) {
            case "open_app":
                return "Mở ứng dụng " + args.optString("app_name", "");
            case "set_alarm":
                return String.format(Locale.getDefault(), "Mở Đồng hồ để đặt báo thức lúc %02d:%02d",
                        number(args, "hour", -1), number(args, "minute", -1));
            case "set_timer":
                return "Mở Đồng hồ để bắt đầu hẹn giờ " + number(args, "seconds", 0) + " giây";
            case "open_gemini":
                return "Mở ứng dụng Gemini";
            case "open_circle_search":
                return "Thử mở Circle to Search của Google";
            case "open_translate":
                return "Mở Google Dịch";
            case "translate_text":
                return "Chuyển văn bản người dùng cung cấp sang Google Dịch";
            default:
                return null;
        }
    }

    private String executeAction(String name, JSONObject args) {
        try {
            switch (name) {
                case "open_app":
                    return openInstalledApp(args.optString("app_name", ""));
                case "set_alarm": {
                    int hour = number(args, "hour", -1);
                    int minute = number(args, "minute", -1);
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        return "Giờ hoặc phút không hợp lệ.";
                    }
                    Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM)
                            .putExtra(AlarmClock.EXTRA_HOUR, hour)
                            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("label", "Vinh Circle"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(alarm);
                    return "Đã mở Đồng hồ để bạn xác nhận báo thức.";
                }
                case "set_timer": {
                    int seconds = number(args, "seconds", 0);
                    if (seconds < 1 || seconds > 86400) return "Thời lượng hẹn giờ phải từ 1 đến 86400 giây.";
                    Intent timer = new Intent(AlarmClock.ACTION_SET_TIMER)
                            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("label", "Vinh Circle"))
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(timer);
                    return "Đã mở Đồng hồ để bạn xác nhận hẹn giờ.";
                }
                case "open_gemini":
                    Launcher.gemini(this);
                    return "Đã gửi lệnh mở Gemini.";
                case "open_circle_search":
                    Launcher.circle(this);
                    return "Đã gửi lệnh mở Circle to Search. Tính năng phụ thuộc Google app và OriginOS.";
                case "open_translate":
                    Launcher.translate(this);
                    return "Đã gửi lệnh mở Google Dịch.";
                case "translate_text": {
                    String text = args.optString("text", "");
                    if (text.trim().isEmpty()) return "Không có văn bản để dịch.";
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, text);
                    send.setPackage("com.google.android.apps.translate");
                    startActivity(send);
                    return "Đã chuyển văn bản sang Google Dịch.";
                }
                default:
                    return "Lệnh không được hỗ trợ.";
            }
        } catch (ActivityNotFoundException ex) {
            return "Không tìm thấy ứng dụng phù hợp trên máy.";
        } catch (Exception ex) {
            return "Không thực hiện được: " + (ex.getMessage() == null ? "lỗi hệ thống" : ex.getMessage());
        }
    }

    private String openInstalledApp(String query) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) return "Bạn chưa nêu tên ứng dụng.";
        PackageManager pm = getPackageManager();
        Intent launchers = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launchers, 0);
        ResolveInfo found = null;
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null) continue;
            String label = normalize(info.loadLabel(pm).toString());
            String pkg = normalize(info.activityInfo.packageName);
            if (label.equals(wanted) || label.contains(wanted) || wanted.contains(label)) {
                found = info;
                break;
            }
            if (found == null && pkg.contains(wanted.replace(" ", ""))) found = info;
        }
        if (found == null) return "Không tìm thấy ứng dụng " + query + ".";
        Intent open = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        open.setClassName(found.activityInfo.packageName, found.activityInfo.name);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(open);
        return "Đã mở " + found.loadLabel(pm) + ".";
    }

    private String normalize(String value) {
        String raw = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD);
        return raw.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').trim();
    }

    private int number(JSONObject object, String key, int fallback) {
        Object value = object.opt(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void appendMessage(String who, String text) {
        if (messages == null) return;
        TextView message = label(who + "\n" + text, 13, who.equals("Vinh AI"), TEXT);
        message.setPadding(dp(11), dp(9), dp(11), dp(9));
        message.setBackground(round(who.equals("Bạn") ? 0xFF34417D : CARD, 14, 0x335F75E8));
        messages.addView(message, margin(0, 0, 0, 7));
        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        if (prompt != null) prompt.setEnabled(!busy);
    }

    private String friendlyError(String detail) {
        if (detail.contains("API_KEY_INVALID") || detail.contains("API key not valid")) {
            return "Khóa API không hợp lệ. Hãy kiểm tra trong Google AI Studio.";
        }
        if (detail.contains("429") || detail.toLowerCase(Locale.ROOT).contains("quota")) {
            return "Gemini API đã chạm hạn mức. Hãy kiểm tra quota tài khoản.";
        }
        return "Không kết nối được Gemini: " + detail;
    }

    private void startInForeground() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Vinh AI nổi", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Được dùng trong khi khung chat Gemini nổi trên ứng dụng khác.");
            manager.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(this, 62,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Vinh AI đang nổi")
                .setContentText("Chạm thanh cử chỉ để mở lại trợ lý trên màn hình.")
                .setSmallIcon(R.drawable.ic_stat)
                .setOngoing(true)
                .setContentIntent(open)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private TextView label(String text, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button miniButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinWidth(dp(48));
        button.setPadding(dp(7), 0, dp(7), 0);
        button.setBackground(round(0xFF4852BC, 13, 0x996E8BFF));
        return button;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void removeOverlay(View view) {
        if (view == null || windows == null) return;
        try {
            windows.removeView(view);
        } catch (Exception ignored) {
            // Already detached.
        }
    }

    private void toast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
}
