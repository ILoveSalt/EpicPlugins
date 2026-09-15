package hgds.epicgrief.api.inventory.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface ClickAction {
    void onClick(Player paramPlayer, ClickType paramClickType, int paramInt);
}