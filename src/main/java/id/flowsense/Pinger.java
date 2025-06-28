package id.flowsense;

import java.net.HttpURLConnection;
import java.net.URL;

public class Pinger {

    public static long checkPing(String url) {
        try {
            long start = System.currentTimeMillis(); // waktu mulai
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // timeout 3 detik
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode(); // kirim request
            if (responseCode != 200) {
                System.out.println("Ping failed: " + responseCode);
                return -1;
            }

            long end = System.currentTimeMillis(); // waktu selesai
            return end - start; // hitung durasi ping
        } catch (Exception e) {
            System.out.println("Ping error: " + e.getMessage());
            return -1;
        }
    }
}

