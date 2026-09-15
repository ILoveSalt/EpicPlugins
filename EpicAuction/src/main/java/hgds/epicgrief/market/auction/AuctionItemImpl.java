package hgds.epicgrief.market.auction;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import hgds.epicgrief.api.utils.BukkitUtil;
import hgds.epicgrief.api.utils.StringUtil;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.api.MarketAPI;
import hgds.epicgrief.market.api.MarketPlayer;
import hgds.epicgrief.market.api.MarketPlayerManager;
import hgds.epicgrief.market.api.event.PlayerBuyAuctionItemEvent;
import hgds.epicgrief.market.messaging.Lang;

public class AuctionItemImpl implements AuctionItem {
    private static final MarketPlayerManager MARKET_PLAYER_MANAGER = MarketAPI.getMarketPlayerManager();

    private final AuctionManager manager;

    private final UUID uuid;

    private final String owner;

    private final ItemStack item;

    private final int price;

    private final AuctionItemType typeItem;

    private final long time;

    private boolean available = true;

    public AuctionItemImpl(AuctionManager manager, UUID uuid, String owner, ItemStack item, int price, long time) {
        if (uuid == null)
            throw new NullPointerException("uuid is marked non-null but is null");
        if (owner == null)
            throw new NullPointerException("owner is marked non-null but is null");
        this.manager = manager;
        this.uuid = uuid;
        this.owner = owner;
        this.item = item;
        this.price = price;
        this.typeItem = AuctionItemType.getType(item);
        this.time = time;
    }

    @Override
    public ItemStack getItem() {
        return this.item.clone();
    }

    @Override
    public UUID getID() {
        return this.uuid;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public void buy(Player player) {
        MarketPlayer marketPlayer = MARKET_PLAYER_MANAGER.getMarketPlayer(player);
        if (marketPlayer == null)
            return;
        if (!this.manager.getAllItems().containsKey(this.uuid)) {
            Lang.sendMessage(player, "AUCTION_ITEM_ALLREADY_SELLED");
            return;
        }
        if (!marketPlayer.hasMoney(this.price)) {
            Lang.sendMessage(player, "AUCTION_NO_MONEY");
            return;
        }
        PlayerBuyAuctionItemEvent event = new PlayerBuyAuctionItemEvent(player, this);
        BukkitUtil.callEvent((Event) event);
        if (event.isCancelled())
            return;
        marketPlayer.changeMoney(-this.price);
        Lang.sendMessage(player, "AUCTION_BUY", this.item.getType().toString(),
                this.item.getAmount(), StringUtil.getNumberFormat(this.price));
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.addItem(this.item.clone());
        this.available = false;
        this.manager.remove(this);
        BukkitUtil.runTaskAsync(() -> {
            MarketPlayer seller = MARKET_PLAYER_MANAGER.getOrCreate(this.owner);
            seller.changeMoney(this.price);
            Player ownerPlayer = Bukkit.getPlayerExact(this.owner);
            if (ownerPlayer == null)
                return;
            Lang.sendMessage(ownerPlayer, "AUCTION_BUY_SELLER", player.getDisplayName(),
                    this.item.getType().toString(), this.item.getAmount(), StringUtil.getNumberFormat(this.price));
        });
    }

    @Override
    public AuctionItemType getType() {
        return this.typeItem;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() > this.time);
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public void remove() {
        this.available = false;
        this.manager.remove(this);
    }
}
