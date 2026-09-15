package hgds.epicgrief.market.api;

import org.bukkit.entity.Player;

public interface MarketPlayer {
    Player getPlayer();

    String getName();

    int getMoney();

    boolean changeMoney(int paramInt);

    boolean hasMoney(int paramInt);
}