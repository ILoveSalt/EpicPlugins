package hgds.epicgrief.market.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import hgds.epicgrief.market.Market;
import hgds.epicgrief.market.api.MarketAPI;
import hgds.epicgrief.market.api.MarketPlayerManager;

public class PlayerListener implements Listener {
    private final MarketPlayerManager marketPlayerManager = MarketAPI.getMarketPlayerManager();

    private final Market market;

    public PlayerListener(Market market) {
        this.market = market;
        Bukkit.getPluginManager().registerEvents(this, (Plugin)market);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;
        this.marketPlayerManager.addMarketPlayer(new CraftMarketPlayer(e.getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        String name = player.getName().toLowerCase();
        this.marketPlayerManager.removeMarketPlayer(name);
        this.market.getAuctionManager().removePlayerGuis(player);
        this.market.getAuctionManager().getOwnerGuis().remove(name);
    }
}
