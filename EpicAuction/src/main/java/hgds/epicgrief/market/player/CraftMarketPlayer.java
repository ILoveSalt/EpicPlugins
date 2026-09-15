package hgds.epicgrief.market.player;

import org.bukkit.entity.Player;
import hgds.epicgrief.api.utils.EconomyUtil;
import hgds.epicgrief.market.api.MarketPlayer;

public class CraftMarketPlayer implements MarketPlayer {
    private final String name;

    public CraftMarketPlayer(String name) {
        if (name == null)
            throw new NullPointerException("name is marked non-null but is null");
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean hasMoney(int money) {
        return (EconomyUtil.getBalance(this.name) >= money);
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public boolean changeMoney(int money) {
        if (getMoney() + money < 0)
            return false;
        if (money < 0) {
            EconomyUtil.withdraw(this.name, Math.abs(money));
        } else {
            EconomyUtil.deposit(this.name, money);
        }
        return true;
    }

    @Override
    public int getMoney() {
        return EconomyUtil.getBalance(this.name);
    }
}
