package hgds.epicgrief.market.auction.gui;

import java.util.Collection;
import java.util.stream.Collectors;
import hgds.epicgrief.market.api.AuctionItem;
import hgds.epicgrief.market.auction.AuctionItemType;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.utils.MarketUtil;

public class AuctionTypeGui extends AuctionAbstractGui {
    private final AuctionItemType type;

    private int amount;

    public AuctionTypeGui(AuctionManager manager, AuctionItemType type) {
        super(manager, Lang.getMessage("AUCTION_MAINGUI_NAME", new Object[0]));
        this.type = type;
        setItems();
    }

    protected void setItems() {
        if (this.type == null)
            return;
        this.amount = MarketUtil.setItems(this.manager, this.inventory, this.amount, (Collection)this.manager.getAllItems().values()
                .stream()
                .filter(auctionItem -> (auctionItem.getType() == this.type))
                .collect(Collectors.toList()), this.type, true);
        MarketUtil.setBack(this.manager, this.inventory);
    }
}
