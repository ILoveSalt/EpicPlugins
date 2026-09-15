package hgds.epicgrief.api.event;

import org.bukkit.entity.Player;

public abstract class PlayerEvent extends GEvent {
    private final Player player;

    public Player getPlayer() {
        return this.player;
    }

    protected PlayerEvent(Player player) {
        this(player, false);
    }

    protected PlayerEvent(Player player, boolean async) {
        super(async);
        this.player = player;
    }
}
