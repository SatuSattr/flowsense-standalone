package id.flowsenseStd.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import id.flowsenseStd.Main;
import id.flowsenseStd.api.DonationEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static id.flowsenseStd.Main.loggx;

public class Http {
    private HttpServer server;

    public void start(int port, String pathx) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(pathx, new WebhookHandler());
        server.setExecutor(null); // default
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    static class WebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            JsonObject json;
            try {
                json = JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            int provider = Universalizer.providerDeterminer(json);
            Universalizer entry = Universalizer.from(json, provider);
            JsonObject universalJson = entry.toJson();

            // Handle
            if (Main.getInstance().getConfig().getBoolean("broadcast-message", true)) {
                Component msg = Main.getInstance().processor.handle(universalJson);
                Main.audiences.all().sendMessage(msg);
            }
            if (Main.getInstance().getConfig().getBoolean("donation-trigger", true)) {
                Main.getInstance().processor.Trigger(universalJson);
            }

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                Bukkit.getPluginManager().callEvent(new DonationEvent(universalJson));
            });

            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
