package hgds.epicgrief.api.inventory.type;

import java.util.Map;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.action.InventoryAction;

public interface GInventory extends BaseInventory {
    void setItem(int slot, GItem item);

    default void setItem(int x, int y, GItem item) {
        setItem(9 * y + x - 10, item);
    }

    void addItem(GItem item);

    String getName();

    void removeItem(int slot);

    void clearInventory();

    int size();

    Map<Integer, GItem> getItems();

    void createInventoryAction(InventoryAction inventoryAction);

    InventoryAction getInventoryAction();
}
