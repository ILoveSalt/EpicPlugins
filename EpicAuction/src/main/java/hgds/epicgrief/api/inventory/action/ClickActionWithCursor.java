package hgds.epicgrief.api.inventory.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class ClickActionWithCursor implements ClickAction {
    private ItemStack cursor;

    public void setCursor(ItemStack cursor) {
        this.cursor = cursor;
    }

    public ItemStack getCursor() {
        return this.cursor;
    }

    public final void onClick(Player clicker, ClickType clickType, int slot) {
        this.cursor = onClick(clicker, clickType, this.cursor, slot);
    }

    public abstract ItemStack onClick(Player paramPlayer, ClickType paramClickType, ItemStack paramItemStack, int paramInt);
}
