package hgds.epicgrief.market;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.market.auction.AuctionItemImpl;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.command.AuctionCommand;
import hgds.epicgrief.market.command.ReloadConfigCommand;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.player.PlayerListener;

public class Market extends JavaPlugin {
    private static Market instance;

    private AuctionCommand auctionCommand;

    private AuctionManager auctionManager;

    private FileConfiguration config;

    public static Market getInstance() {
        return instance;
    }

    public AuctionManager getAuctionManager() {
        return this.auctionManager;
    }

    public void onEnable() {
        instance = this;
        this.config = GalaxyAPI.getConfigAPI().loadConfig(this, "configShop.yml");
        GalaxyAPI.getConfigAPI().loadConfig(this, "auction.yml");
        Lang.load(this);
        this.auctionManager = new AuctionManager(this);
        reloadConfig();
        new PlayerListener(this);
        new ReloadConfigCommand(this);
    }

    private void loadLimitAuction() {
        Map<String, Integer> limits = new HashMap<>();
        for (String string : this.config.getStringList("limits")) {
            if (!string.contains(":"))
                continue;
            String[] split = string.split(":");
            String groupName = split[0];
            int limit = Integer.parseInt(split[1]) - 1;
            if (groupName.equals("default"))
                continue;
            limits.put(groupName, Integer.valueOf(limit));
        }
        limits.put("default", Integer.valueOf(this.config.getInt("limitDefault") - 1));
        this.auctionManager.setLimitItemsToSell(limits);
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void reloadConfig() {
        this.auctionManager.saveConfig();
        loadLimitAuction();
        this.auctionManager.clearAll();
        loadAuction();
    }

    private void loadAuction() {
        FileConfiguration config = GalaxyAPI.getConfigAPI().loadConfig(this, "auction.yml");
        this.auctionManager.setConfig(config);
        ConfigurationSection section = config.getConfigurationSection("Items");
        if (section != null && section.getKeys(false) != null)
            for (String uuidString : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String patch = "Items." + uuidString + ".";
                String owner = config.getString(patch + "owner");
                ItemStack itemStack = config.getItemStack(patch + "item");
                int price = config.getInt(patch + "price");
                long date = config.getLong(patch + "date");
                AuctionItemImpl item = new AuctionItemImpl(this.auctionManager, uuid, owner, itemStack, price, date);
                this.auctionManager.getAllItems().put(item.getID(), item);
            }
        if (this.auctionCommand != null)
            return;
        this.auctionCommand = new AuctionCommand(this.auctionManager, this.config);
    }

    public static double round(double d) {
        return (d > 0.0D) ? (Math.floor(d * 100.0D + 1.0E-6D) / 100.0D) : (Math.ceil(d * 100.0D) / 100.0D);
    }
}
