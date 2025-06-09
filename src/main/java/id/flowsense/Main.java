package id.flowsense;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.milkdrinkers.colorparser.ColorParser;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class Main extends JavaPlugin {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static BukkitAudiences audiences;
    private static Main instance;
    public static String prefix, token, prtoken, clientId;
    private int provider;

    private FlowAuth flowAuth;
    private Thread pollingThread;
    private volatile boolean running = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        audiences = BukkitAudiences.create(this);
        saveDefaultConfig();
        if (reloadAll()) {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + " &aSuccessfully enabled! &f(took " + timeTaken + " ms)");
        } else {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + " &cPlugin not enabled! &f(took " + timeTaken + " ms)");
        };
    }

    @Override
    public void onDisable() {
        stopPolling();
        if (flowAuth != null && clientId != null) {
            try {
                flowAuth.exit(token, clientId);
                loggx("<gray>Client <red>exited</red> saat onDisable()");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (audiences != null) audiences.close();
    }

    public static void loggx(String s) {
        Component msg = ColorParser.of(s).parseLegacy().build();
        audiences.console().sendMessage(msg);
    }

    public static void sendColored(CommandSender sender, String message) {
        Component msg = ColorParser.of(message).parseLegacy().build();
        audiences.sender(sender).sendMessage(msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            sendColored(sender, "<gray>Reloading TipFlow...");
            reloadAll();
            sendColored(sender, "<green>Reload complete.");
            return true;
        }
        return false;
    }

    private boolean reloadAll() {
        reloadConfig();
        FileConfiguration config = getConfig();
        token = config.getString("token");
        prtoken = config.getString("webhook-token");
        provider = config.getInt("provider");
        prefix = getConfig().getString("prefix", "&9[flowsense]");

        stopPolling();

        flowAuth = new FlowAuth();
        try {
            String response = FlowAuth.auth(token, provider, prtoken);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("error")) {
                String errorMsg = json.get("error").getAsString();
                loggx(prefix + " &6" + errorMsg);
                return false;
            }

            if (json.has("client_id")) {
                clientId = json.get("client_id").getAsString();
                if (clientId == null || clientId.isEmpty()) {
                    loggx("<red>Auth gagal! (client_id kosong)</red>");
                    return false;
                }
                loggx("<green>Auth sukses! Client ID: </green>" + clientId);
                startPollingThread();
                return true;
            } else {
                loggx("<red>Auth gagal! (client_id tidak ditemukan)</red>");
                return false;
            }
        } catch (Exception e) {
            loggx("<red>Gagal melakukan auth: </red>" + e.getMessage());
            return false;
        }

    }


    private void startPollingThread() {
        running = true;
        pollingThread = new Thread(() -> {
            while (running) {
                try {
                    boolean updated = flowAuth.update(token, clientId);
                    if (!updated) {
                        loggx("<red>Update gagal ke backend, menghentikan polling.</red>");
                        break;
                    }

                    JsonObject entry = FlowPoll.get(token, clientId);
                    if (entry != null) {
                        loggx("<green>Donation received:</green> " + entry);
                    }

                } catch (IOException e) {
                    loggx("<red>Polling error: </red>" + e.getMessage());
                    break;
                }
            }
        });
        pollingThread.start();
    }

    private void stopPolling() {
        running = false;
        if (pollingThread != null && pollingThread.isAlive()) {
            pollingThread.interrupt();
            try {
                pollingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Main getInstance() {
        return instance;
    }
}
