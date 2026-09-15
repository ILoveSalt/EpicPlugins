package hgds.epicgrief.market.auction.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.builders.item.ItemBuilder;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.market.auction.AuctionItemType;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.messaging.Lang;

public class AuctionTypeMainGui {
    private static final InventoryAPI INVENTORY_API = GalaxyAPI.getInventoryAPI();

    private final AuctionManager manager;

    private final GInventory inventory;

    public AuctionTypeMainGui(AuctionManager auctionManager) {
        this.manager = auctionManager;
        this.inventory = INVENTORY_API.createInventory(Lang.getMessage("AUCTION_MAINGUI_NAME"), 6);
        setItems();
    }

    private void setItems() {
        int slot = 10;
        for (AuctionItemType type : AuctionItemType.values()) {
            if (type != AuctionItemType.ALL) {
                this.inventory.setItem(slot++, new GItem(ItemBuilder.newBuilder(type.getItem())
                        .removeFlags()
                        .setName(type.getName())
                        .setLore(Lang.getList("AUCTION_SUB_TYPE_LORE"))
                        .create(), (player, clickType, i) -> this.manager.openTypeGui(player, type)));
                if ((slot - 8) % 9 == 0)
                    slot += 2;
            }
        }
        this.inventory.setItem(49, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                .setName(Lang.getMessage("PROFILE_BACK_ITEM_NAME"))
                .setLore(Lang.getList("PROFILE_BACK_ITEM_LORE2"))
                .create(), (player, clickType, i) -> this.manager.openMainGui(player)));
    }

    public void open(Player player) {
        if (this.inventory == null)
            return;
        this.inventory.openInventory(player);
    }
}
