package hgds.epicgrief.market.auction.gui;

import java.util.List;
import java.util.stream.Collectors;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.auction.AuctionItemType;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.utils.MarketUtil;

public class AuctionPlayerGui extends AuctionAbstractGui {
    private final String gamerWho;

    private int amount;

    public AuctionPlayerGui(AuctionManager manager, String gamerWho) {
        super(manager, Lang.getMessage("AUCTION_PLAYERGUI_NAME", new Object[] { gamerWho }));
        this.gamerWho = gamerWho;
        setItems();
    }

    protected void setItems() {
        if (this.gamerWho == null)
            return;
        List<AuctionItem> auctionItemList = (List<AuctionItem>)this.manager.getAllItems().values().stream().filter(auctionItem -> auctionItem.getOwner().equalsIgnoreCase(this.gamerWho)).collect(Collectors.toList());
        this.amount = MarketUtil.setItems(this.manager, this.inventory, this.amount, auctionItemList, AuctionItemType.ALL, false);
        MarketUtil.setBack(this.manager, this.inventory);
    }
}
