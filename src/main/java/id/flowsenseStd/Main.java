package id.flowsenseStd;

import com.google.gson.JsonObject;
import io.github.milkdrinkers.colorparser.ColorParser;
import id.flowsenseStd.api.DonationEvent;
import id.flowsenseStd.http.Http;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Main extends JavaPlugin implements TabCompleter {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static BukkitAudiences audiences;
    private static Main instance;

    public static String prefix;
    private static boolean broadcastmssage, donationtrigger;
    private boolean iloveherxx = true;

    public BodyProcess processor;
    private Http httpServer;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        audiences = BukkitAudiences.create(this);
        saveDefaultConfig();
        getCommand("flowsense").setTabCompleter(this);

        reloadAll(false);

        long endTime = System.currentTimeMillis();
        loggx(prefix + "<green>Flowsense Enabled in " + (endTime - startTime) + " ms");
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop();
        }
        if (audiences != null) audiences.close();
    }

    public static void loggx(String s) {
        Component msg = ColorParser.of(s).parseLegacy().build();
        audiences.console().sendMessage(msg);
    }

    public static void sendColored(CommandSender sender, String message) {
        if (sender == null) return;
        Component msg = ColorParser.of(message).parseLegacy().build();
        audiences.sender(sender).sendMessage(msg);
    }

    public static Main getInstance() {
        return instance;
    }

    private boolean reloadAll(boolean isreload) {
        reloadConfig();
        FileConfiguration config = getConfig();
        processor = new BodyProcess(config, getLogger());
        prefix = config.getString("prefix", "&9[flowsense] ");
        broadcastmssage = config.getBoolean("broadcast-message", true);
        donationtrigger = config.getBoolean("donation-trigger", true);

        try {
            if (httpServer != null) httpServer.stop();
            httpServer = new Http();
            httpServer.start(config.getInt("port", 9876), config.getString("webhook-path", "/webhook"));
            iloveherxx = true;
            loggx(prefix + "&aFlowsense enabled on port &e" + config.getInt("port", 9876));
            return true;
        } catch (Exception e) {
            loggx(prefix + "&cError: " + e.getMessage());
            iloveherxx = false;
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("flowsense.reload")) {
                    sendColored(sender, prefix + "<red>Kamu tidak punya permission.");
                    return true;
                }
                sendColored(sender, prefix + "<gray>Reloading FlowSense...");
                boolean ok = reloadAll(true);
                sendColored(sender, prefix + (ok ? "<green>Reload sukses!" : "<red>Reload error."));
                return true;
            }

            if (args[0].equalsIgnoreCase("fakedonate") || args[0].equalsIgnoreCase("fd")) {
                if (!sender.hasPermission("flowsense.fakedonate")) {
                    sendColored(sender, prefix + "<red>Kamu tidak punya permission.");
                    return true;
                }

                if (args.length < 4) {
                    sendColored(sender, prefix + "<red>Usage:</red> /flowsense fakedonate <donator> <amount> <provider> [unit] [qty] <message>");
                    return true;
                }

                String donatorName = args[1];
                long amount;
                int provider;

                try {
                    amount = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sendColored(sender, prefix + "<red>Amount harus angka.");
                    return true;
                }

                try {
                    provider = Integer.parseInt(args[3]);
                    if (provider < 1 || provider > 3) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sendColored(sender, prefix + "<red>Provider harus 1 (Saweria), 2 (Tako), 3 (Trakteer).");
                    return true;
                }

                String unit = "";
                int unit_qty = 1;
                StringBuilder msgBuilder = new StringBuilder();

                if (provider == 3) {
                    if (args.length < 6) {
                        sendColored(sender, prefix + "<red>Provider 3 butuh <unit> dan <unit_qty>.");
                        return true;
                    }
                    unit = args[4];
                    try {
                        unit_qty = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        sendColored(sender, prefix + "<red>Qty harus angka.");
                        return true;
                    }
                    for (int i = 6; i < args.length; i++) msgBuilder.append(args[i]).append(" ");
                } else {
                    for (int i = 4; i < args.length; i++) msgBuilder.append(args[i]).append(" ");
                }

                String message = msgBuilder.toString().trim();
                LocalDateTime now = LocalDateTime.now();
                String createdAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                JsonObject fakeEntry = new JsonObject();
                fakeEntry.addProperty("id", (int) (Math.random() * 9999));
                fakeEntry.addProperty("created_at", createdAt);
                fakeEntry.addProperty("amount", amount);
                fakeEntry.addProperty("donator_name", donatorName);
                fakeEntry.addProperty("message", message);
                fakeEntry.addProperty("provider", provider);
                fakeEntry.addProperty("unit", unit);
                fakeEntry.addProperty("unit_qty", unit_qty);
                fakeEntry.addProperty("donator_email", "flowsense@example.com");

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
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("flowsense")) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("flowsense.reload") && "reload".startsWith(args[0])) suggestions.add("reload");
            if (sender.hasPermission("flowsense.fakedonate") && "fakedonate".startsWith(args[0])) suggestions.add("fakedonate");
            if (sender.hasPermission("flowsense.fakedonate") && "fd".startsWith(args[0])) suggestions.add("fd");
        }

        return suggestions;
    }
}
