package hgds.epicgrief.packetlib.inventory.inventories;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.inventory.type.MultiInventory;

public class CraftMultiInventory implements MultiInventory {
    private static final InventoryAPI API = GalaxyAPI.getInventoryAPI();

    private GInventory lastUsed;

    private final List<GInventory> inventories = new ArrayList<>();

    private final Player player;

    private final int rows;

    private final String name;

    public CraftMultiInventory(Player player, String name, int rows) {
        this.player = player;
        this.name = name;
        this.rows = rows;
        GInventory inventory = API.createInventory(player, name, rows);
        this.inventories.add(inventory);
    }

    public void openInventory(Player player, int page) {
        GInventory inventory = this.inventories.get(page);
        this.lastUsed = inventory;
        if (inventory == null)
            inventory = this.inventories.get(0);
        inventory.openInventory(player);
    }

    public void openInventory(Player player) {
        player.openInventory(((GInventory)this.inventories.get(0)).getHandle());
    }

    public void setDisableAction(boolean action) {
        this.inventories.forEach(gInventory -> gInventory.setDisableAction(action));
    }

    public boolean isDisableAction() {
        return ((GInventory)this.inventories.get(0)).isDisableAction();
    }

    public Inventory getHandle() {
        return this.lastUsed.getHandle();
    }

    public void setItem(int page, int slot, GItem item) {
        createPages(page);
        GInventory inventory = this.inventories.get(page);
        if (inventory == null)
            return;
        inventory.setItem(slot, item);
    }

    public void addItem(int page, GItem item) {
        createPages(page);
        GInventory inventory = this.inventories.get(page);
        inventory.addItem(item);
    }

    public void setItem(int slot, GItem item) {
        this.inventories.forEach(inv -> inv.setItem(slot, item));
    }

    public void removeItem(int page, int slot) {
        if (page > this.inventories.size())
            return;
        GInventory inventory = this.inventories.get(page);
        inventory.removeItem(slot);
    }

    public void removePage(int page) {
        this.inventories.remove(page);
    }

    public void clearInventories() {
        this.inventories.forEach(GInventory::clearInventory);
    }

    public String getName() {
        return this.name;
    }

    public int size() {
        return this.rows * 9;
    }

    public int pages() {
        return this.inventories.size();
    }

    public List<GInventory> getInventories() {
        return this.inventories;
    }

    private void createPages(int page) {
        page++;
        if (page > this.inventories.size())
            for (int i = 0; i < page; i++) {
                if (this.inventories.size() < i + 1)
                    if (i != 0)
                        this.inventories.add(API.createInventory(this.player, this.name + " | " + (i + 1), this.rows));
            }
    }
}

