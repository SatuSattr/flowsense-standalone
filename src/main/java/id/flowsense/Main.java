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
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Main extends JavaPlugin implements TabCompleter {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static BukkitAudiences audiences;
    private static Main instance;
    public static String prefix, token, prtoken, clientId;
    private static boolean broadcastmssage, donationtrigger, hasinternet;
    private int provider;
    private boolean iloveherxx = true;
    RemoteFetcher refetcher = new RemoteFetcher();
    private FlowAuth flowAuth;
    private FlowPoll flowPoll;
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
        hasinternet = SureInternet.HasInternet();

        if (reloadAll(false, false, null)) {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + "&aSuccessfully enabled! &f(took " + timeTaken + " ms)");
        } else {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            loggx(prefix + "<#FF7808>Plugin not enabled correctly! &f(took " + timeTaken + " ms)");
            iloveherxx = false;
        }
    }

    @Override
    public void onDisable() {
        stopPolling();
        if (flowAuth != null && clientId != null) {
            try {
                flowAuth.exit(token, clientId);
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
        if (sender == null) {
            return;
        }
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
                if (!sender.hasPermission("flowsense.reload")) {
                    sendColored(sender, prefix + "<red>You don't have permission to use this.");
                    return true;
                }
                boolean aylacantik;
                sendColored(sender, prefix + "<gray>Reloading FlowSense...");
                if (sender instanceof Player) {
                    aylacantik = reloadAll(true, iloveherxx, sender);
                } else {
                    aylacantik = reloadAll(true, iloveherxx, null);
                }

                if (aylacantik) {
                    sendColored(sender, prefix + "<green>Reload perfectly complete.");
                } else {
                    sendColored(sender, prefix + "&cReload &acompleted &cwith errors.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("fakedonate") || args[0].equalsIgnoreCase("fd")) {
                if (!sender.hasPermission("flowsense.fakedonate")) {
                    sendColored(sender, prefix + "<red>You don't have permission to use this.");
                    return true;
                }

                if (args.length < 4) {
                    sendColored(sender, prefix + "<red>Usage:</red> /flowsense fakedonate <donator_name> <amount> <provider_id> [unit] [unit_qty] <message>");
                    return true;
                }

                String donatorName = args[1];
                long amount;
                int provider;

                try {
                    amount = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sendColored(sender, prefix + "<red>Amount must be a number!</red>");
                    return true;
                }

                try {
                    provider = Integer.parseInt(args[3]);
                    if (provider < 1 || provider > 3) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sendColored(sender, prefix + "<red>Provider ID must be 1 (Saweria), 2 (Tako), or 3 (Trakteer).</red>");
                    return true;
                }

                String unit = "";
                int unit_qty = 1;
                StringBuilder messageBuilder = new StringBuilder();

                if (provider == 3) {
                    if (args.length < 6) {
                        sendColored(sender, prefix + "<red>Provider 3 (Trakteer) requires <unit> and <unit_qty> before message.</red>");
                        return true;
                    }
                    unit = args[4];
                    try {
                        unit_qty = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        sendColored(sender, prefix + "<red>Unit quantity must be a number.</red>");
                        return true;
                    }

                    // Build message from args[6]+
                    for (int i = 6; i < args.length; i++) {
                        messageBuilder.append(args[i]).append(" ");
                    }

                } else {
                    // Build message from args[4]+
                    for (int i = 4; i < args.length; i++) {
                        messageBuilder.append(args[i]).append(" ");
                    }
                }

                String message = messageBuilder.toString().trim();
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String createdat = now.format(formatter);

                JsonObject fakeEntry = new JsonObject();
                fakeEntry.addProperty("id", (int) (Math.random() * 9999));
                fakeEntry.addProperty("created_at", createdat);
                fakeEntry.addProperty("amount", amount);
                fakeEntry.addProperty("donator_name", donatorName);
                fakeEntry.addProperty("message", message);
                fakeEntry.addProperty("provider", provider);
                fakeEntry.addProperty("unit", unit);
                fakeEntry.addProperty("unit_qty", unit_qty);
                fakeEntry.addProperty("donator_email", "flowsense@gmail.com");

                // Kirim ke player + trigger
                if (broadcastmssage) {
                    Component formatted = processor.handle(fakeEntry);
                    audiences.all().sendMessage(formatted);
                }
                if (donationtrigger) {
                    processor.Trigger(fakeEntry);
                }

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    Bukkit.getPluginManager().callEvent(new DonationEvent(fakeEntry));
                });
                return true;
            }


            if (args[0].equalsIgnoreCase("lookup")) {

                if (!sender.hasPermission("flowsense.lookup")) {
                    sendColored(sender, prefix + "<red>You don't have permission to use this.");
                    return true;
                }


                String partialToken = getEveryThirdChar(token);
                String url = "https://" + refetcher.getMainUrl()
                        + refetcher.getCatcherPath()
                        + refetcher.getCatcherFile("input")
                        + "?ux=" + partialToken;
                long ping = Pinger.checkPing("https://" + refetcher.getMainUrl() + refetcher.getPingerUrl());
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
                List<String> suggestions = new ArrayList<>();
                String input = args[0].toLowerCase();

                if (sender.hasPermission("flowsense.reload") && "reload".startsWith(input)) {
                    suggestions.add("reload");
                }

                if (sender.hasPermission("flowsense.lookup") && "lookup".startsWith(input)) {
                    suggestions.add("lookup");
                }

                if (sender.hasPermission("flowsense.fakedonate") && "fakedonate".startsWith(input)) {
                    suggestions.add("fakedonate");
                }

                if (sender.hasPermission("flowsense.fakedonate") && "fd".startsWith(input)) {
                    suggestions.add("fd");
                }

                return suggestions;
            }


            if ((args[0].equalsIgnoreCase("fakedonate") || args[0].equalsIgnoreCase("fd"))) {
                if (!sender.hasPermission("flowsense.fakedonate")) return Collections.emptyList();

                List<String> suggestions = new ArrayList<>();
                switch (args.length) {
                    case 2 -> suggestions.add("<donator_name>");
                    case 3 -> suggestions.add("<amount>");
                    case 4 -> suggestions.addAll(List.of("1", "2", "3"));
                    case 5 -> {
                        // Coba parsing provider_id
                        try {
                            int provider = Integer.parseInt(args[3]);
                            if (provider == 3) {
                                suggestions.add("<unit>");
                            } else if (provider == 1 || provider == 2) {
                                suggestions.add("<message>");
                            }
                        } catch (NumberFormatException e) {
                            // provider invalid, no suggestion
                        }
                    }
                    case 6 -> {
                        try {
                            int provider = Integer.parseInt(args[3]);
                            if (provider == 3) {
                                // kalau args[5] masih kosong, ya suggest unit_qty
                                suggestions.add("<unit_qty>");
                            }
                        } catch (NumberFormatException e) {
                            // do nothing
                        }
                    }
                    case 7 -> {
                        try {
                            int provider = Integer.parseInt(args[3]);
                            if (provider == 3) {
                                // selesai unit dan unit_qty → message sekarang
                                suggestions.add("<message>");
                            }
                        } catch (NumberFormatException e) {
                            // do nothing
                        }
                    }
                }
                return suggestions;
            }

        }

        return Collections.emptyList();
    }


    private boolean reloadAll(boolean isreload, boolean iloveher, CommandSender sender) {
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
            sendColored(sender, prefix + "&cProvider is not valid! please use 1,2, or 3!");
            return false;
        }

        hasinternet = SureInternet.HasInternet();
        if (hasinternet) {
            if (refetcher.fetch()) {
                flowAuth = new FlowAuth(
                        "https://" + refetcher.getMainUrl() + refetcher.getAuthPath() + refetcher.getAuthFile("auth"),
                        "https://" + refetcher.getMainUrl() + refetcher.getAuthPath() + refetcher.getAuthFile("update"),
                        "https://" + refetcher.getMainUrl() + refetcher.getAuthPath() + refetcher.getAuthFile("exit")
                );
                flowPoll = new FlowPoll("https://" + refetcher.getMainUrl() + refetcher.getCatcherPath() + refetcher.getCatcherFile("output"));
                try {
                    if (isreload && iloveher) {
                        boolean exiting = flowAuth.exit(token, clientId);
                    }
                    String response = flowAuth.auth(token, provider, prtoken);
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
                            sendColored(sender, prefix + "<red>Authorization Failed! (invalid token)</red>");
                            return false;
                        }
                        loggx(prefix + "<green>Client Authorized! &r" + getProviderName(provider, true) + " &7(" + clientId + ")");
                        sendColored(sender, prefix + "<green>Client Authorized! &r" + getProviderName(provider, true) + " &7(" + clientId + ")");
                        iloveherxx = true;
                        startPollingThread();
                        return true;
                    } else {
                        loggx(prefix + "<red>Authorization Failed! (invalid token)</red>");
                        sendColored(sender, prefix + "<red>Authorization Failed! (invalid token)</red>");
                        return false;
                    }
                } catch (Exception e) {
                    loggx(prefix + "<red>Authorization Failed!: </red>" + e.getMessage());
                    sendColored(sender, prefix + "<red>Authorization Failed!: </red>" + e.getMessage());
                    return false;
                }
            } else {
                loggx(prefix + "&cFailed fetching remote url! please make sure you have internet access!");
                iloveherxx = false;
            }
        } else {
            loggx(prefix + "&cYour not Connected to the internet!");
            sendColored(sender, prefix + "&cYour not Connected to the internet!");
            loggx(prefix + "&eThe plugin will start but you cant received any notification unless you use /fakedonation");
            sendColored(sender, prefix + "&eThe plugin will start but you cant received any notification unless you use /fakedonation");
            return false;
        }
        return false;
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

                    JsonObject entry = flowPoll.get(token, clientId);
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
