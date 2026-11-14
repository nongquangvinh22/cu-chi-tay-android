package vn.vivogemini.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int REQ_NOTIF = 42;
    private WebView web;
    private Speaker speaker;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        speaker = new Speaker(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#120E24"));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "VinhNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.getUrl() == null || !"file".equals(request.getUrl().getScheme());
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/www/phone/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.evaluateJavascript(
                    "window.__vinhRefresh&&window.__vinhRefresh();window.__vinhPull&&window.__vinhPull();",
                    null);
        }
    }

    @Override
    protected void onDestroy() {
        if (speaker != null) speaker.shutdown();
        if (web != null) web.destroy();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript("window.__vinhBack?window.__vinhBack():'0'", value -> {
            if (value != null && value.contains("1")) return;
            finish();
        });
    }

    private final class Bridge {
        @JavascriptInterface
        public String getSettings() {
            return Store.get(MainActivity.this).toString();
        }

        @JavascriptInterface
        public void saveSettings(String json) {
            if (json == null || json.length() > 200_000) return;
            Store.replace(MainActivity.this, json);
            main.post(() -> BarController.sync(MainActivity.this));
        }

        @JavascriptInterface
        public String setBarEnabled(boolean on) {
            if (!on) return "ok";
            if (Store.get(MainActivity.this).optBoolean("bankSafeActive", false)) {
                return "bank_safe";
            }
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                main.post(() -> Launcher.overlaySettings(MainActivity.this));
                return "need_overlay";
            }
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                main.post(() -> requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF));
                return "need_notif";
            }
            return "ok";
        }

        @JavascriptInterface public void openCircle() { main.post(() -> Launcher.circle(MainActivity.this)); }
        @JavascriptInterface public void openGemini() { main.post(() -> Launcher.gemini(MainActivity.this)); }
        @JavascriptInterface public void openGeminiLive() { main.post(() -> Launcher.geminiLive(MainActivity.this)); }
        @JavascriptInterface public void openTranslate() { main.post(() -> Launcher.translate(MainActivity.this)); }
        @JavascriptInterface public void openOverlaySettings() { main.post(() -> Launcher.overlaySettings(MainActivity.this)); }
        @JavascriptInterface public void openListenerSettings() { main.post(() -> Launcher.listenerSettings(MainActivity.this)); }
        @JavascriptInterface public void openAssistantSettings() { main.post(() -> Launcher.assistantSettings(MainActivity.this)); }
        @JavascriptInterface public void openGoogleSettings() { main.post(() -> Launcher.appSettings(MainActivity.this, Launcher.GOOGLE)); }
        @JavascriptInterface public void openGeminiSettings() { main.post(() -> Launcher.appSettings(MainActivity.this, Launcher.BARD)); }
        @JavascriptInterface public void openAutostart() { main.post(() -> Launcher.autostart(MainActivity.this)); }
        @JavascriptInterface public void openBattery() { main.post(() -> Launcher.battery(MainActivity.this)); }
        @JavascriptInterface public void openAppInfo() { main.post(() -> Launcher.appSettings(MainActivity.this, getPackageName())); }
        @JavascriptInterface public void openRelease() { main.post(() -> Launcher.release(MainActivity.this)); }

        @JavascriptInterface
        public void speak(String text) {
            if (text == null) return;
            String sample = text.length() > 240 ? text.substring(0, 240) : text;
            String mode = Store.get(MainActivity.this).optString("voice", "auto");
            main.post(() -> speaker.say(sample, mode));
        }

        @JavascriptInterface
        public void shareText(String text) {
            if (text == null) return;
            String body = text.length() > 20_000 ? text.substring(0, 20_000) : text;
            main.post(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, body);
                send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(send, "Nhật ký Vinh Circle").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            });
        }

        @JavascriptInterface
        public String getStatus() {
            JSONObject status = new JSONObject();
            try {
                boolean overlay = Settings.canDrawOverlays(MainActivity.this);
                String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
                String assistant = Settings.Secure.getString(getContentResolver(), "assistant");
                String voice = Settings.Secure.getString(getContentResolver(), "voice_interaction_service");
                String blob = (assistant == null ? "" : assistant) + " " + (voice == null ? "" : voice);
                PackageManager pm = getPackageManager();
                status.put("overlay", overlay);
                status.put("bankSafe", Store.get(MainActivity.this).optBoolean("bankSafeActive", false));
                status.put("listener", listeners != null && listeners.contains(getPackageName()));
                status.put("google", pm.getLaunchIntentForPackage(Launcher.GOOGLE) != null);
                status.put("gemini", pm.getLaunchIntentForPackage(Launcher.BARD) != null);
                status.put("assistantGoogle", blob.toLowerCase().contains("google"));
                status.put("notifications", Build.VERSION.SDK_INT < 33
                        || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
            } catch (Exception ignored) {
                // Return whatever was filled.
            }
            return status.toString();
        }

        @JavascriptInterface
        public String listApps() {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(mainIntent, 0);
            Map<String, String> labeled = new LinkedHashMap<>();
            for (ResolveInfo info : infos) {
                if (info.activityInfo == null) continue;
                labeled.put(info.activityInfo.packageName, info.loadLabel(pm).toString());
            }
            List<Map.Entry<String, String>> rows = new ArrayList<>(labeled.entrySet());
            rows.sort(Comparator.comparing(entry -> entry.getValue(), String.CASE_INSENSITIVE_ORDER));
            JSONArray array = new JSONArray();
            int count = 0;
            for (Map.Entry<String, String> row : rows) {
                if (count++ >= 100) break;
                JSONObject item = new JSONObject();
                try {
                    item.put("pkg", row.getKey());
                    item.put("label", row.getValue());
                    array.put(item);
                } catch (Exception ignored) {
                    // Skip a row rather than fail the picker.
                }
            }
            return array.toString();
        }
    }
}
