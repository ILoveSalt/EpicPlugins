package hgds.epicgrief.api.inventory;

import java.util.List;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.inventory.action.InventoryAction;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.inventory.type.MultiInventory;
import hgds.epicgrief.api.inventory.type.ScrollInventory;

public interface InventoryAPI {
    GInventory createInventory(Player paramPlayer, String paramString, int paramInt, InventoryAction paramInventoryAction);

    GInventory createInventory(Player paramPlayer, String paramString, int paramInt);

    GInventory createInventory(String paramString, int paramInt);

    GInventory createInventory(Player paramPlayer, int paramInt, String paramString, Object... paramVarArgs);

    GInventory createInventory(int paramInt, String paramString, Object... paramVarArgs);

    MultiInventory createMultiInventory(Player paramPlayer, String paramString, int paramInt);

    MultiInventory createMultiInventory(String paramString, int paramInt);

    MultiInventory createMultiInventory(Player paramPlayer, int paramInt, String paramString, Object... paramVarArgs);

    MultiInventory createMultiInventory(int paramInt, String paramString, Object... paramVarArgs);

    ScrollInventory createScrollInventory(Player paramPlayer, String paramString);

    ScrollInventory createScrollInventory(String paramString);

    void pageButton(int paramInt1, List<GInventory> paramList, int paramInt2, int paramInt3);

    void pageButton(int paramInt1, MultiInventory paramMultiInventory, int paramInt2, int paramInt3);
}
