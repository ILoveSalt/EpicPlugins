package hgds.epicgrief;

import hgds.epicgrief.epiccoins.core.CoinSystem;
import hgds.epicgrief.epiccoins.core.EpicCoinsPlatform;
import hgds.epicgrief.epiccoins.core.event.CoinChangeEventResult;
import hgds.epicgrief.epiccoins.core.event.CoinsUpdateCause;
import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;
import hgds.epicgrief.epiccoins.core.player.PlayerColor;
import hgds.epicgrief.epiccoins.spigot.commands.CoinsCommand;
import hgds.epicgrief.epiccoins.spigot.commands.EpicCoinsCommand;
import hgds.epicgrief.epiccoins.spigot.commands.PayCommand;
import hgds.epicgrief.epiccoins.spigot.event.BukkitCoinPlayerCoinsChangeEvent;
import hgds.epicgrief.epiccoins.spigot.event.BukkitCoinPlayerColorSetEvent;
import hgds.epicgrief.epiccoins.spigot.hook.PlaceHolderAPIHook;
import hgds.epicgrief.epiccoins.spigot.hook.VaultHook;
import hgds.epicgrief.epiccoins.spigot.listeners.PlayerListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EpicCoins extends JavaPlugin implements EpicCoinsPlatform {

	public static Thread MAIN_SERVER_THREAD;
	private static EpicCoins INSTANCE;

	@Override
	public void onLoad() {
		MAIN_SERVER_THREAD = Thread.currentThread();
		INSTANCE = this;

		new CoinSystem(this);
	}

	@Override
	public void onEnable() {
		INSTANCE = this;

		Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

		if(getCommand("epiccoins") != null) getCommand("epiccoins").setExecutor(new EpicCoinsCommand());
		registerCommand(new CoinsCommand());
		if(CoinSystem.getInstance().getConfig().command_pay_enabled) registerCommand(new PayCommand());
		hook();
	}

	@Override
	public void onDisable() {
		if(CoinSystem.getInstance() != null) CoinSystem.getInstance().shutdown();
	}

	@Override
	public String getPlatformName() {
		return "Paper";
	}

	@Override
	public String getServerVersion() {
		return Bukkit.getVersion();
	}

	@Override
	public File getFolder() {
		return getDataFolder();
	}

	@Override
	public String getColor(CoinPlayer player) {
		Player bukkitPlayer =  Bukkit.getPlayer(player.getUUID());
		if(bukkitPlayer == null) return null;
		String color = CoinSystem.getInstance().getConfig().defaultColor;
		for(PlayerColor colors : CoinSystem.getInstance().getConfig().playerColors){
			if(bukkitPlayer.hasPermission(colors.getPermission())){
				color = colors.getColor();
				break;
			}
		}
		BukkitCoinPlayerColorSetEvent event = new BukkitCoinPlayerColorSetEvent(color,player,bukkitPlayer);
		Bukkit.getPluginManager().callEvent(event);
		if(event.getColor() != null) color = event.getColor();
		return color;
	}

	@Override
	public CoinChangeEventResult executeCoinChangeEvent(CoinPlayer player, long oldCoins, long newCoins, CoinsUpdateCause cause, String message) {
		BukkitCoinPlayerCoinsChangeEvent event = new BukkitCoinPlayerCoinsChangeEvent(player,oldCoins,newCoins,cause,message);
		Bukkit.getPluginManager().callEvent(event);
		return new CoinChangeEventResult(event.isCancelled(),event.getNewCoins());
	}

	private void registerCommand(Command command){
		Bukkit.getCommandMap().register(getName().toLowerCase(), command);
	}

	private void hook(){
		if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			System.out.println("["+MessageManager.getInstance().system_name+"] PlaceHolderAPI found");
			new PlaceHolderAPIHook().register();
		}
		if(CoinSystem.getInstance().getConfig().hook_vault_enabled && Bukkit.getPluginManager().isPluginEnabled("Vault")){
			ServicePriority priority = ServicePriority.Highest;
			try{
				priority = ServicePriority.valueOf(CoinSystem.getInstance().getConfig().hook_vault_priority);
			}catch (Exception ignored){}
			System.out.println("["+MessageManager.getInstance().system_name+"] Vault found (priority="+priority.toString()+")");
			Bukkit.getServer().getServicesManager().register(Economy.class, new VaultHook(), this,priority);
		}
	}

	public String format(long coins){
		if(!CoinSystem.getInstance().getConfig().number_formatting_enabled) return ""+coins;
		String number = String.valueOf(coins);
		if(number.length() > 3){
			int charposition = 0;
			char[] chars = number.toCharArray();
			StringBuilder formatted = new StringBuilder();
			for(int i = chars.length-1;i > -1;i--){
				formatted.append(chars[i]);
				if(charposition == 2){
					charposition = 0;
					formatted.append(CoinSystem.getInstance().getConfig().number_formatting_symbol);
				}else charposition++;
			}
			String finish = formatted.reverse().toString();
			if(finish.startsWith(CoinSystem.getInstance().getConfig().number_formatting_symbol)) finish = finish.substring(1);
			return finish;
		}
		return ""+coins;
	}

	public static EpicCoins getInstance(){
		return INSTANCE;
	}
}
