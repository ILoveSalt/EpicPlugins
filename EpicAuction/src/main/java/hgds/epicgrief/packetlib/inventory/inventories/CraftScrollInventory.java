package hgds.epicgrief.packetlib.inventory.inventories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.inventory.type.ScrollInventory;

public class CraftScrollInventory implements ScrollInventory {
    private static final InventoryAPI API = GalaxyAPI.getInventoryAPI();

    private final GInventory inventory;

    private final List<GItem> scrollItem = new ArrayList<>();

    private final Map<Integer, GItem> defaultItems = new HashMap<>();

    private boolean disableAction;

    public boolean isDisableAction() {
        return this.disableAction;
    }

    public void setDisableAction(boolean disableAction) {
        this.disableAction = disableAction;
    }

    public CraftScrollInventory(Player player, String name) {
        this.inventory = API.createInventory(player, name, 5);
        this.disableAction = true;
    }

    public void openInventory(Player player) {
        this.inventory.openInventory(player);
    }

    public void addItemScroll(GItem item) {
        this.scrollItem.add(item);
        setButtonScroll();
    }

    public void addItemsScroll(List<GItem> items) {
        this.scrollItem.addAll(items);
        setButtonScroll();
    }

    public void removeItemScroll(int numberItem) {
        this.scrollItem.remove(numberItem);
    }

    public void removeItemsScroll() {
        this.scrollItem.clear();
    }

    public void clearInventory() {
        this.inventory.clearInventory();
        this.scrollItem.clear();
        this.defaultItems.clear();
    }

    public String getName() {
        return this.inventory.getName();
    }

    public void setItem(int slot, GItem item) {
        this.inventory.setItem(slot, item);
        this.defaultItems.put(Integer.valueOf(slot), item);
    }

    private void setButtonScroll() {}

    public Inventory getHandle() {
        return this.inventory.getHandle();
    }
}
