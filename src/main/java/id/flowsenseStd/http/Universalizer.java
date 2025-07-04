package id.flowsenseStd.http;

import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Universalizer {
    public String id;
    public int provider;
    public int amount;
    public String donator_name;
    public String donator_email;
    public String message;
    public String unit;
    public int unit_qty;
    public String created_at;

    // Method utama buat convert dari JsonObject ke universal object
    public static Universalizer from(JsonObject json, int provider) {
        Universalizer entry = new Universalizer();
        entry.provider = provider;
        entry.created_at = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        if (provider == 1) { // Saweria
            entry.id = json.has("id") ? json.get("id").getAsString() : "unknown";
            entry.amount = json.has("amount_raw") ? json.get("amount_raw").getAsInt() : 0;
            entry.donator_name = json.has("donator_name") ? json.get("donator_name").getAsString() : "";
            entry.donator_email = json.has("donator_email") ? json.get("donator_email").getAsString() : "";
            entry.message = json.has("message") ? json.get("message").getAsString() : "";
            entry.unit = "";
            entry.unit_qty = 0;

        } else if (provider == 2) { // Tako
            entry.id = json.has("id") ? json.get("id").getAsString() : "unknown";
            entry.amount = json.has("amount") ? json.get("amount").getAsInt() : 0;
            entry.donator_name = json.has("gifterName") ? json.get("gifterName").getAsString() : "";
            entry.donator_email = json.has("gifterEmail") ? json.get("gifterEmail").getAsString() : "";
            entry.message = json.has("message") ? json.get("message").getAsString() : "";
            entry.unit = "";
            entry.unit_qty = 0;

        } else if (provider == 3) { // Trakteer
            entry.id = json.has("transaction_id") ? json.get("transaction_id").getAsString() : "unknown";
            entry.amount = json.has("net_amount") ? json.get("net_amount").getAsInt() : 0;
            entry.donator_name = json.has("supporter_name") ? json.get("supporter_name").getAsString() : "";
            entry.donator_email = ""; // not available
            entry.message = json.has("supporter_message") ? json.get("supporter_message").getAsString() : "";
            entry.unit = json.has("unit") ? json.get("unit").getAsString() : "";
            entry.unit_qty = json.has("quantity") ? json.get("quantity").getAsInt() : 0;

        } else {
            // Unknown provider
            entry.id = "unknown";
            entry.amount = 0;
            entry.donator_name = "";
            entry.donator_email = "";
            entry.message = "";
            entry.unit = "";
            entry.unit_qty = 0;
        }

        return entry;
    }

    // Convert kembali ke JsonObject (kalau perlu)
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("provider", provider);
        json.addProperty("amount", amount);
        json.addProperty("donator_name", donator_name);
        json.addProperty("donator_email", donator_email);
        json.addProperty("message", message);
        json.addProperty("unit", unit);
        json.addProperty("unit_qty", unit_qty);
        json.addProperty("created_at", created_at);
        return json;
    }

    // Penentu jenis provider berdasarkan struktur JSON
    public static int providerDeterminer(JsonObject json) {
        if (json.has("version") && json.has("donator_name") && json.has("amount_raw")) {
            return 1; // Saweria
        }
        if (json.has("soundboardName") && json.has("pollingTitle") && json.has("gifterName")) {
            return 2; // Tako
        }
        if (json.has("transaction_id") && json.has("supporter_name") && json.has("unit")) {
            return 3; // Trakteer
        }
        return 0; // Unknown
    }
}

