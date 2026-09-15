package hgds.epicgrief.packetlib.inventory;

import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.action.ClickAction;
import hgds.epicgrief.api.inventory.action.ClickActionWithCursor;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.listener.GListener;
import hgds.epicgrief.market.Market;

public class GuiManagerListener extends GListener<Market> {
    GuiManagerListener(Market javaPlugin) {
        super(javaPlugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        GInventory inventory = getHolder(e.getView().getTopInventory());
        if (inventory == null)
            return;
        e.setCancelled((e.getClick().isShiftClick() || inventory.isDisableAction()));
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getHandle().getSize())
            return;
        e.setCancelled(true);
        GItem item = inventory.getItems().get(slot);
        if (item == null || inventory.getHandle().getItem(slot) == null)
            return;
        ClickAction clickAction = item.getClickAction();
        if (clickAction instanceof ClickActionWithCursor)
            ((ClickActionWithCursor) clickAction).setCursor(e.getCursor());
        clickAction.onClick((Player) e.getWhoClicked(), e.getClick(), slot);
        if (clickAction instanceof ClickActionWithCursor)
            e.setCursor(((ClickActionWithCursor) clickAction).getCursor());
    }

    private GInventory getHolder(Inventory inventory) {
        if (inventory == null)
            return null;
        return (inventory.getHolder() instanceof GInventory) ? (GInventory) inventory.getHolder() : null;
    }

    @EventHandler
    public void onDisableDrag(InventoryDragEvent e) {
        GInventory inventory = getHolder(e.getInventory());
        if (inventory == null)
            return;
        e.setCancelled(inventory.isDisableAction());
        for (Iterator<Integer> iterator = e.getRawSlots().iterator(); iterator.hasNext(); ) {
            int slot = iterator.next();
            if (slot >= 0 && slot < inventory.size())
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCloseInv(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        GInventory inventory = getHolder(e.getInventory());
        if (inventory == null)
            return;
        inventory.getInventoryAction().onClose(player);
    }
}
