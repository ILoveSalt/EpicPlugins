package hgds.epicgrief.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class GEvent extends Event {
    protected GEvent() {}

    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected GEvent(boolean async) {
        super(async);
    }

    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
