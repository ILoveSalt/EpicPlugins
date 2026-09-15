package hgds.epicgrief.api.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.api.inventory.action.ClickAction;

public class GItem {
    private ItemStack item;

    private ClickAction clickAction;

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void setClickAction(ClickAction clickAction) {
        this.clickAction = clickAction;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public ClickAction getClickAction() {
        return this.clickAction;
    }

    public GItem(ItemStack itemStack, ClickAction clickAction) {
        this.item = itemStack;
        this.clickAction = clickAction;
    }

    public GItem(ItemStack itemStack) {
        this(itemStack, (player, clickType, slot) -> {

        });
    }
}
