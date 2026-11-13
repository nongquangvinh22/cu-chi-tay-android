package vn.vivogemini.assistant;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Speaks notifications the user opted in to. Text stays in memory for the utterance only:
 * it is not written to disk, not logged, and not sent off the phone.
 */
public class SpeakService extends NotificationListenerService {
    private Speaker speaker;
    private String lastKey = "";
    private long lastAt;

    @Override
    public void onCreate() {
        super.onCreate();
        speaker = new Speaker(this);
    }

    @Override
    public void onDestroy() {
        if (speaker != null) speaker.shutdown();
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        JSONObject settings = Store.get(this);
        if (!settings.optBoolean("readEnabled", false)) return;
        if (!allowed(settings, sbn.getPackageName())) return;
        if (!dayOk(settings) || !timeOk(settings)) return;
        String key = sbn.getKey();
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) && now - lastAt < 2000) return;
        String spoken = extract(notification);
        if (spoken.isBlank()) return;
        lastKey = key;
        lastAt = now;
        if (speaker != null) speaker.say(spoken, settings.optString("voice", "auto"));
    }

    private static boolean allowed(JSONObject settings, String pkg) {
        JSONArray list = settings.optJSONArray("readPackages");
        if (list == null) return false;
        for (int i = 0; i < list.length(); i++) {
            if (pkg.equals(list.optString(i))) return true;
        }
        return false;
    }

    private static boolean dayOk(JSONObject settings) {
        JSONArray days = settings.optJSONArray("days");
        if (days == null || days.length() == 0) return true;
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int key = dow == Calendar.SUNDAY ? 7 : dow - 1;
        for (int i = 0; i < days.length(); i++) {
            if (days.optInt(i) == key) return true;
        }
        return false;
    }

    private static boolean timeOk(JSONObject settings) {
        int start = minutes(settings.optString("from", "00:00"));
        int end = minutes(settings.optString("to", "00:00"));
        if (start == end) return true;
        Calendar now = Calendar.getInstance();
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (start < end) return cur >= start && cur < end;
        return cur >= start || cur < end;
    }

    private static int minutes(String hhmm) {
        try {
            String[] parts = hhmm.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static String extract(Notification notification) {
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence big = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        String body = big != null ? big.toString() : text == null ? "" : text.toString();
        String head = title == null ? "" : title.toString();
        String spoken = (head + ". " + body).replaceAll("\\s+", " ").trim();
        if (spoken.length() > 180) spoken = spoken.substring(0, 180);
        return spoken;
    }
}
