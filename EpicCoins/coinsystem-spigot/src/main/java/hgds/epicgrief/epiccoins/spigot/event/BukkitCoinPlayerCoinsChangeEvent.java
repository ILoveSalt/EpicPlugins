package hgds.epicgrief.epiccoins.spigot.event;

import hgds.epicgrief.epiccoins.core.event.CoinsUpdateCause;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;
import hgds.epicgrief.EpicCoins;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitCoinPlayerCoinsChangeEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();

	private final CoinPlayer coinplayer;
	private String message;
	private long oldCoins;
	private long newCoins;
	private CoinsUpdateCause cause;
	private boolean cancelled;
	
	public BukkitCoinPlayerCoinsChangeEvent(CoinPlayer coinplayer, long oldCoins, long newCoins, CoinsUpdateCause cause, String message){
	    super(Thread.currentThread() != EpicCoins.MAIN_SERVER_THREAD);
		this.coinplayer = coinplayer;
		this.oldCoins = oldCoins;
		this.newCoins = newCoins;
		this.cause = cause;
		this.message = message;
        cancelled = false;
	}
	@Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public CoinPlayer getCoinPlayer(){
    	return this.coinplayer;
    }

    public String getMessage() {
        return message;
    }

    public long getOldCoins(){
    	return this.oldCoins;
    }

    public long getNewCoins(){
        return this.newCoins;
    }

    public CoinsUpdateCause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled(){
	    return cancelled;
    }

    @Override
    public void setCancelled(boolean value){
	    this.cancelled = value;
    }

    public void setNewCoins(long coins){
	    this.newCoins = coins;
    }
}
