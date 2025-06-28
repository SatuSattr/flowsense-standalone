package id.flowsense;

import com.google.gson.JsonObject;
import io.github.milkdrinkers.colorparser.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class BodyProcess {

    private final FileConfiguration config;
    private final Logger logger;

    public BodyProcess(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    // 🔁 Format: Ubah JSON donasi ke String berisi placeholder
    public String Format(JsonObject entry) {
        try {
            String createdAt = entry.get("created_at").getAsString();
            int idx = entry.get("id").getAsInt();
            int amountRaw = entry.get("amount").getAsInt();
            String donatorName = entry.get("donator_name").getAsString();
            String donatorEmail = entry.get("donator_email").getAsString();
            String messagex = entry.get("message").getAsString();
            String unitx = entry.get("unit").getAsString();
            int unitqty = entry.get("unit_qty").getAsInt();
            String amountFormatted_US = NumberFormat.getNumberInstance(Locale.US).format(amountRaw);
            String amountFormatted_DE = NumberFormat.getNumberInstance(Locale.GERMAN).format(amountRaw);

            int providerId = entry.get("provider").getAsInt();
            String providerName = switch (providerId) {
                case 1 -> "Saweria";
                case 2 -> "Tako";
                case 3 -> "Trakteer";
                default -> "Unknown";
            };

            String template = config.getString("message", "");
            return template
                    .replace("{id}", String.valueOf(idx))
                    .replace("{created_at}", createdAt)
                    .replace("{provider_id}", String.valueOf(providerId))
                    .replace("{provider_name}", providerName)
                    .replace("{amount_raw}", String.valueOf(amountRaw))
                    .replace("{amount_formatted}", amountFormatted_US)
                    .replace("{amount_formatted_US}", amountFormatted_US)
                    .replace("{amount_formatted_DE}", amountFormatted_DE)
                    .replace("{donator_name}", donatorName)
                    .replace("{donator_email}", donatorEmail)
                    .replace("{message}", messagex)
                    .replace("{unit}", unitx)
                    .replace("{unit_qty}", String.valueOf(unitqty));
        } catch (Exception e) {
            logger.severe("Error in BodyProcess.Format:");
            e.printStackTrace();
            return "[ERROR] Gagal memformat pesan donasi.";
        }
    }

    // 📣 Broadcast ke player (kalau diaktifin)
    public Component handle(JsonObject entry) {
        try {
            String formattedMessage = Format(entry);
            return ColorParser.of(formattedMessage).parseLegacy().build();
        } catch (Exception e) {
            logger.severe("Error in BodyProcess.handle:");
            e.printStackTrace();
            return Component.text("[ERROR] Gagal memproses pesan donasi.");
        }
    }

    // 🎯 Trigger: Eksekusi command berdasarkan amount_raw
    public void Trigger(JsonObject entry) {
        if (!config.getBoolean("donation-trigger", true)) return;

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                long amount = entry.get("amount").getAsLong();

                for (String key : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section == null || !section.contains("amount") || !section.contains("commands")) continue;

                    String condition = section.getString("amount");
                    List<String> commands = section.getStringList("commands");
                    int delay = section.getInt("delay", 0);

                    if (matchesCondition(amount, condition)) {
                        for (int i = 0; i < commands.size(); i++) {
                            String cmd = commands.get(i);
                            String parsed = FormatPlaceholder(cmd, entry);
                            int finalDelay = delay * i;

                            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                            }, finalDelay); // delay per index
                        }
                    }
                }
            } catch (Exception e) {
                logger.severe("Error in BodyProcess.Trigger:");
                e.printStackTrace();
            }
        });
    }


    // 🔍 Cek apakah amount cocok dengan kondisi
    private boolean matchesCondition(long amount, String condition) {
        if (condition.startsWith(">"))
            return amount > Long.parseLong(condition.substring(1));
        if (condition.startsWith("<"))
            return amount < Long.parseLong(condition.substring(1));
        if (condition.startsWith("=") || condition.isEmpty())
            return amount == Long.parseLong(condition.substring(1));
        return false;
    }

    // 🧩 Format ulang command sebelum dieksekusi
    private String FormatPlaceholder(String input, JsonObject entry) {
        String createdAt = entry.get("created_at").getAsString();
        int idx = entry.get("id").getAsInt();
        int amountRaw = entry.get("amount").getAsInt();
        String donatorName = entry.get("donator_name").getAsString();
        String donatorEmail = entry.get("donator_email").getAsString();
        String messagex = entry.get("message").getAsString();
        String unitx = entry.get("unit").getAsString();
        int unitqty = entry.get("unit_qty").getAsInt();
        String amountFormatted_US = NumberFormat.getNumberInstance(Locale.US).format(amountRaw);
        String amountFormatted_DE = NumberFormat.getNumberInstance(Locale.GERMAN).format(amountRaw);

        int providerId = entry.get("provider").getAsInt();
        String providerName = switch (providerId) {
            case 1 -> "Saweria";
            case 2 -> "Tako";
            case 3 -> "Trakteer";
            default -> "Unknown";
        };

        return input
                .replace("{id}", String.valueOf(idx))
                .replace("{created_at}", createdAt)
                .replace("{provider_id}", String.valueOf(providerId))
                .replace("{provider_name}", providerName)
                .replace("{amount_raw}", String.valueOf(amountRaw))
                .replace("{amount_formatted}", amountFormatted_US)
                .replace("{amount_formatted_US}", amountFormatted_US)
                .replace("{amount_formatted_DE}", amountFormatted_DE)
                .replace("{donator_name}", donatorName)
                .replace("{donator_email}", donatorEmail)
                .replace("{message}", messagex)
                .replace("{unit}", unitx)
                .replace("{unit_qty}", String.valueOf(unitqty));
    }

}
