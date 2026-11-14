package vn.vivogemini.assistant;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings shortcut for a single, user-confirmed screen search. */
public final class CircleSearchTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel("Khoanh vùng");
        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            unlockAndRun(this::openCaptureActivity);
        } else {
            openCaptureActivity();
        }
    }

    private void openCaptureActivity() {
        Intent intent = new Intent(this, CircleCaptureActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pending = PendingIntent.getActivity(
                        this, 8810, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending);
            } else {
                startActivityAndCollapse(intent);
            }
        } catch (Exception ex) {
            try {
                startActivity(intent);
            } catch (Exception ignored) {
                // The tile remains available; the user can open capture from the floating bar.
            }
        }
    }
}
