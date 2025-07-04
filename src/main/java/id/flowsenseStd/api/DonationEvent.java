package id.flowsenseStd.api;

import com.google.gson.JsonObject;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DonationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final JsonObject donationBody;

    public DonationEvent(JsonObject donationBody) {
        this.donationBody = donationBody;
    }

    public JsonObject getBody() {
        return donationBody;
    }

    public String get(String key) {
        return donationBody.has(key) ? donationBody.get(key).getAsString() : null;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
