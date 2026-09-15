package hgds.epicgrief.market.auction.gui;

import org.bukkit.entity.Player;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.MultiInventory;
import hgds.epicgrief.market.auction.AuctionManager;

public abstract class AuctionAbstractGui {
    protected static final InventoryAPI INVENTORY_API = GalaxyAPI.getInventoryAPI();

    protected final AuctionManager manager;

    protected final MultiInventory inventory;

    AuctionAbstractGui(AuctionManager manager, String name) {
        this.manager = manager;
        this.inventory = INVENTORY_API.createMultiInventory(name, 6);
        update();
    }

    public void update() {
        if (this.inventory == null)
            return;
        setItems();
    }

    public void open(Player player) {
        if (this.inventory == null)
            return;
        this.inventory.openInventory(player);
    }

    protected abstract void setItems();
}
