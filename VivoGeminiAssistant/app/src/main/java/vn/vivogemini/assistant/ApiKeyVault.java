package vn.vivogemini.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class ApiKeyVault {
    private static final String ALIAS = "vinh_circle_gemini_api_key_v1";
    private static final String PREFS = "vinh_circle_ai_secrets";
    private static final String VALUE = "encrypted_key";

    private ApiKeyVault() {}

    static void save(Context context, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            clear(context);
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        prefs(context).edit().putString(VALUE, Base64.encodeToString(payload, Base64.NO_WRAP)).apply();
    }

    static String load(Context context) throws Exception {
        String encoded = prefs(context).getString(VALUE, null);
        if (encoded == null || encoded.isEmpty()) return "";
        byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
        if (payload.length < 29) throw new IllegalStateException("Khóa API đã lưu không hợp lệ.");
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[payload.length - iv.length];
        System.arraycopy(payload, 0, iv, 0, iv.length);
        System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    static void clear(Context context) {
        prefs(context).edit().remove(VALUE).apply();
    }

    static boolean hasKey(Context context) {
        return prefs(context).contains(VALUE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }
}
