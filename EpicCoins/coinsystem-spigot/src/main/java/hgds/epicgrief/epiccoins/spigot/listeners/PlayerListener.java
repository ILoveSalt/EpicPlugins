package hgds.epicgrief.epiccoins.spigot.listeners;

import hgds.epicgrief.epiccoins.core.CoinSystem;
import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.epiccoins.core.manager.PermissionManager;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;
import hgds.epicgrief.EpicCoins;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener{

	@EventHandler
	public void onJoin(final PlayerJoinEvent event){
		Bukkit.getScheduler().runTaskAsynchronously(EpicCoins.getInstance(), ()->{
			if(!CoinSystem.getInstance().getStorage().isConnected()){
				if(event.getPlayer().hasPermission(PermissionManager.getInstance().admin)){
					event.getPlayer().sendMessage(MessageManager.getInstance().mysql_noconnection);
				}
				return;
			}
			if(CoinSystem.getInstance().getConfig().system_player_onlyproxy_check) return;
			CoinPlayer player;
			try {
				player = CoinSystem.getInstance().getPlayerManager().getPlayerSave(event.getPlayer().getUniqueId());
			}catch (Exception exception){
				exception.printStackTrace();
				event.getPlayer().sendMessage(MessageManager.getInstance().prefix + "§cError while loading your coins profile.");
				return;
			}
			if(player == null) CoinSystem.getInstance().getPlayerManager().createPlayer(event.getPlayer().getName(),event.getPlayer().getUniqueId());
			else player.updateInfos(event.getPlayer().getName(),CoinSystem.getInstance().getPlatform().getColor(player)
					,System.currentTimeMillis());
		});
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		if(CoinSystem.getInstance().getConfig().system_player_onlyproxy_check) return;
		Bukkit.getScheduler().runTaskAsynchronously(EpicCoins.getInstance(),()->{
			CoinPlayer player = CoinSystem.getInstance().getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
			if(player != null) player.updateInfos(event.getPlayer().getName(),CoinSystem.getInstance().getPlatform().getColor(player)
					,System.currentTimeMillis());
		});
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e){
		Bukkit.getScheduler().runTaskAsynchronously(EpicCoins.getInstance(),()->{
			if(CoinSystem.getInstance().getConfig().system_player_addcoinsonkill && e.getEntity().getKiller() != null){
				CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(e.getEntity().getKiller().getUniqueId());
				if(coinplayer != null) coinplayer.addCoins(CoinSystem.getInstance().getConfig().system_player_addcoinsonkill_amount,"kill");
			}
			if(CoinSystem.getInstance().getConfig().system_player_removecoinsondeath){
				CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(e.getEntity().getUniqueId());
				if(coinplayer != null){
					if(coinplayer.getCoins() == 0) return;
					coinplayer.removeCoins(Math.min(coinplayer.getCoins(), CoinSystem.getInstance().getConfig().system_player_removecoinsondeath_amount), "death");
				}
			}
		});
	}
}
