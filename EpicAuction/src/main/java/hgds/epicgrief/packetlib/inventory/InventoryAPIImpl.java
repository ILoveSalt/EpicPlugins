package hgds.epicgrief.packetlib.inventory;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.builders.item.ItemBuilder;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.action.InventoryAction;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.inventory.type.MultiInventory;
import hgds.epicgrief.api.inventory.type.ScrollInventory;
import hgds.epicgrief.market.Market;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.packetlib.inventory.inventories.CraftGInventory;
import hgds.epicgrief.packetlib.inventory.inventories.CraftMultiInventory;
import hgds.epicgrief.packetlib.inventory.inventories.CraftScrollInventory;

public class InventoryAPIImpl implements InventoryAPI {
    public InventoryAPIImpl(Market javaPlugin) {
        new GuiManagerListener(javaPlugin);
    }

    public GInventory createInventory(Player player, String name, int rows, InventoryAction inventoryAction) {
        return (GInventory)new CraftGInventory(name, rows, inventoryAction);
    }

    public GInventory createInventory(Player player, String name, int rows) {
        return createInventory(player, name, rows, (InventoryAction)null);
    }

    public GInventory createInventory(String name, int rows) {
        return createInventory((Player)null, name, rows);
    }

    public GInventory createInventory(Player player, int rows, String key, Object... objects) {
        return createInventory(player, Lang.getMessage(key, objects), rows);
    }

    public GInventory createInventory(int rows, String key, Object... objects) {
        return createInventory(Lang.getMessage(key, objects), rows);
    }

    public MultiInventory createMultiInventory(Player player, String name, int rows) {
        return (MultiInventory)new CraftMultiInventory(player, name, rows);
    }

    public MultiInventory createMultiInventory(String name, int rows) {
        return createMultiInventory((Player)null, name, rows);
    }

    public MultiInventory createMultiInventory(Player player, int rows, String key, Object... objects) {
        return createMultiInventory(player, Lang.getMessage(key, objects), rows);
    }

    public MultiInventory createMultiInventory(int rows, String key, Object... objects) {
        return createMultiInventory(Lang.getMessage(key, objects), rows);
    }

    public ScrollInventory createScrollInventory(Player player, String name) {
        return (ScrollInventory)new CraftScrollInventory(player, name);
    }

    public ScrollInventory createScrollInventory(String name) {
        return createScrollInventory(null, name);
    }

    public void pageButton(int pagesCount, List<GInventory> pages, int slotDown, int slotUp) {
        for (int i = 0; i < pages.size(); i++) {
            int finalI = i;
            if (i == 0 && pagesCount > 1 && pages.size() > 1) {
                ((GInventory)pages.get(i)).setItem(slotUp, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                        .setName(Lang.getMessage("PAGE_ARROW1", new Object[0]))
                        .setLore(Lang.getList("PAGE_ARROW_LORE", new Object[] { Integer.valueOf(i + 2) })).create(), (player, clickType, slot) -> ((GInventory)pages.get(finalI + 1)).openInventory(player)));
            } else if (i > 0 && i < pagesCount - 1 && pages.size() > i + 1) {
                ((GInventory)pages.get(i)).setItem(slotDown, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                        .setName(Lang.getMessage("PAGE_ARROW2", new Object[0]))
                        .setLore(Lang.getList("PAGE_ARROW_LORE", new Object[] { Integer.valueOf(i) })).create(), (player, clickType, slot) -> ((GInventory)pages.get(finalI - 1)).openInventory(player)));
                ((GInventory)pages.get(i)).setItem(slotUp, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                        .setName(Lang.getMessage("PAGE_ARROW1", new Object[0]))
                        .setLore(Lang.getList("PAGE_ARROW_LORE", new Object[] { Integer.valueOf(i + 2) })).create(), (player, clickType, slot) -> ((GInventory)pages.get(finalI + 1)).openInventory(player)));
            } else if (pages.size() > 1 && pagesCount > 1) {
                ((GInventory)pages.get(i)).setItem(slotDown, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                        .setName(Lang.getMessage("PAGE_ARROW2", new Object[0]))
                        .setLore(Lang.getList("PAGE_ARROW_LORE", new Object[] { Integer.valueOf(i) })).create(), (player, clickType, slot) -> ((GInventory)pages.get(finalI - 1)).openInventory(player)));
            }
        }
    }

    public void pageButton(int pagesCount, MultiInventory inventory, int slotDown, int slotUp) {
        pageButton(pagesCount, inventory.getInventories(), slotDown, slotUp);
    }
}