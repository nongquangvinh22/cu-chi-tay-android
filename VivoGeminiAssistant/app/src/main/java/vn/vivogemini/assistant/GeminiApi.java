package vn.vivogemini.assistant;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class GeminiApi {
    static final String MODEL = "gemini-3.8-flash";
    private GeminiApi() {}

    static JSONObject ask(String apiKey, JSONArray history) throws Exception {
        String encodedKey = URLEncoder.encode(apiKey, "UTF-8");
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + encodedKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(25000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        JSONObject request = new JSONObject();
        request.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(
                new JSONObject().put("text",
                        "Bạn là trợ lý AI tiếng Việt chạy trực tiếp trong ứng dụng Vinh Circle của Nông Quang Vinh. "
                        + "Trả lời rõ ràng, hữu ích và ngắn gọn. Khi người dùng yêu cầu mở ứng dụng, đặt báo thức, "
                        + "hẹn giờ, mở Gemini, Google Circle to Search hoặc Google Dịch, hãy gọi đúng công cụ. "
                        + "Không tự thực thi hành động; ứng dụng sẽ hỏi người dùng xác nhận trước. "
                        + "Không yêu cầu, đọc hoặc gửi mật khẩu, mã OTP hay thông tin thanh toán. "
                        + "Không tự nhận rằng bạn đã nhìn thấy màn hình nếu chưa có dữ liệu."))));
        request.put("contents", history);
        request.put("tools", new JSONArray().put(
                new JSONObject().put("functionDeclarations", declarations())));
        request.put("toolConfig", new JSONObject().put("functionCallingConfig",
                new JSONObject().put("mode", "AUTO")));
        request.put("generationConfig", new JSONObject()
                .put("temperature", 0.25)
                .put("maxOutputTokens", 1200));

        try {
            byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
                out.flush();
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            JSONObject json = new JSONObject(readAll(stream));
            if (status < 200 || status >= 300) {
                JSONObject error = json.optJSONObject("error");
                String message = error == null ? ("HTTP " + status)
                        : error.optString("message", "HTTP " + status);
                throw new IllegalStateException(message);
            }
            return parse(json);
        } finally {
            connection.disconnect();
        }
    }

    private static JSONArray declarations() throws Exception {
        JSONArray all = new JSONArray();
        all.put(function("open_app", "Mở một ứng dụng đã cài trên điện thoại theo tên hiển thị.",
                new JSONObject().put("app_name", property("STRING", "Tên ứng dụng cần mở.")),
                "app_name"));
        all.put(function("set_alarm", "Mở giao diện Đồng hồ để người dùng xác nhận tạo báo thức.",
                new JSONObject()
                        .put("hour", property("INTEGER", "Giờ từ 0 đến 23."))
                        .put("minute", property("INTEGER", "Phút từ 0 đến 59."))
                        .put("label", property("STRING", "Nhãn báo thức, nếu có.")),
                "hour", "minute"));
        all.put(function("set_timer", "Mở giao diện Đồng hồ để xác nhận bắt đầu hẹn giờ.",
                new JSONObject()
                        .put("seconds", property("INTEGER", "Thời lượng hẹn giờ tính bằng giây."))
                        .put("label", property("STRING", "Nhãn hẹn giờ, nếu có.")),
                "seconds"));
        all.put(function("open_gemini", "Mở ứng dụng Gemini chính thức đã cài trên máy.",
                new JSONObject(), new String[0]));
        all.put(function("open_circle_search", "Thử mở Circle to Search trong ứng dụng Google trên máy; không mở trình duyệt.",
                new JSONObject(), new String[0]));
        all.put(function("open_translate", "Mở ứng dụng Google Dịch đã cài trên máy.",
                new JSONObject(), new String[0]));
        all.put(function("translate_text", "Chuyển một đoạn văn bản người dùng cung cấp sang Google Dịch.",
                new JSONObject().put("text", property("STRING", "Đoạn văn bản cần dịch.")),
                "text"));
        return all;
    }

    private static JSONObject property(String type, String description) throws Exception {
        return new JSONObject().put("type", type).put("description", description);
    }

    private static JSONObject function(String name, String description,
                                       JSONObject properties, String... required) throws Exception {
        JSONObject parameters = new JSONObject().put("type", "OBJECT").put("properties", properties);
        if (required.length > 0) {
            JSONArray req = new JSONArray();
            for (String field : required) req.put(field);
            parameters.put("required", req);
        }
        return new JSONObject().put("name", name)
                .put("description", description)
                .put("parameters", parameters);
    }

    private static JSONObject parse(JSONObject response) throws Exception {
        JSONArray candidates = response.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new IllegalStateException("Gemini không trả về nội dung. Hãy thử lại.");
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts == null) throw new IllegalStateException("Phản hồi Gemini không hợp lệ.");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (part.has("functionCall")) {
                return new JSONObject().put("kind", "function")
                        .put("call", part.getJSONObject("functionCall"));
            }
            if (part.has("text")) {
                if (text.length() > 0) text.append("\n");
                text.append(part.optString("text"));
            }
        }
        return new JSONObject().put("kind", "text").put("text", text.toString());
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }
}
