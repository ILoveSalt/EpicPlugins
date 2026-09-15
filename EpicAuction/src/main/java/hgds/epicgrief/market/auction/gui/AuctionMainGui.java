package hgds.epicgrief.market.auction.gui;

import hgds.epicgrief.market.auction.AuctionItemType;
import hgds.epicgrief.market.auction.AuctionManager;
import hgds.epicgrief.market.messaging.Lang;
import hgds.epicgrief.market.utils.MarketUtil;

public class AuctionMainGui extends AuctionAbstractGui {
    private int amount;

    public AuctionMainGui(AuctionManager manager) {
        super(manager, Lang.getMessage("AUCTION_MAINGUI_NAME", new Object[0]));
        setItems();
    }

    protected void setItems() {
        this.amount = MarketUtil.setItems(this.manager, this.inventory, this.amount, this.manager
                .getAllItems().values(), AuctionItemType.ALL, true);
    }
}
