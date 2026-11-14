package vn.vivogemini.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssistantActivity extends Activity {
    private static final int REQUEST_VOICE = 510;
    private static final int BG = Color.rgb(13, 15, 39);
    private static final int PANEL = Color.rgb(28, 31, 73);
    private static final int TEXT = Color.rgb(244, 245, 255);
    private static final int MUTED = Color.rgb(181, 190, 225);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final JSONArray history = new JSONArray();
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText prompt;
    private EditText keyInput;
    private TextView keyState;
    private Button send;
    private TextToSpeech tts;
    private boolean busy;

    public static void open(Context context) {
        Intent intent = new Intent(context, AssistantActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        updateKeyState();
        append("Vinh AI", "Xin chào! Tôi là trợ lý Gemini tích hợp trực tiếp trong Vinh Circle. "
                + "Tôi có thể trò chuyện, mở ứng dụng và mở màn hình đặt báo thức/hẹn giờ. "
                + "Mỗi thao tác hệ thống đều cần bạn xác nhận.");
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.setLanguage(new Locale("vi", "VN"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF11102F, 0xFF172F55, 0xFF42236D}));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("✦  VINH AI");
        title.setTextColor(TEXT);
        title.setTextSize(23);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Gemini trực tiếp • Không mở trình duyệt\nBản quyền Nông Quang Vinh");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(12);
        root.addView(subtitle, margin(0, 0, 0, 10));

        LinearLayout keyCard = card();
        root.addView(keyCard, margin(0, 0, 0, 10));
        keyCard.addView(label("KẾT NỐI GOOGLE GEMINI API"));
        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextColor(TEXT);
        keyInput.setHintTextColor(MUTED);
        keyInput.setHint("Dán khóa API từ Google AI Studio");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setTextSize(13);
        keyInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        keyInput.setBackground(round(0xFF11152E, 12));
        keyCard.addView(keyInput, margin(0, 8, 0, 0));

        LinearLayout keyActions = new LinearLayout(this);
        keyActions.setGravity(Gravity.CENTER_VERTICAL);
        keyCard.addView(keyActions, margin(0, 6, 0, 0));
        Button save = button("Lưu khóa an toàn");
        keyActions.addView(save, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button clear = button("Xóa khóa");
        keyActions.addView(clear, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        clear.setPadding(dp(8), 0, dp(8), 0);
        save.setOnClickListener(v -> saveKey());
        clear.setOnClickListener(v -> {
            ApiKeyVault.clear(this);
            keyInput.setText("");
            updateKeyState();
            toast("Đã xóa khóa API trên điện thoại.");
        });

        keyState = label("Khóa API chưa được lưu.");
        keyState.setTextColor(MUTED);
        keyCard.addView(keyState, margin(0, 5, 0, 0));

        TextView privacy = new TextView(this);
        privacy.setText("Khóa được mã hóa bằng Android Keystore trên thiết bị. Nội dung trò chuyện được gửi đến Google Gemini API để xử lý.");
        privacy.setTextColor(MUTED);
        privacy.setTextSize(11);
        keyCard.addView(privacy, margin(0, 4, 0, 0));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(round(0x66101430, 18));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(10), dp(10), dp(10), dp(10));
        scroll.addView(messages);

        prompt = new EditText(this);
        prompt.setTextColor(TEXT);
        prompt.setHintTextColor(MUTED);
        prompt.setHint("Ví dụ: Mở YouTube hoặc đặt báo thức lúc 6:30...");
        prompt.setTextSize(14);
        prompt.setMinLines(2);
        prompt.setMaxLines(4);
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        prompt.setPadding(dp(12), dp(10), dp(12), dp(10));
        prompt.setBackground(round(0xFF151936, 16));
        root.addView(prompt, margin(0, 10, 0, 8));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(controls);
        Button mic = button("🎙 Nói");
        controls.addView(mic, new LinearLayout.LayoutParams(0, dp(48), 1));
        send = button("Gửi Gemini  ➤");
        controls.addView(send, new LinearLayout.LayoutParams(0, dp(48), 2));
        mic.setOnClickListener(v -> startVoice());
        send.setOnClickListener(v -> sendPrompt());
    }

    private void saveKey() {
        String value = keyInput.getText().toString().trim();
        if (value.isEmpty()) {
            toast("Hãy nhập khóa Gemini API trước.");
            return;
        }
        try {
            ApiKeyVault.save(this, value);
            keyInput.setText("");
            updateKeyState();
            toast("Đã lưu khóa Gemini API an toàn trên máy.");
        } catch (Exception ex) {
            toast("Không lưu được khóa: " + ex.getMessage());
        }
    }

    private void updateKeyState() {
        if (keyState == null) return;
        keyState.setText(ApiKeyVault.hasKey(this)
                ? "✓ Đã lưu khóa API trong bộ nhớ bảo vệ của Android."
                : "Khóa API chưa được lưu.");
    }

    private void sendPrompt() {
        if (busy) return;
        String message = prompt.getText().toString().trim();
        if (message.isEmpty()) return;
        String key;
        try {
            key = ApiKeyVault.load(this);
        } catch (Exception ex) {
            toast("Không đọc được khóa API đã lưu. Hãy xóa và lưu lại.");
            return;
        }
        if (key.isEmpty()) {
            toast("Hãy lưu khóa Gemini API trước khi trò chuyện.");
            return;
        }

        prompt.setText("");
        append("Bạn", message);
        try {
            history.put(new JSONObject().put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", message))));
        } catch (Exception ignored) {}
        setBusy(true);
        worker.execute(() -> {
            try {
                JSONObject result = GeminiApi.ask(key, history);
                runOnUiThread(() -> handleResponse(result));
            } catch (Exception ex) {
                String error = ex.getMessage() == null ? "Lỗi kết nối Gemini." : ex.getMessage();
                runOnUiThread(() -> append("Lỗi", friendlyError(error)));
            } finally {
                runOnUiThread(() -> setBusy(false));
            }
        });
    }

    private void handleResponse(JSONObject result) {
        if ("function".equals(result.optString("kind"))) {
            JSONObject call = result.optJSONObject("call");
            if (call == null) {
                append("Vinh AI", "Gemini trả về một yêu cầu không hợp lệ.");
                return;
            }
            JSONObject args = call.optJSONObject("args");
            if (args == null) args = new JSONObject();
            showConfirmation(call.optString("name", ""), args);
        } else {
            String answer = result.optString("text", "").trim();
            if (answer.isEmpty()) answer = "Gemini chưa tạo câu trả lời. Bạn thử diễn đạt lại nhé.";
            append("Vinh AI", answer);
            if (tts != null) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "vinh-ai-answer");
            try {
                history.put(new JSONObject().put("role", "model")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", answer))));
            } catch (Exception ignored) {}
        }
    }

    private void showConfirmation(String function, JSONObject args) {
        String description = describe(function, args);
        if (description == null) {
            append("Vinh AI", "Tôi không nhận diện được thao tác này nên sẽ không thực hiện.");
            return;
        }
        append("Vinh AI", "Tôi đề xuất: " + description);
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thao tác")
                .setMessage(description + "\n\nBạn có muốn thực hiện không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    String outcome = execute(function, args);
                    append("Hệ thống", outcome);
                    try {
                        history.put(new JSONObject().put("role", "model")
                                .put("parts", new JSONArray().put(new JSONObject().put("text", outcome))));
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Hủy", (dialog, which) -> append("Hệ thống", "Đã hủy. Không có thao tác nào được thực hiện."))
                .show();
    }

    private String describe(String function, JSONObject args) {
        switch (function) {
            case "open_app":
                return "Mở ứng dụng: " + args.optString("app_name", "ứng dụng");
            case "set_alarm":
                return String.format(Locale.getDefault(), "Mở Đồng hồ để đặt báo thức lúc %02d:%02d",
                        number(args, "hour", -1), number(args, "minute", -1));
            case "set_timer":
                return "Mở Đồng hồ để bắt đầu hẹn giờ " + number(args, "seconds", 0) + " giây";
            case "open_gemini":
                return "Mở ứng dụng Gemini chính thức trên máy";
            case "open_circle_search":
                return "Thử mở Circle to Search bằng ứng dụng Google trên máy";
            case "open_translate":
                return "Mở ứng dụng Google Dịch";
            case "translate_text":
                return "Chuyển văn bản đã cung cấp sang ứng dụng Google Dịch";
            default:
                return null;
        }
    }

    private String execute(String function, JSONObject args) {
        try {
            switch (function) {
                case "open_app":
                    return openApp(args.optString("app_name", ""));
                case "set_alarm": {
                    int hour = number(args, "hour", -1);
                    int minute = number(args, "minute", -1);
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        return "Giờ hoặc phút không hợp lệ. Hãy yêu cầu lại, ví dụ 6 giờ 30.";
                    }
                    Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM)
                            .putExtra(AlarmClock.EXTRA_HOUR, hour)
                            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("label", "Vinh Circle"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(alarm);
                    return "Đã mở giao diện Đồng hồ để bạn xác nhận báo thức.";
                }
                case "set_timer": {
                    int seconds = number(args, "seconds", 0);
                    if (seconds <= 0 || seconds > 86400) return "Thời lượng hẹn giờ phải từ 1 đến 86400 giây.";
                    Intent timer = new Intent(AlarmClock.ACTION_SET_TIMER)
                            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("label", "Vinh Circle"))
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(timer);
                    return "Đã mở giao diện Đồng hồ để bạn xác nhận hẹn giờ.";
                }
                case "open_gemini":
                    Launcher.gemini(this);
                    return "Đã gửi yêu cầu mở ứng dụng Gemini.";
                case "open_circle_search":
                    Launcher.circle(this);
                    return "Đã gửi yêu cầu mở Circle to Search. Nếu không hiện, hãy dùng cử chỉ hệ thống Google được hỗ trợ trên máy.";
                case "open_translate":
                    Launcher.translate(this);
                    return "Đã gửi yêu cầu mở Google Dịch.";
                case "translate_text":
                    return shareToTranslate(args.optString("text", ""));
                default:
                    return "Lệnh không được hỗ trợ.";
            }
        } catch (ActivityNotFoundException ex) {
            return "Máy chưa có ứng dụng phù hợp để thực hiện lệnh này.";
        } catch (Exception ex) {
            return "Không thực hiện được: " + (ex.getMessage() == null ? "lỗi hệ thống" : ex.getMessage());
        }
    }

    private String openApp(String query) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) return "Bạn chưa nêu tên ứng dụng.";
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
        ResolveInfo match = null;
        for (ResolveInfo app : apps) {
            String label = normalize(app.loadLabel(pm).toString());
            String pkg = normalize(app.activityInfo.packageName);
            if (label.equals(wanted) || label.contains(wanted) || wanted.contains(label)) {
                match = app;
                break;
            }
            if (match == null && pkg.contains(wanted.replace(" ", ""))) match = app;
        }
        if (match == null) return "Không tìm thấy ứng dụng " + query + ". Hãy kiểm tra ứng dụng đã được cài chưa.";
        Intent open = new Intent(Intent.ACTION_MAIN);
        open.addCategory(Intent.CATEGORY_LAUNCHER);
        open.setClassName(match.activityInfo.packageName, match.activityInfo.name);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(open);
        return "Đã mở " + match.loadLabel(pm) + ".";
    }

    private String shareToTranslate(String text) {
        if (text.trim().isEmpty()) return "Không có văn bản để dịch.";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        send.setPackage("com.google.android.apps.translate");
        try {
            startActivity(send);
            return "Đã chuyển văn bản sang Google Dịch.";
        } catch (ActivityNotFoundException ex) {
            Launcher.translate(this);
            return "Chưa tìm thấy trình nhận chia sẻ của Google Dịch; đã thử mở ứng dụng.";
        }
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói yêu cầu cho Vinh AI");
        try {
            startActivityForResult(intent, REQUEST_VOICE);
        } catch (ActivityNotFoundException ex) {
            toast("Máy chưa có dịch vụ nhận dạng giọng nói.");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                prompt.setText(results.get(0));
                prompt.setSelection(prompt.length());
                sendPrompt();
            }
        }
    }

    private void append(String who, String message) {
        TextView row = new TextView(this);
        row.setText(who + "\n" + message);
        row.setTextColor(TEXT);
        row.setTextSize(14);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(round(who.equals("Bạn") ? 0xFF303C7B : PANEL, 14));
        messages.addView(row, margin(0, 0, 0, 8));
        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void setBusy(boolean value) {
        busy = value;
        if (send != null) {
            send.setEnabled(!value);
            send.setText(value ? "Đang hỏi Gemini…" : "Gửi Gemini  ➤");
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(round(0xDD1C2149, 18));
        return box;
    }

    private TextView label(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(TEXT);
        text.setTextSize(12);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        return text;
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setBackground(round(0xFF4B50D5, 14));
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), 0x665F80FF);
        return shape;
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private String normalize(String value) {
        String raw = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD);
        return raw.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').trim();
    }

    private String friendlyError(String error) {
        if (error.contains("API_KEY_INVALID") || error.contains("API key not valid")) {
            return "Khóa API không hợp lệ. Hãy kiểm tra và lưu lại khóa từ Google AI Studio.";
        }
        if (error.contains("429") || error.toLowerCase(Locale.ROOT).contains("quota")) {
            return "Google Gemini API đã chạm giới hạn lượt dùng hoặc quota. Hãy kiểm tra hạn mức/tài khoản API.";
        }
        return "Không kết nối được Gemini: " + error;
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
