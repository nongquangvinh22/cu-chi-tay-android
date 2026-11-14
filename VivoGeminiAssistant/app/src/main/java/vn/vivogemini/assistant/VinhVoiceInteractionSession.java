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

/** Routes the Android assistant gesture to the installed Gemini app. */
final class VinhVoiceInteractionSession extends VoiceInteractionSession {
    VinhVoiceInteractionSession(Context context) {
        super(context);
    }

    @Override
    public View onCreateContentView() {
        TextView view = new TextView(getContext());
        view.setText("Đang mở Gemini…");
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER);
        view.setPadding(24, 18, 24, 18);
        return view;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Context context = getContext();
        // Route the Android assistant gesture to the installed Gemini app.
        // Gemini Live, when available for this account/device, is selected inside Gemini.
        Launcher.gemini(context);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 220);
    }
}
