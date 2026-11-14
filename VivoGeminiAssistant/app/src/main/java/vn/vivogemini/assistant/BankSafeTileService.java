package vn.vivogemini.assistant;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import org.json.JSONObject;

/**
 * One-tap safety switch for banking sessions.
 * It disables the gesture overlay and notification speech. Turning the tile off
 * does not silently re-enable either feature; the user must choose them again.
 */
public class BankSafeTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean safe = Store.get(this).optBoolean("bankSafeActive", false);
        tile.setLabel("Bank an toàn");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(safe ? "Đã tắt thanh và đọc thông báo" : "Chạm để tắt tích hợp");
        }
        tile.setState(safe ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            JSONObject settings = Store.get(this);
            boolean enable = !settings.optBoolean("bankSafeActive", false);
            settings.put("bankSafeActive", enable);
            if (enable) {
                settings.put("barEnabled", false);
                settings.put("readEnabled", false);
            }
            Store.replace(this, settings.toString());
            BarController.sync(this);
        } catch (Exception ignored) {
            // The app remains in its last saved safe state.
        }
        onStartListening();
    }
}
