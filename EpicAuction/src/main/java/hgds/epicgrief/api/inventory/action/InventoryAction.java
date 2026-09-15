package hgds.epicgrief.api.inventory.action;

import org.bukkit.entity.Player;

public interface InventoryAction {
    default void onOpen(Player player) {}

    default void onClose(Player player) {}
}