package vn.vivogemini.assistant;

import android.content.Context;
import org.json.JSONObject;

/** Full settings document shared by the page and the overlay. */
public final class Store {
    private static final String PREF = "vinh_circle";
    private static final String KEY = "json";

    private Store() {}

    public static JSONObject get(Context context) {
        String raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "{}");
        try {
            return new JSONObject(raw == null ? "{}" : raw);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    public static void replace(Context context, String json) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, json).apply();
    }

    public static int num(JSONObject object, String key, int fallback) {
        Object value = object.opt(key);
        if (value instanceof Number) return Math.round(((Number) value).floatValue());
        return fallback;
    }
}
