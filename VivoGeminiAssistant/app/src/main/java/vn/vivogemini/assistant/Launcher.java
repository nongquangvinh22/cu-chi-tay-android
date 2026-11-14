package vn.vivogemini.assistant;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public final class Launcher {
    static final String GOOGLE = "com.google.android.googlequicksearchbox";
    static final String BARD = "com.google.android.apps.bard";
    static final String TRANSLATE = "com.google.android.apps.translate";
    static final String RELEASE =
            "https://github.com/nongquangvinh22/cu-chi-tay-android/releases";

    private Launcher() {}

    public static void circle(Context context) {
        try {
            Intent capture = new Intent(context, CircleCaptureActivity.class);
            capture.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(capture);
        } catch (Exception ex) {
            toast(context,
                    "Không mở được màn hình khoanh vùng. Hãy thêm nút Khoanh vùng trong Cài đặt nhanh rồi thử lại.");
        }
    }

    public static void gemini(Context context) {
        if (launchPackage(context, BARD)) return;
        if (view(context, "https://gemini.google.com/app")) {
            toast(context, "Đang mở Gemini trên web vì chưa tìm thấy ứng dụng Gemini.");
            return;
        }
        toast(context, "Chưa mở được Gemini. Hãy kiểm tra kết nối Internet hoặc cài ứng dụng Gemini.");
    }

    public static void geminiLive(Context context) {
        if (launchPackage(context, BARD)) {
            toast(context, "Đã mở Gemini. Chọn Live trong ứng dụng nếu tài khoản và thiết bị hỗ trợ.");
            return;
        }
        if (view(context, "https://gemini.google.com/app")) {
            toast(context, "Đã mở Gemini trên web. Gemini Live có thể cần ứng dụng và thiết bị được hỗ trợ.");
            return;
        }
        toast(context, "Chưa mở được Gemini. Hãy kiểm tra kết nối Internet hoặc cài ứng dụng Gemini.");
    }

    public static void translate(Context context) {
        if (launchPackage(context, TRANSLATE)) return;
        if (launchPackage(context, GOOGLE)) {
            toast(context, "Đã mở Google. Hãy cài Google Dịch để dịch văn bản hoặc nội dung được chia sẻ.");
            return;
        }
        toast(context, "Chưa tìm thấy Google Dịch. Hãy cài ứng dụng Google Dịch.");
    }

    public static void overlaySettings(Context context) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        start(context, intent, "Không mở được quyền hiển thị trên ứng dụng khác.");
    }

    public static void listenerSettings(Context context) {
        start(context, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                "Không mở được quyền đọc thông báo.");
    }

    public static void assistantSettings(Context context) {
        if (component(context, "com.android.settings", "com.android.settings.Settings$ManageAssistActivity")) return;
        start(context, new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
                "Hãy vào Cài đặt, Ứng dụng mặc định, rồi chọn Google.");
    }

    public static void appSettings(Context context, String pkg) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        start(context, intent, "Không mở được thông tin ứng dụng.");
    }

    public static void autostart(Context context) {
        if (component(context, "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")) return;
        if (component(context, "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")) return;
        if (component(context, "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.PurviewTabActivity")) return;
        appSettings(context, context.getPackageName());
    }

    public static void battery(Context context) {
        start(context, new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Không mở được cài đặt pin.");
    }

    public static void release(Context context) {
        view(context, RELEASE);
    }

    public static void run(Context context, String action, boolean live) {
        if ("circle".equals(action)) circle(context);
        else if ("gemini".equals(action)) {
            if (live) geminiLive(context);
            else gemini(context);
        } else if ("translate".equals(action)) translate(context);
        else if ("assistant".equals(action)) AssistantOverlayService.show(context);
    }

    private static boolean launchPackage(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean assist(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_ASSIST);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean component(Context context, String pkg, String cls) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkg, cls));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (cls.contains("Omnient")) intent.putExtra("source", "long_press_nav_handle");
            context.startActivity(intent);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean view(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void start(Context context, Intent intent, String fail) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ex) {
            toast(context, fail);
        }
    }

    private static void toast(Context context, String text) {
        Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
}
