package hgds.epicgrief.market.auction.gui;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.builders.item.ItemBuilder;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.MultiInventory;
import hgds.epicgrief.api.utils.TimeLeftFormat;
import hgds.epicgrief.api.utils.TimeUtil;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.utils.MarketUtil;

public class AuctionMyGui {
    private static final InventoryAPI INVENTORY_API = GalaxyAPI.getInventoryAPI();

    private final AuctionManager manager;

    private MultiInventory inventory;

    private final Player player;

    public AuctionMyGui(AuctionManager auctionManager, Player player) {
        this.manager = auctionManager;
        this.player = player;
        this.inventory = INVENTORY_API.createMultiInventory(player,
                Lang.getMessage("AUCTION_GUI_NAME", player.getName()), 6);
        update();
    }

    public void update() {
        if (this.inventory == null)
            return;
        setItems();
    }

    private void setItems() {
        this.inventory.clearInventories();
        int slot = 10;
        int page = 0;
        for (AuctionItem auctionItem : this.manager.getAllItems().values().stream()
                .filter(item -> item.getOwner().equalsIgnoreCase(this.player.getName()))
                .sorted(Comparator.comparingLong(AuctionItem::getTime))
                .collect(Collectors.toList())) {
            boolean expired = auctionItem.isExpired();
            String time = TimeUtil.getTimeLeft((auctionItem.getTime() - System.currentTimeMillis()) / 1000L, TimeLeftFormat.DAYS);
            this.inventory.setItem(page, slot++, new GItem(
                    ItemBuilder.newBuilder(auctionItem.getItem())
                            .glowing(expired)
                            .setLore(Lang.getList("AUCTION_LORE_MYGUI", auctionItem.getPrice(),
                                    expired ? Lang.getMessage("AUCTION_NO_AUCTION") : time)).create(),
                    (clicker, clickType, i) -> {
                        if (clickType.isLeftClick())
                            return;
                        if (MarketUtil.isFull(clicker.getInventory())) {
                            clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F);
                            Lang.sendMessage(clicker, "INVENTORY_IS_FULL");
                            clicker.closeInventory();
                            return;
                        }
                        if (!auctionItem.isAvailable())
                            return;
                        auctionItem.remove();
                        Lang.sendMessage(clicker, "AUCTION_REMOVED_ITEM");
                        clicker.getInventory().addItem(auctionItem.getItem());
                    }));
            if ((slot - 8) % 9 == 0)
                slot += 2;
            if (slot >= 44) {
                slot = 10;
                page++;
            }
        }
        MarketUtil.setBack(this.manager, this.inventory);
        if (slot == 10 && page == 0) {
            this.inventory.setItem(0, 22, new GItem(ItemBuilder.newBuilder(Material.BARRIER)
                    .setName(Lang.getMessage("AUCTION_NO_ITEMS_NAME"))
                    .setLore(Lang.getList("AUCTION_NO_ITEMS_LORE"))
                    .create(), (clicker, clickType, i) -> clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F)));
            return;
        }
        INVENTORY_API.pageButton(page + 1, this.inventory, 47, 51);
    }

    public void open() {
        this.inventory.openInventory(this.player);
    }
}
