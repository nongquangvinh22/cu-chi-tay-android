package vn.vivogemini.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.voice.VoiceInteractionSession;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Routes the Android assistant gesture into the native Vinh AI panel. */
final class VinhVoiceInteractionSession extends VoiceInteractionSession {
    VinhVoiceInteractionSession(Context context) {
        super(context);
    }

    @Override
    public View onCreateContentView() {
        TextView view = new TextView(getContext());
        view.setText("Đang mở Vinh AI…");
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setPadding(24, 18, 24, 18);
        return view;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Context context = getContext();
        if (Settings.canDrawOverlays(context)) {
            AssistantOverlayService.show(context);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 220);
        } else {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 220);
        }
    }
}
