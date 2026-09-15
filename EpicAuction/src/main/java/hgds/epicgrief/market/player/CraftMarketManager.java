package hgds.epicgrief.market.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import hgds.epicgrief.market.api.MarketPlayer;
import hgds.epicgrief.market.api.MarketPlayerManager;

public class CraftMarketManager implements MarketPlayerManager {
    private final Map<String, MarketPlayer> marketPlayers = new ConcurrentHashMap<>();

    public MarketPlayer getMarketPlayer(String name) {
        return this.marketPlayers.get(name);
    }

    public MarketPlayer getMarketPlayer(Player player) {
        MarketPlayer marketPlayer = getMarketPlayer(player.getName());
        if (marketPlayer == null)
            throw new NullPointerException("" + player.getName());
        return marketPlayer;
    }

    public MarketPlayer getOrCreate(String name) {
        MarketPlayer marketPlayer = getMarketPlayer(name);
        if (marketPlayer == null)
            return new CraftMarketPlayer(name);
        return marketPlayer;
    }

    public MarketPlayer getOrCreate(Player player) {
        return getOrCreate(player.getName());
    }

    public void addMarketPlayer(MarketPlayer marketPlayer) {
        this.marketPlayers.put(marketPlayer.getName(), marketPlayer);
    }

    public void removeMarketPlayer(MarketPlayer marketPlayer) {
        String name = marketPlayer.getName();
        removeMarketPlayer(name);
    }

    public void removeMarketPlayer(String name) {
        this.marketPlayers.remove(name);
    }

    public Map<String, MarketPlayer> getMarketPlayers() {
        return this.marketPlayers;
    }

    public boolean contains(String name) {
        return this.marketPlayers.containsKey(name);
    }
}
