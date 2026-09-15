package hgds.epicgrief.api.inventory.type;

import java.util.List;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.inventory.GItem;

public interface MultiInventory extends BaseInventory {
    void openInventory(Player paramPlayer, int paramInt);

    void setItem(int paramInt1, int paramInt2, GItem paramGItem);

    void addItem(int paramInt, GItem paramGItem);

    void setItem(int paramInt, GItem paramGItem);

    void removeItem(int paramInt1, int paramInt2);

    void removePage(int paramInt);

    void clearInventories();

    String getName();

    int size();

    int pages();

    List<GInventory> getInventories();
}
