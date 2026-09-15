package hgds.epicgrief.market.api;

import hgds.epicgrief.market.player.CraftMarketManager;

public final class MarketAPI {
    private static MarketPlayerManager marketPlayerManager;

    public static MarketPlayerManager getMarketPlayerManager() {
        if (marketPlayerManager == null)
            marketPlayerManager = (MarketPlayerManager)new CraftMarketManager();
        return marketPlayerManager;
    }
}