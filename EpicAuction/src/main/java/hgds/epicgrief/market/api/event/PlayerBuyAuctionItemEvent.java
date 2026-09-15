package hgds.epicgrief.market.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import hgds.epicgrief.api.event.PlayerEvent;
import hgds.epicgrief.market.api.AuctionItem;

public class PlayerBuyAuctionItemEvent extends PlayerEvent implements Cancellable {
    private final AuctionItem auctionItem;

    private boolean cancelled;

    public AuctionItem getAuctionItem() {
        return this.auctionItem;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public PlayerBuyAuctionItemEvent(Player player, AuctionItem auctionItem) {
        super(player);
        this.auctionItem = auctionItem;
    }
}
