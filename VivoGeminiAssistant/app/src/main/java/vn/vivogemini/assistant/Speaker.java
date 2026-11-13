package vn.vivogemini.assistant;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/** On-device speech. Callers must not pass secrets into logs. */
public final class Speaker {
    private final TextToSpeech tts;
    private boolean ready;

    public Speaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> ready = status == TextToSpeech.SUCCESS);
    }

    public void say(String text, String mode) {
        if (!ready || text == null || text.isBlank()) return;
        boolean vi = "vi".equals(mode) || ("auto".equals(mode) && looksVietnamese(text)) || mode == null;
        tts.setLanguage(vi ? new Locale("vi", "VN") : Locale.US);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vinh-circle");
    }

    public void shutdown() {
        tts.shutdown();
    }

    static boolean looksVietnamese(String text) {
        return text.matches(".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ].*");
    }
}
