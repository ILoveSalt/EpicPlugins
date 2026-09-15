package hgds.epicgrief.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.EpicItems;
import hgds.epicgrief.api.CustomItem;
import hgds.epicgrief.api.ItemAPI;
import hgds.epicgrief.utils.Utils;

public class CustomItemCommand implements CommandExecutor {
   public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
      Configuration config = Bukkit.getPluginManager().getPlugin("EpicItems").getConfig();
      if (!commandSender.hasPermission("customitems.admin")) {
         commandSender.sendMessage(Utils.color(config.getString("messages.permission")));
         return false;
      } else {
         if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
               for(String string : Utils.color(config.getStringList("messages.help"))) {
                  commandSender.sendMessage(string);
               }

               return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
               ((EpicItems)Bukkit.getPluginManager().getPlugin("EpicItems")).reload();
               commandSender.sendMessage(Utils.color(config.getString("messages.reload")));
               return true;
            }
         } else if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (args.length != 4) {
               this.sendGiveUsage(commandSender, config);
               return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player != null && player.isOnline()) {
               ItemAPI itemAPI = (ItemAPI)Bukkit.getPluginManager().getPlugin("EpicItems");
               CustomItem item = itemAPI.getItem(args[2]);
               if (item == null) {
                  commandSender.sendMessage(Utils.color(config.getString("messages.item-not-find").replace("%item%", args[2])));
                  return false;
               }

               int amount;
               try {
                  amount = Integer.parseInt(args[3]);
               } catch (Exception var11) {
                  commandSender.sendMessage(Utils.color(config.getString("messages.number")));
                  return true;
               }

               ItemStack bukkit = item.getBukkit();
               itemAPI.updatePlaceholder(bukkit);
               bukkit.setAmount(amount);
               player.getInventory().addItem(new ItemStack[]{bukkit});
               commandSender.sendMessage(Utils.color(config.getString("messages.item-give").replace("%player%", player.getName()).replace("%item%", args[2])));
               return true;
            }

            commandSender.sendMessage(Utils.color(config.getString("messages.player-not-find").replace("%player%", args[1])));
            return false;
         }

         commandSender.sendMessage(Utils.color(config.getString("messages.argument")));
         return true;
      }
   }

   private void sendGiveUsage(CommandSender commandSender, Configuration config) {
      String usage = config.getString("messages.give-usage");
      if (usage == null || usage.isBlank()) {
         usage = "§cИспользование: /customitems give <ник> <предмет> <кол-во>";
      }
      commandSender.sendMessage(Utils.color(usage));
   }
}
