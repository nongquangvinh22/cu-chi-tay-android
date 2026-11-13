package vn.vivogemini.assistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            BarController.sync(context);
        } catch (Exception ignored) {
            // OriginOS may block a foreground service from boot. The bar returns when the app opens.
        }
    }
}
