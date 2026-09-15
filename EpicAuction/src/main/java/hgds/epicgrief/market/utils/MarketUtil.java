package hgds.epicgrief.market.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.builders.item.ItemBuilder;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.MultiInventory;
import hgds.epicgrief.api.utils.StringUtil;
import hgds.epicgrief.api.utils.TimeLeftFormat;
import hgds.epicgrief.api.utils.TimeUtil;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.api.MarketAPI;
import hgds.epicgrief.market.api.MarketPlayer;
import hgds.epicgrief.market.api.MarketPlayerManager;
import hgds.epicgrief.market.auction.AuctionAcceptGui;
import hgds.epicgrief.market.auction.AuctionItemType;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.auction.gui.AuctionPlayerGui;
import hgds.epicgrief.market.messaging.Lang;

public final class MarketUtil {
    private MarketUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final MarketPlayerManager MARKET_PLAYER_MANAGER = MarketAPI.getMarketPlayerManager();

    private static final InventoryAPI INVENTORY_API = GalaxyAPI.getInventoryAPI();

    public static int getItemsAmount(Player player, ItemStack itemStack) {
        PlayerInventory inventory = player.getInventory();
        if (!inventory.contains(itemStack.getType()))
            return 0;
        int amount = 0;
        for (ItemStack content : inventory.getContents()) {
            if (content != null) {
                if (content.getType() == itemStack.getType()) {
                    ItemMeta meta = content.getItemMeta();
                    if (meta == null || !meta.hasDisplayName())
                        amount += content.getAmount();
                }
            }
        }
        return amount;
    }

    public static int setItems(AuctionManager manager, MultiInventory inventory, int amountOld, Collection<AuctionItem> auctionItems, AuctionItemType type, boolean button) {
        List<AuctionItem> collect = auctionItems.stream()
                .sorted(Comparator.comparingLong(AuctionItem::getTime))
                .collect(Collectors.toList());
        if (amountOld != collect.size())
            inventory.clearInventories();
        int slot = 10;
        int page = 0;
        for (AuctionItem auctionItem : collect) {
            if (auctionItem.isExpired() || !manager.getAllItems().containsKey(auctionItem.getID()))
                continue;
            ItemStack itemStack = ItemBuilder.newBuilder(auctionItem.getItem())
                    .addLore(Lang.getList("AUCTION_LORE_ITEM", auctionItem.getOwner(),
                            StringUtil.getNumberFormat(auctionItem.getPrice()),
                            TimeUtil.getTimeLeft((auctionItem.getTime() - System.currentTimeMillis()) / 1000L, TimeLeftFormat.DAYS)))
                    .create();
            int finalPage = page;
            inventory.setItem(page, slot++, new GItem(itemStack, (player, clickType, i) -> {
                MarketPlayer marketPlayer = MARKET_PLAYER_MANAGER.getMarketPlayer(player);
                if (marketPlayer == null)
                    return;
                if (clickType.isRightClick()) {
                    AuctionPlayerGui playerGui = manager.getPlayerGui(player, auctionItem.getOwner());
                    playerGui.open(player);
                    return;
                }
                if (auctionItem.getOwner().equalsIgnoreCase(player.getName())) {
                    Lang.sendMessage(player, "AUCTION_BUY_YOU_ERROR");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F);
                    return;
                }
                if (!manager.getAllItems().containsKey(auctionItem.getID()) || !auctionItem.isAvailable()) {
                    Lang.sendMessage(player, "AUCTION_ITEM_ALLREADY_SELLED");
                    return;
                }
                if (!marketPlayer.hasMoney(auctionItem.getPrice())) {
                    Lang.sendMessage(player, "AUCTION_NO_MONEY");
                    return;
                }
                if (isFull(player.getInventory())) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F);
                    Lang.sendMessage(player, "INVENTORY_IS_FULL");
                    player.closeInventory();
                    return;
                }
                AuctionAcceptGui acceptGui = new AuctionAcceptGui(player, auctionItem);
                acceptGui.open(() -> auctionItem.buy(player), () -> player.closeInventory());
            }));
            if ((slot - 8) % 9 == 0)
                slot += 2;
            if (slot >= 44) {
                slot = 10;
                page++;
            }
        }
        if (button)
            setTypeItem(manager, inventory, type);
        setTypeMyGuiItem(manager, inventory);
        if (slot == 10 && page == 0) {
            inventory.setItem(0, 22, new GItem(ItemBuilder.newBuilder(Material.BARRIER)
                    .setName(Lang.getMessage("AUCTION_NO_ITEMS_NAME"))
                    .setLore(Lang.getList("AUCTION_NO_ITEMS_LORE"))
                    .create(), (player, clickType, i) -> player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F)));
            return 0;
        }
        INVENTORY_API.pageButton(page + 1, inventory, 47, 51);
        return collect.size();
    }

    private static void setTypeMyGuiItem(AuctionManager manager, MultiInventory inventory) {
        inventory.getInventories().forEach(gInventory -> gInventory.setItem(49, new GItem(ItemBuilder.newBuilder(Material.RED_BED)
                .setName(Lang.getMessage("AUCTION_MYGUI_ITEM_NAME"))
                .setLore(Lang.getList("AUCTION_MYGUI_ITEM_LORE"))
                .create(), (player, clickType, i) -> {
            if (!clickType.isLeftClick())
                return;
            manager.getOrCreateMyGui(player).open();
        })));
        inventory.getInventories().forEach(gInventory -> gInventory.setItem(45, new GItem(ItemBuilder.newBuilder(Material.KNOWLEDGE_BOOK)
                .setName(Lang.getMessage("AUCTION_HELP_ITEM_NAME"))
                .setLore(Lang.getList("AUCTION_HELP_ITEM_LORE"))
                .create())));
    }

    private static void setTypeItem(AuctionManager manager, MultiInventory inventory, AuctionItemType type) {
        inventory.getInventories().forEach(gInventory -> gInventory.setItem(53, new GItem(ItemBuilder.newBuilder(type.getItem())
                .setName(Lang.getMessage("AUCTION_CATEGORY_NAME"))
                .setLore(Lang.getList("AUCTION_CATEGORY_LORE", type.getName()))
                .create(), (player, clickType, i) -> manager.openTypeMainGui(player))));
    }

    public static boolean contains(Player player, ItemStack itemStack) {
        return (getItemsAmount(player, itemStack) <= 0);
    }

    public static void removeItems(Player player, ItemStack itemStack, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack content : inventory.getContents()) {
            if (amount <= 0)
                break;
            if (content != null && content.getType() != Material.AIR) {
                if (content.isSimilar(itemStack)) {
                    ItemStack removeItem = content.clone();
                    if (removeItem.getAmount() >= amount)
                        removeItem.setAmount(amount);
                    inventory.removeItem(removeItem);
                    amount -= removeItem.getAmount();
                }
            }
        }
    }

    public static boolean isFull(PlayerInventory playerInventory) {
        int amount = 36;
        for (ItemStack itemStack : playerInventory.getStorageContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR)
                amount--;
        }
        return (amount == 0);
    }

    public static void setBack(AuctionManager manager, MultiInventory inventory) {
        inventory.getInventories().forEach(inv -> inv.setItem(49, new GItem(ItemBuilder.newBuilder(Material.SPECTRAL_ARROW)
                .setName(Lang.getMessage("PROFILE_BACK_ITEM_NAME"))
                .setLore(Lang.getList("PROFILE_BACK_ITEM_LORE2"))
                .create(), (player, clickType, i) -> manager.openMainGui(player))));
    }
}

