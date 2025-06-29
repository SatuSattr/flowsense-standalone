package id.flowsense;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class FlowAuth {
    private final String AUTH_URL;
    private final String UPDATE_URL;
    private final String EXIT_URL;

    public FlowAuth(String authurl, String updateurl, String exiturl) {
        this.AUTH_URL = authurl;
        this.UPDATE_URL = updateurl;
        this.EXIT_URL = exiturl;
    }

    private String extractClientId(String json) {
        String key = "\"client_id\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    private String sendPostRequest(String urlStr, String urlParameters) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(urlParameters.getBytes("UTF-8"));
        }

        InputStream stream = (conn.getResponseCode() >= 400)
                ? conn.getErrorStream()
                : conn.getInputStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    public String auth(String token, int provider, String prtoken) throws IOException {
        String params = "token=" + URLEncoder.encode(token, "UTF-8") +
                "&provider=" + provider +
                "&prtoken=" + URLEncoder.encode(prtoken, "UTF-8");
        return sendPostRequest(AUTH_URL, params);
    }

    public boolean update(String token, String clientId) throws IOException {
        String params = "token=" + URLEncoder.encode(token, "UTF-8") +
                "&clientid=" + URLEncoder.encode(clientId, "UTF-8");
        return sendPostRequest(UPDATE_URL, params).trim().equalsIgnoreCase("true");
    }

    public boolean exit(String token, String clientId) throws IOException {
        String params = "token=" + URLEncoder.encode(token, "UTF-8") +
                "&clientid=" + URLEncoder.encode(clientId, "UTF-8");
        return sendPostRequest(EXIT_URL, params).trim().equalsIgnoreCase("true");
    }
}
