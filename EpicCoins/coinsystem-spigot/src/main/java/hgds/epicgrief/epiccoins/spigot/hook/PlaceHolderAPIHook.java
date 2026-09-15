package hgds.epicgrief.epiccoins.spigot.hook;

import hgds.epicgrief.epiccoins.core.CoinSystem;
import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;
import hgds.epicgrief.epiccoins.core.utils.GeneralUtil;
import hgds.epicgrief.EpicCoins;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceHolderAPIHook extends PlaceholderExpansion {

	@Override
	public String getIdentifier() {
		return "epiccoins";
	}

	@Override
	public String getAuthor() {
		return "hgds_2";
	}

	@Override
	public String getVersion() {
		return CoinSystem.getInstance().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		if(player == null|| identifier == null) return "";
		if(identifier.equalsIgnoreCase("coins") || identifier.equalsIgnoreCase("coin") || identifier.equalsIgnoreCase("money")){
			CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());
			if(coinplayer != null) return EpicCoins.getInstance().format(coinplayer.getCoins());
		}else if(identifier.equalsIgnoreCase("firstlogin")){
			CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());
			if(coinplayer != null) return CoinSystem.getInstance().getConfig().dateFormat.format(coinplayer.getFirstLogin());
		}else if(identifier.equalsIgnoreCase("lastlogin")){
			CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());
			if(coinplayer != null) return CoinSystem.getInstance().getConfig().dateFormat.format(coinplayer.getLastLogin());
		}else if(identifier.equalsIgnoreCase("id") || identifier.equalsIgnoreCase("playerid")){
			CoinPlayer coinplayer = CoinSystem.getInstance().getPlayerManager().getPlayer(player.getUniqueId());
			if(coinplayer != null) return String.valueOf(coinplayer.getID());
		}else if(identifier.equalsIgnoreCase("top_amount")){
			return getTopAmount(1);
		}else if(identifier.startsWith("top_amount_")){
			String index0 = identifier.substring(identifier.lastIndexOf("_")+1);
			if(GeneralUtil.isNumber(index0)){
				return getTopAmount(Integer.parseInt(index0));
			}else return "";
		}else if(identifier.equalsIgnoreCase("top")){
			return getTopName(1);
		}else if(identifier.startsWith("top_")){
			String index0 = identifier.substring(identifier.lastIndexOf("_")+1);
			if(GeneralUtil.isNumber(index0)){
				return getTopName(Integer.parseInt(index0));
			}else return "";
		}
		return null;
	}

	private String getTopAmount(int index) {
		CoinPlayer player = getTopPlayer(index);
		return player == null ? "" : EpicCoins.getInstance().format(player.getCoins());
	}

	private String getTopName(int index) {
		CoinPlayer player = getTopPlayer(index);
		return player == null ? "" : player.getName();
	}

	private CoinPlayer getTopPlayer(int index) {
		if(index < 1) return null;
		List<CoinPlayer> players = CoinSystem.getInstance().getPlayerManager().getTopCoins(index);
		return players.size() < index ? null : players.get(index - 1);
	}
}
