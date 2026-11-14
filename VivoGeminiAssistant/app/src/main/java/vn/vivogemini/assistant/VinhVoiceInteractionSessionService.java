package vn.vivogemini.assistant;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

/** Creates a lightweight session when Android invokes Vinh Circle as the default assistant. */
public class VinhVoiceInteractionSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        return new VinhVoiceInteractionSession(this);
    }
}
