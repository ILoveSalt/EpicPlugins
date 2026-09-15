package hgds.epicgrief.epiccoins.spigot.commands;

import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.EpicCoins;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EpicCoinsCommand implements CommandExecutor{

	public boolean onCommand(CommandSender sender, Command cmd, String label,String[] arg3) {
		sender.sendMessage(MessageManager.getInstance().prefix+"§7EpicCoins §ev"+ EpicCoins.getInstance().getDescription().getVersion()+" §7by §ehgds_2");
		return true;
	}
}
