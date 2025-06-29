package id.flowsense;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FlowPoll {
    private final String pollUrl;

    public FlowPoll(String pollUrl) {
        this.pollUrl = pollUrl;
    }

    public JsonObject get(String token, String clientId) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        payload.addProperty("clientid", Integer.parseInt(clientId));

        HttpURLConnection conn = (HttpURLConnection) new URL(pollUrl).openConnection();
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
