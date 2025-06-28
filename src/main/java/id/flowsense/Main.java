package id.flowsense;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import id.flowsense.api.DonationEvent;
import io.github.milkdrinkers.colorparser.ColorParser;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Main extends JavaPlugin implements TabCompleter {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static BukkitAudiences audiences;
    private static Main instance;
    public static String prefix, token, prtoken, clientId;
    private static boolean broadcastmssage, donationtrigger;
    private int provider;
    private boolean iloveherxx = true;

    private FlowAuth flowAuth;
    private Thread pollingThread;
    private volatile boolean running = false;

    BodyProcess processor = new BodyProcess(getConfig(), getLogger());

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        audiences = BukkitAudiences.create(this);
        saveDefaultConfig();
        getCommand("flowsense").setTabCompleter(this);
        if (reloadAll(false, false)) {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + "&aSuccessfully enabled! &f(took " + timeTaken + " ms)");
        } else {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + "&cPlugin not enabled! &f(took " + timeTaken + " ms)");
            iloveherxx = false;
        }
        ;
    }

    @Override
    public void onDisable() {
        stopPolling();
        if (flowAuth != null && clientId != null) {
            try {
                FlowAuth.exit(token, clientId);
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

    public static String getEveryThirdChar(String x) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < x.length(); i += 3) {
            result.append(x.charAt(i));
            if (result.length() == 10) break;
        }
        return result.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                sendColored(sender, prefix + "<gray>Reloading FlowSense...");
                boolean aylacantik = reloadAll(true, iloveherxx);
                if (aylacantik) {
                    sendColored(sender, prefix + "<green>Reload perfectly complete.");
                } else {
                    sendColored(sender, prefix + "&cReload &acompleted &cwith errors.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("lookup")) {
                String partialToken = getEveryThirdChar(token);
                String url = "https://ux.appcloud.id/catcher/ientry.php?ux=" + partialToken;
                long ping = Pinger.checkPing("https://ux.appcloud.id/pinger");
                if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
                    loggx(" ");
                    loggx("<#424242>========= &8Flowsense Lookup <#424242>=========");
                    loggx(" &7• <#e2af41>Client ID&8: &7" + clientId);
                    loggx(" &7• <#e2af41>Provider&8: " + getProviderName(provider, true) + " &7(" + provider + ")");
                    loggx(" &7• <#e2af41>Ping to Backend&8: <#68f068>" + ping + "ms");
                    loggx(" &7• <#e27c41>Webhook URL&8: &6" + url);
                    loggx("<#424242>==================================");
                    loggx(" ");
                } else {
                    sendColored(sender, " ");
                    sendColored(sender, "<#424242>========= &8Flowsense Lookup <#424242>=========");
                    sendColored(sender, " &7• <#e2af41>Client ID&8: &7" + clientId);
                    sendColored(sender, " &7• <#e2af41>Provider&8: " + getProviderName(provider, true) + " &7(" + provider + ")");
                    sendColored(sender, " &7• <#e2af41>Ping to Backend&8: <#68f068>" + ping + "ms");
                    sendColored(sender, " &7• <#e27c41>Webhook URL&8: &6" + "<click:open_url:'" + url + "'>" + url + "</click>");
                    sendColored(sender, "<#424242>==================================");
                    sendColored(sender, " ");
                }

                return true;
            }

        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("flowsense")) {
            if (args.length == 1) {
                List<String> subcommands = Arrays.asList("reload", "lookup");
                List<String> suggestions = new ArrayList<>();
                String input = args[0].toLowerCase();
                for (String sub : subcommands) {
                    if (sub.startsWith(input)) {
                        suggestions.add(sub);
                    }
                }
                return suggestions;
            }
        }
        return Collections.emptyList();
    }


    private boolean reloadAll(boolean isreload, boolean iloveher) {
        reloadConfig();
        FileConfiguration config = getConfig();
        processor = new BodyProcess(getConfig(), getLogger());
        token = config.getString("token");
        prtoken = config.getString("webhook-token");
        provider = config.getInt("provider");
        prefix = getConfig().getString("prefix", "&9[flowsense] ");
        broadcastmssage = getConfig().getBoolean("broadcast-message", true);
        donationtrigger = getConfig().getBoolean("donation-trigger", true);

        stopPolling();

        if (provider > 3) {
            loggx(prefix + "&cProvider is not valid! please use 1,2, or 3!");
            return false;
        }
        flowAuth = new FlowAuth();
        try {
            if (isreload && iloveher) {
                boolean exiting = FlowAuth.exit(token, clientId);
            }
            String response = FlowAuth.auth(token, provider, prtoken);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("error")) {
                String errorMsg = json.get("error").getAsString();
                loggx(prefix + "&6" + errorMsg);
                return false;
            }

            if (json.has("client_id")) {
                clientId = json.get("client_id").getAsString();
                if (clientId == null || clientId.isEmpty()) {
                    loggx(prefix + "<red>Authorization Failed! (invalid token)</red>");
                    return false;
                }
                loggx(prefix + "<green>Client Authorized! &r" + getProviderName(provider, true) + " &7(" + clientId + ")");
                iloveherxx = true;
//                loggx(prefix + "&aWebhook Addr:&d " + "https://ux.appcloud.id/catcher/ientry.php?ux=" + getFirstAndLastFive(token));
                startPollingThread();
                return true;
            } else {
                loggx(prefix + "<red>Authorization Failed! (invalid token)</red>");
                return false;
            }

        } catch (Exception e) {
            loggx(prefix + "<red>Authorization Failed!: </red>" + e.getMessage());
            return false;
        }
    }


    private String getProviderName(int xx, boolean iscolored) {
        if (xx == 1) {
            if (iscolored) return "<#FAAE2B>Saweria";
            return "Saweria";
        } else if (xx == 2) {
            if (iscolored) return "<#35A0E8>Tako";
            return "Tako";
        } else if (xx == 3) {
            if (iscolored) return "<#e33446>Trakteer";
            return "Trakteer";
        }
        return "Unknown";
    }


    private void startPollingThread() {
        running = true;
        pollingThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    boolean updated = flowAuth.update(token, clientId);
                    if (!updated) {
                        loggx("<red>Update gagal ke backend, menghentikan polling.</red>");
                        break;
                    }

                    JsonObject entry = FlowPoll.get(token, clientId);
                    if (entry != null) {
                        if (broadcastmssage) {
                            Component message = processor.handle(entry);
                            audiences.all().sendMessage(message);
                        }
                        if (donationtrigger) {
                            processor.Trigger(entry);
                        }

                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            Bukkit.getPluginManager().callEvent(new DonationEvent(entry));
                        });

                    }

                    Thread.sleep(500); // biar gak spamming backend, dan bisa interrupted

                } catch (IOException e) {
                    loggx("<red>Polling error: </red>" + e.getMessage());
                    break;
                } catch (InterruptedException e) {
                    // tangkap interrupt, keluarin dari loop
                    Thread.currentThread().interrupt(); // set ulang status interrupted
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
        }
    }

    public static Main getInstance() {
        return instance;
    }
}
