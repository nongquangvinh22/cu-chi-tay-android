package vn.vivogemini.assistant;

import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionSession;
import android.os.Bundle;

/** Entry point registered with Android as an optional user-selected assistant. */
public class VinhVoiceInteractionService extends VoiceInteractionService {
    @Override
    public void onReady() {
        super.onReady();
        // This app does not request the current app's screen or screenshot as assist context.
        setDisabledShowContext(
                VoiceInteractionSession.SHOW_WITH_ASSIST
                        | VoiceInteractionSession.SHOW_WITH_SCREENSHOT);
    }

    @Override
    public void onLaunchVoiceAssistFromKeyguard() {
        // Deliberately do not surface personal content over the lock screen.
        super.onLaunchVoiceAssistFromKeyguard();
    }
}
