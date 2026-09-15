package hgds.epicgrief.market.api;

import java.util.Map;
import org.bukkit.entity.Player;

public interface MarketPlayerManager {
    MarketPlayer getMarketPlayer(String paramString);

    MarketPlayer getMarketPlayer(Player paramPlayer);

    MarketPlayer getOrCreate(String paramString);

    MarketPlayer getOrCreate(Player paramPlayer);

    void addMarketPlayer(MarketPlayer paramMarketPlayer);

    void removeMarketPlayer(MarketPlayer paramMarketPlayer);

    void removeMarketPlayer(String paramString);

    Map<String, MarketPlayer> getMarketPlayers();

    boolean contains(String paramString);
}
