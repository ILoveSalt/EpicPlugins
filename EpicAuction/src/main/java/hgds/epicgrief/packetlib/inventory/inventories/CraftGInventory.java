package hgds.epicgrief.packetlib.inventory.inventories;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.action.InventoryAction;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.utils.Head;
import hgds.epicgrief.market.Market;

public class CraftGInventory implements GInventory {
    private static final Market JAVA_PLUGIN = Market.getInstance();

    private final Inventory handle;

    private final String name;

    private final int rows;

    private final Map<Integer, GItem> items = new ConcurrentHashMap<>();

    private boolean disableAction;

    private InventoryAction inventoryAction = new InventoryAction() {
    };

    public CraftGInventory(String name, int rows, InventoryAction inventoryAction) {
        this.name = name;
        this.rows = rows;
        this.handle = Bukkit.createInventory((InventoryHolder) this, 9 * rows, name);
        this.disableAction = true;
        if (inventoryAction != null)
            this.inventoryAction = inventoryAction;
    }

    @Override
    public Inventory getHandle() {
        return this.handle;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<Integer, GItem> getItems() {
        return this.items;
    }

    @Override
    public boolean isDisableAction() {
        return this.disableAction;
    }

    @Override
    public void setDisableAction(boolean disableAction) {
        this.disableAction = disableAction;
    }

    @Override
    public InventoryAction getInventoryAction() {
        return this.inventoryAction;
    }

    @Override
    public void setItem(int slot, GItem item) {
        ItemStack stack = item.getItem();
        this.items.put(slot, item);
        if (!isHead(stack)) {
            this.handle.setItem(slot, stack);
            return;
        }
        if (stack.getItemMeta() instanceof SkullMeta && ((SkullMeta) stack.getItemMeta()).getOwningPlayer() == null
                && ((SkullMeta) stack.getItemMeta()).getPlayerProfile() == null) {
            this.handle.setItem(slot, stack);
            return;
        }
        // Профили голов подгружаются асинхронно — ставим сначала «чистую» голову,
        // затем обновляем слот (на новых версиях Paper делать это можно только в основном потоке)
        this.handle.setItem(slot, Head.getPlayerHead());
        Bukkit.getScheduler().runTaskLater(JAVA_PLUGIN, () -> this.handle.setItem(slot, stack), 1L);
    }

    private boolean isHead(ItemStack stack) {
        return (stack != null && stack.getType() != Material.AIR && stack.getItemMeta() instanceof SkullMeta);
    }

    @Override
    public void addItem(GItem gItem) {
        for (int slot = 0; slot < this.handle.getSize(); slot++) {
            ItemStack current = this.handle.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                setItem(slot, gItem);
                return;
            }
        }
    }

    @Override
    public void removeItem(int slot) {
        GItem item = this.items.remove(slot);
        if (item == null)
            return;
        this.handle.setItem(slot, null);
    }

    @Override
    public void clearInventory() {
        this.items.clear();
        this.handle.clear();
    }

    @Override
    public int size() {
        return this.rows * 9;
    }

    @Override
    public void createInventoryAction(InventoryAction inventoryAction) {
        this.inventoryAction = inventoryAction;
    }
}
