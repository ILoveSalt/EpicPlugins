package hgds.epicgrief.market.auction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.permission.PermissionsAPI;
import hgds.epicgrief.market.Market;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.auction.gui.AuctionAbstractGui;
import hgds.epicgrief.market.auction.gui.AuctionMainGui;
import hgds.epicgrief.market.auction.gui.AuctionMyGui;
import hgds.epicgrief.market.auction.gui.AuctionPlayerGui;
import hgds.epicgrief.market.auction.gui.AuctionTypeGui;
import hgds.epicgrief.market.auction.gui.AuctionTypeMainGui;

public class AuctionManager {
    private static final PermissionsAPI PERMISSIONS_API = GalaxyAPI.getPermissionsAPI();

    private final Map<UUID, AuctionItem> items = new ConcurrentHashMap<>();

    private final Map<String, Integer> limitItemsToSell = new HashMap<>();

    private final AuctionMainGui auctionMainGui;

    private final AuctionTypeMainGui auctionTypeMainGui;

    private final Map<AuctionItemType, AuctionTypeGui> typeGuis = new ConcurrentHashMap<>();

    private final Map<String, AuctionMyGui> ownerGuis = new ConcurrentHashMap<>();

    private final Map<String, AuctionPlayerGui> playersGui = new ConcurrentHashMap<>();

    private FileConfiguration config;

    private final Market market;

    public Map<String, AuctionMyGui> getOwnerGuis() {
        return this.ownerGuis;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public AuctionManager(Market market) {
        this.market = market;
        this.auctionMainGui = new AuctionMainGui(this);
        this.auctionTypeMainGui = new AuctionTypeMainGui(this);
        Arrays.stream(AuctionItemType.values()).forEach(type -> this.typeGuis.put(type, new AuctionTypeGui(this, type)));
        AtomicInteger count = new AtomicInteger();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                count.getAndIncrement();
                this.auctionMainGui.update();
                this.typeGuis.values().forEach(AuctionTypeGui::update);
                this.ownerGuis.values().forEach(AuctionMyGui::update);
                this.playersGui.values().forEach(AuctionAbstractGui::update);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (count.get() > 1000) {
                saveConfig();
                count.set(0);
            }
        }, 5L, 1L, TimeUnit.SECONDS);
    }

    public void clearAll() {
        this.items.clear();
    }

    public void add(AuctionItem auctionItem) {
        this.items.put(auctionItem.getID(), auctionItem);
        String uuid = auctionItem.getID().toString();
        this.config.set("Items." + uuid + ".item", auctionItem.getItem());
        this.config.set("Items." + uuid + ".owner", auctionItem.getOwner());
        this.config.set("Items." + uuid + ".price", auctionItem.getPrice());
        this.config.set("Items." + uuid + ".date", auctionItem.getTime());
        saveConfig();
    }

    public void setLimitItemsToSell(Map<String, Integer> limitItemsToSell) {
        this.limitItemsToSell.clear();
        this.limitItemsToSell.putAll(limitItemsToSell);
    }

    public void remove(AuctionItem auctionItem) {
        AuctionItem removed = this.items.remove(auctionItem.getID());
        if (removed == null)
            return;
        if (this.config.getConfigurationSection("Items") != null)
            this.config.getConfigurationSection("Items").set(auctionItem.getID().toString(), null);
        saveConfig();
    }

    public void saveConfig() {
        GalaxyAPI.getConfigAPI().saveConfig((JavaPlugin) this.market, "auction.yml");
    }

    public Map<UUID, AuctionItem> getAllItems() {
        return this.items;
    }

    public boolean checkLimit(Player player) {
        long owned = this.items.values().stream()
                .filter(auctionItem -> auctionItem.getOwner().equalsIgnoreCase(player.getName()) && !auctionItem.isExpired())
                .count();
        return getLimit(player) >= owned;
    }

    public int getLimit(Player player) {
        String groupName = PERMISSIONS_API.getPlayerGroup(player);
        return this.limitItemsToSell.getOrDefault(groupName, 10);
    }

    public void openMainGui(Player player) {
        this.auctionMainGui.open(player);
    }

    public void openTypeMainGui(Player player) {
        this.auctionTypeMainGui.open(player);
    }

    public void openTypeGui(Player player, AuctionItemType type) {
        this.typeGuis.get(type).open(player);
    }

    public void removePlayerGuis(Player player) {
        this.playersGui.keySet().removeIf(name -> name.startsWith(player.getName().toLowerCase()));
    }

    public AuctionPlayerGui getPlayerGui(Player player, String who) {
        AuctionPlayerGui gui = this.playersGui.get(player.getName().toLowerCase() + who.toLowerCase());
        if (gui == null) {
            gui = new AuctionPlayerGui(this, who);
            this.playersGui.put(player.getName().toLowerCase() + who.toLowerCase(), gui);
        }
        return gui;
    }

    public AuctionMyGui getOrCreateMyGui(Player player) {
        AuctionMyGui gui = this.ownerGuis.get(player.getName().toLowerCase());
        if (gui == null) {
            gui = new AuctionMyGui(this, player);
            this.ownerGuis.put(player.getName().toLowerCase(), gui);
        }
        return gui;
    }
}

