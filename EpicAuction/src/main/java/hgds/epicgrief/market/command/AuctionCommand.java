package hgds.epicgrief.market.command;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.commands.CommandSource;
import hgds.epicgrief.api.commands.depend.CommandIssuer;
import hgds.epicgrief.api.utils.BukkitUtil;
import hgds.epicgrief.api.utils.StringUtil;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.auction.AuctionItemImpl;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.auction.gui.AuctionMyGui;
import hgds.epicgrief.market.auction.gui.AuctionPlayerGui;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.utils.MarketUtil;

public final class AuctionCommand implements CommandIssuer {
    private final List<Material> materialList = Arrays.asList(
            Material.SHULKER_SHELL, Material.BLACK_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.RED_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.ENCHANTED_BOOK, Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION);

    private final AuctionManager manager;

    private final FileConfiguration config;

    public AuctionCommand(AuctionManager manager, FileConfiguration config) {
        this.manager = manager;
        this.config = config;
        CommandSource spigotCommand = GalaxyAPI.getCommandsAPI().register("auctionhouse", this, "auction", "auctions", "ah", "auc");
        spigotCommand.setOnlyPlayers(true);
    }

    @Override
    public void execute(CommandSender gamerEntity, String s, String[] args) {
        Player player = (Player) gamerEntity;
        if (args.length < 1) {
            this.manager.openMainGui(player);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "show": {
                if (args.length < 2) {
                    AuctionMyGui auctionMyGui = this.manager.getOrCreateMyGui(player);
                    auctionMyGui.open();
                    return;
                }
                String who = args[1];
                BukkitUtil.runTaskAsync(() -> {
                    int count = (int) this.manager.getAllItems().values().stream()
                            .filter(item -> item.getOwner().equalsIgnoreCase(who))
                            .filter(AuctionItem::isAvailable)
                            .count();
                    if (count < 1) {
                        Lang.sendMessage(player, "AUCTION_SHOW_ERROR", who);
                        return;
                    }
                    if (!player.isOnline())
                        return;
                    AuctionPlayerGui gui = this.manager.getPlayerGui(player, who);
                    BukkitUtil.runTask(() -> gui.open(player));
                });
                return;
            }
            case "buy":
            case "sell":
                executeSell(player, gamerEntity, args);
                return;
            default:
                Lang.sendMessage(gamerEntity, "AUCTION_COMMAND_HELP");
        }
    }

    private void executeSell(Player player, CommandSender gamerEntity, String[] args) {
        if (args.length < 2) {
            Lang.sendMessage(gamerEntity, "AUCTION_FORMAT_NO_ARGS");
            return;
        }
        if (!this.manager.checkLimit(player)) {
            Lang.sendMessage(player, "AUCTION_LIMIT_ERROR", this.manager.getLimit(player));
            return;
        }
        ItemStack itemHand = player.getInventory().getItemInMainHand();
        if (itemHand.getType() == Material.AIR) {
            Lang.sendMessage(player, "AUCTION_SELL_AIR");
            return;
        }
        Integer price = parseInt(args[1]);
        if (price == null) {
            Lang.sendMessage(player, "AUCTION_COMMAND_ERROR");
            return;
        }
        if (price < 1) {
            Lang.sendMessage(player, "AUCTION_COMMAND_ERROR1");
            return;
        }
        ItemStack itemStack = itemHand.clone();
        int amount = itemStack.getAmount();
        if (args.length > 2) {
            Integer parsedAmount = parseInt(args[2]);
            if (parsedAmount == null) {
                Lang.sendMessage(player, "AUCTION_COMMAND_ERROR2");
                return;
            }
            if (itemHand.getAmount() < parsedAmount) {
                Lang.sendMessage(player, "AUCTION_ADD_TO_SELL_ERROR");
                return;
            }
            if (parsedAmount < 1) {
                Lang.sendMessage(player, "AUCTION_COMMAND_ERROR1");
                return;
            }
            amount = parsedAmount;
            itemStack.setAmount(amount);
        }
        PlayerInventory playerInventory = player.getInventory();
        if (playerInventory.getItemInOffHand().getType() == itemStack.getType()
                || (playerInventory.getHelmet() != null && playerInventory.getHelmet().getType() == itemStack.getType())) {
            Lang.sendMessage(player, "AUCTION_COMMAND_ERROR3");
            return;
        }
        AuctionItemImpl auctionItemImpl = new AuctionItemImpl(this.manager, UUID.randomUUID(), player.getName(),
                itemStack, price, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3L));
        this.manager.add(auctionItemImpl);
        Lang.sendMessage(player, "AUCTION_ADD_TO_SELL", itemStack.getType().toString(),
                StringUtil.getNumberFormat(price));
        if (this.materialList.contains(itemHand.getType())) {
            removeItem(player, itemHand);
            return;
        }
        MarketUtil.removeItems(player, itemStack, amount);
    }

    private void removeItem(Player player, ItemStack removed) {
        ItemStack[] items = player.getInventory().getStorageContents();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].isSimilar(removed)) {
                player.getInventory().clear(i);
                break;
            }
        }
    }

    private Integer parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return null;
        }
    }
}

