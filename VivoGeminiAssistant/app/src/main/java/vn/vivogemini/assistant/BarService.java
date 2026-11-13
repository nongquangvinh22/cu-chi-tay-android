package vn.vivogemini.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import org.json.JSONObject;

/** Overlay the user turns on. Touches stay on the bar; the service never captures the screen. */
public class BarService extends Service {
    private static final String CHANNEL = "vinh_bar";
    private static final int NOTE_ID = 41;

    private WindowManager windows;
    private WindowManager.LayoutParams lp;
    private View bar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean dragMode;
    private boolean preferLive;
    private boolean hideLandscape;
    private String holdAction = "circle";
    private String doubleAction = "gemini";
    private String tripleAction = "translate";
    private int holdMs = 500;
    private float align = 50f;
    private boolean vertical;
    private boolean longFired;
    private boolean moved;
    private float downX;
    private float downY;
    private float lastX;
    private int taps;
    private final Runnable longPress = () -> {
        longFired = true;
        Launcher.run(this, holdAction, preferLive);
    };
    private final Runnable flushTaps = () -> {
        int count = taps;
        taps = 0;
        if (count >= 3) Launcher.run(this, tripleAction, preferLive);
        else if (count == 2) Launcher.run(this, doubleAction, preferLive);
    };

    private final ComponentCallbacks callbacks = new ComponentCallbacks() {
        @Override public void onConfigurationChanged(Configuration newConfig) { apply(); }
        @Override public void onLowMemory() {}
    };

    private final View.OnTouchListener touch = (v, event) -> {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                longFired = false;
                moved = false;
                downX = event.getRawX();
                downY = event.getRawY();
                lastX = downX;
                handler.removeCallbacks(longPress);
                handler.removeCallbacks(flushTaps);
                if (!dragMode) handler.postDelayed(longPress, holdMs);
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = event.getRawX() - downX;
                float dy = event.getRawY() - downY;
                if (Math.hypot(dx, dy) > dp(12)) {
                    moved = true;
                    handler.removeCallbacks(longPress);
                    if (dragMode && !vertical) {
                        float delta = event.getRawX() - lastX;
                        lastX = event.getRawX();
                        int travel = Math.max(1, screenWidth() - lp.width);
                        align = Math.max(0f, Math.min(100f, align + (delta / travel) * 100f));
                        lp.x = Math.round(travel * (align / 100f));
                        windows.updateViewLayout(bar, lp);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress);
                if (dragMode && moved) persistAlign();
                else if (!longFired && !moved && !dragMode && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    taps += 1;
                    handler.removeCallbacks(flushTaps);
                    handler.postDelayed(flushTaps, 320);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windows = getSystemService(WindowManager.class);
        startInForeground();
        registerComponentCallbacks(callbacks);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        apply();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        removeBar();
        unregisterComponentCallbacks(callbacks);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void apply() {
        JSONObject settings = Store.get(this);
        boolean enabled = settings.optBoolean("barEnabled", false);
        if (!enabled || !Settings.canDrawOverlays(this)) {
            removeBar();
            stopSelf();
            return;
        }
        read(settings);
        if (bar == null) {
            bar = new View(this);
            bar.setOnTouchListener(touch);
            lp = new WindowManager.LayoutParams(
                    dp(120),
                    dp(18),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            windows.addView(bar, lp);
        }
        paint(settings);
        place(settings);
        boolean hide = hideLandscape
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        bar.setVisibility(hide ? View.GONE : View.VISIBLE);
        try {
            windows.updateViewLayout(bar, lp);
        } catch (Exception ignored) {
            // The view was already removed.
        }
    }

    private void read(JSONObject settings) {
        dragMode = settings.optBoolean("dragMode", false);
        preferLive = settings.optBoolean("preferLive", true);
        hideLandscape = settings.optBoolean("hideLandscape", true);
        holdAction = settings.optString("holdAction", "circle");
        doubleAction = settings.optString("doubleAction", "gemini");
        tripleAction = settings.optString("tripleAction", "translate");
        holdMs = Store.num(settings, "holdMs", 500);
        align = Store.num(settings, "align", 50);
        vertical = "vertical".equals(settings.optString("axis", "horizontal"));
    }

    private void paint(JSONObject settings) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(99));
        drawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        switch (settings.optString("theme", "breeze")) {
            case "google" -> drawable.setColors(new int[] {0xFF4285F4, 0xFFEA4335, 0xFFFBBC05, 0xFF34A853});
            case "cyber" -> drawable.setColors(new int[] {0xFF39E1FF, 0xFF6D7CFF});
            case "glass" -> drawable.setColor(0x88FFFFFF);
            default -> drawable.setColors(new int[] {0xFF5B7CFA, 0xFF8A6BFF, 0xFFC06AD8});
        }
        bar.setBackground(drawable);
        float opacity = Store.num(settings, "opacity", 85) / 100f;
        bar.setAlpha(Math.max(0.2f, Math.min(1f, opacity)));
    }

    private void place(JSONObject settings) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int widthDp = Store.num(settings, "widthDp", 120);
        int thickDp = Store.num(settings, "thicknessDp", 18);
        int edge = Store.num(settings, "bottomDp", 6);
        lp.width = dp(vertical ? thickDp : widthDp);
        lp.height = dp(vertical ? widthDp : thickDp);
        if (!vertical) {
            int travel = Math.max(0, metrics.widthPixels - lp.width);
            lp.gravity = Gravity.BOTTOM | Gravity.START;
            lp.x = Math.round(travel * (align / 100f));
            lp.y = dp(edge);
        } else {
            int travel = Math.max(0, metrics.heightPixels - lp.height);
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.x = dp(8);
            lp.y = Math.round(travel * (align / 100f));
        }
    }

    private void persistAlign() {
        try {
            JSONObject settings = Store.get(this);
            settings.put("align", Math.round(align));
            Store.replace(this, settings.toString());
        } catch (Exception ignored) {
            // Keep the on-screen position even if prefs fail.
        }
    }

    private void removeBar() {
        if (bar != null) {
            try {
                windows.removeView(bar);
            } catch (Exception ignored) {
                // Already detached.
            }
            bar = null;
        }
    }

    private void startInForeground() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Thanh cử chỉ", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Hiện khi thanh Vinh Circle đang nổi.");
            manager.createNotificationChannel(channel);
        }
        PendingIntent open = PendingIntent.getActivity(
                this,
                3,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Vinh Circle")
                .setContentText("Thanh cử chỉ đang bật")
                .setSmallIcon(R.drawable.ic_stat)
                .setOngoing(true)
                .setContentIntent(open)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTE_ID, notification);
        }
    }

    private int screenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
