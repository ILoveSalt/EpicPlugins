package hgds.epicgrief;

import org.bukkit.inventory.ItemStack;

public class FurnaceInfo {
    private String name;

    private ItemStack item;

    private int burnTime;

    private int result;

    private int cooktime;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getCooktime() {
        return this.cooktime;
    }

    public void setCooktime(int cooktime) {
        this.cooktime = cooktime;
    }
}
