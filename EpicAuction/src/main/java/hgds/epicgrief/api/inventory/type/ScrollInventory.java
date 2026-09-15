package hgds.epicgrief.api.inventory.type;

import java.util.List;
import hgds.epicgrief.api.inventory.GItem;

public interface ScrollInventory extends BaseInventory {
    void addItemScroll(GItem paramGItem);

    void addItemsScroll(List<GItem> paramList);

    void removeItemScroll(int paramInt);

    void removeItemsScroll();

    void clearInventory();

    String getName();

    void setItem(int paramInt, GItem paramGItem);
}
