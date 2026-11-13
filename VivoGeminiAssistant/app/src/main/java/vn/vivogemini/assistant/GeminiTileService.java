package vn.vivogemini.assistant;

import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import org.json.JSONObject;

/** Quick Settings tile toggles the gesture bar. It does not read the screen. */
public class GeminiTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean on = Store.get(this).optBoolean("barEnabled", false) && Settings.canDrawOverlays(this);
        tile.setLabel("Vinh Circle");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(on ? "Thanh đang bật" : "Thanh đang tắt");
        }
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!Settings.canDrawOverlays(this)) {
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapseCompat(open);
            return;
        }
        try {
            JSONObject settings = Store.get(this);
            settings.put("barEnabled", !settings.optBoolean("barEnabled", false));
            Store.replace(this, settings.toString());
            BarController.sync(this);
        } catch (Exception ignored) {
            BarController.sync(this);
        }
        onStartListening();
    }

    private void startActivityAndCollapseCompat(Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(android.app.PendingIntent.getActivity(
                        this, 7, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
            } else {
                startActivityAndCollapse(intent);
            }
        } catch (Exception ex) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
