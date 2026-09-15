package hgds.epicgrief.market.auction;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.builders.item.ItemBuilder;
import hgds.epicgrief.api.inventory.GItem;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.inventory.type.GInventory;
import hgds.epicgrief.api.utils.StringUtil;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.messaging.Lang;

public class AuctionAcceptGui {
    private static final InventoryAPI API = GalaxyAPI.getInventoryAPI();

    private final Player player;

    private final AuctionItem auctionItem;

    private GInventory inventory;

    public AuctionAcceptGui(Player player, AuctionItem auctionItem) {
        this.player = player;
        this.auctionItem = auctionItem;
        String name = Lang.getMessage("AUCTION_CONFIRMED_GUI");
        this.inventory = API.createInventory(player, name, 5);
    }

    public void open(Runnable yes, Runnable no) {
        if (this.inventory == null || yes == null)
            return;
        this.inventory.setItem(20, new GItem(ItemBuilder.newBuilder(Material.LIME_STAINED_GLASS)
                .setName(Lang.getMessage("CONFIRMED_NAME"))
                .setLore(Lang.getList("AUCTION_CONFIRMED_GUI_LORE", this.auctionItem.getItem().getType().toString(),
                        this.auctionItem.getItem().getAmount(),
                        StringUtil.getNumberFormat(this.auctionItem.getPrice()), this.auctionItem.getOwner()))
                .create(), (player, clickType, slot) -> {
            yes.run();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F);
        }));
        this.inventory.setItem(24, new GItem(ItemBuilder.newBuilder(Material.RED_STAINED_GLASS)
                .setName(Lang.getMessage("CANCEL_NAME"))
                .setLore(Lang.getList("ACCEPT_LORE_NO"))
                .create(), (player, clickType, i) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, 1.0F);
            if (no != null) {
                no.run();
            } else {
                player.closeInventory();
            }
        }));
        this.inventory.openInventory(this.player);
    }
}
