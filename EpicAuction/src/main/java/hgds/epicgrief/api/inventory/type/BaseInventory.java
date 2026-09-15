package hgds.epicgrief.api.inventory.type;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface BaseInventory extends InventoryHolder {
    default void openInventory(Player player) {
        player.openInventory(getHandle());
    }

    void setDisableAction(boolean paramBoolean);

    boolean isDisableAction();

    Inventory getHandle();

    default Inventory getInventory() {
        return getHandle();
    }
}