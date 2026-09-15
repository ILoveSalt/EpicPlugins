package hgds.epicgrief.market.api;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.market.auction.AuctionItemType;

public interface AuctionItem {
    UUID getID();

    ItemStack getItem();

    String getOwner();

    int getPrice();

    void buy(Player paramPlayer);

    AuctionItemType getType();

    long getTime();

    boolean isExpired();

    boolean isAvailable();

    void remove();
}