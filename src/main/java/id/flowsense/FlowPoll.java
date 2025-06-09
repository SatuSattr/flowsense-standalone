package id.flowsense;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FlowPoll {
    private static final String POLL_URL = "https://ux.appcloud.id/catcher/oentry.php";

    public static JsonObject get(String token, String clientId) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        payload.addProperty("clientid", Integer.parseInt(clientId));

        HttpURLConnection conn = (HttpURLConnection) new URL(POLL_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) return null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String response = in.readLine();
            if (response == null || response.trim().equals("null")) return null;
            return JsonParser.parseString(response).getAsJsonObject();
        }
    }
}
