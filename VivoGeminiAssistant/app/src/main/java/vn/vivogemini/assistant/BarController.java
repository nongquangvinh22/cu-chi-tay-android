package vn.vivogemini.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONObject;

public final class BarController {
    private BarController() {}

    public static void sync(Context context) {
        JSONObject settings = Store.get(context);
        boolean bankSafe = settings.optBoolean("bankSafeActive", false);
        if (bankSafe) {
            try {
                settings.put("barEnabled", false);
                settings.put("readEnabled", false);
                Store.replace(context, settings.toString());
            } catch (Exception ignored) {
                // Keep the bar disabled below even if preferences cannot be rewritten.
            }
        }
        boolean enabled = !bankSafe && settings.optBoolean("barEnabled", false);
        if (!enabled || !Settings.canDrawOverlays(context)) {
            context.stopService(new Intent(context, BarService.class));
            return;
        }
        Intent intent = new Intent(context, BarService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }
}
