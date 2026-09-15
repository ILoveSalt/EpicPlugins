package hgds.epicgrief;

import com.google.common.collect.ImmutableMap;
import hgds.epicgrief.util.command.ExecutableCommand;
import hgds.epicgrief.util.message.Message;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class CheckManager {
    private final CheckConfig config;

    private final CheckConfig.PunishCommands commands;

    private final ConcurrentHashMap<Player, Player> checkedPlayerModeratorMap = new ConcurrentHashMap<>();

    private final BukkitTask notifierTask;

    public CheckManager(Main plugin) {
        this.config = plugin.getPluginConfig();
        this.commands = this.config.getCommands();
        long periodTicks = this.config.getMessages().getCheckNotificationPeriod();
        this.notifierTask = new CheckNotifier().runTaskTimerAsynchronously((Plugin) plugin, 0L, periodTicks);
    }

    /**
     * Останавливает периодические уведомления (вызывается при отключении плагина).
     */
    public void dispose() {
        if (this.notifierTask != null) {
            this.notifierTask.cancel();
        }
    }

    private static void notifyOnlinePlayers(Message message, Map<String, String> args, Player reliableReceiver) {
        Message formattedMessage = message.format(args);
        formattedMessage.send(reliableReceiver);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != reliableReceiver && onlinePlayer.hasPermission("check.notify")) {
                formattedMessage.send(onlinePlayer);
            }
        }
    }

    public void checkPlayer(Player playerForCheck, Player moderator) {
        this.config.getMessages().getCheckNotificationStart().send(playerForCheck,
                ImmutableMap.of("moderator", moderator.getName()));
        if (this.config.getRestrictions().shouldRestrictInventoryInteraction()) {
            playerForCheck.closeInventory();
        }
        this.checkedPlayerModeratorMap.put(playerForCheck, moderator);
    }

    public void actToCheckedPlayer(Player checkedPlayer, ModeratorActType action) {
        if (action == ModeratorActType.CANCEL_CHECK) {
            justify(checkedPlayer);
            return;
        }
        ExecutableCommand commandToExecute = null;
        switch (action) {
            case CONFIRM_CHEATS:
                commandToExecute = this.commands.getPunishCheats();
                break;
            case DODGED:
                commandToExecute = this.commands.getPunishDodged();
                break;
            case CHEAT_PROGRAM_FILES_FOUND:
                commandToExecute = this.commands.getPunishCheatFilesFound();
                break;
            case IGNORED:
                commandToExecute = this.commands.getPunishIgnored();
                break;
            default:
                break;
        }
        punish(checkedPlayer, commandToExecute);
    }

    public void punishQuit(Player checkedPlayer) {
        punish(checkedPlayer, this.commands.getPunishQuit());
    }

    public void punishFair(Player checkedPlayer) {
        punish(checkedPlayer, this.commands.getPunishFair());
    }

    private void punish(Player checkedPlayer, ExecutableCommand command) {
        Player moderator = this.checkedPlayerModeratorMap.get(checkedPlayer);
        boolean runAsConsole = this.commands.getExecutor() == CheckConfig.PunishCommands.Executor.CONSOLE;
        CommandSender executor = runAsConsole ? Bukkit.getConsoleSender() : moderator;
        Map<String, String> args = ImmutableMap.of(
                "player", checkedPlayer.getName(),
                "moderator", moderator.getName());
        command.execute(executor, args);
        this.checkedPlayerModeratorMap.remove(checkedPlayer);
        notifyPlayerFailedCheck(checkedPlayer, moderator);
    }

    private void justify(Player checkedPlayer) {
        Player moderator = this.checkedPlayerModeratorMap.get(checkedPlayer);
        this.checkedPlayerModeratorMap.remove(checkedPlayer);
        notifyPlayerPassedCheck(checkedPlayer, moderator);
    }

    private void notifyPlayerPassedCheck(Player who, Player moderator) {
        CheckConfig.Messages messages = this.config.getMessages();
        messages.getYouPassedCheck().send(who, ImmutableMap.of("moderator", moderator.getName()));
        notifyOnlinePlayers(messages.getPlayerPassedCheck(),
                ImmutableMap.of("player", who.getName(), "moderator", moderator.getName()), moderator);
    }

    private void notifyPlayerFailedCheck(Player who, Player moderator) {
        notifyOnlinePlayers(this.config.getMessages().getPlayerFailedCheck(),
                ImmutableMap.of("player", who.getName(), "moderator", moderator.getName()), moderator);
    }

    public boolean isBeingChecked(Player player) {
        return this.checkedPlayerModeratorMap.containsKey(player);
    }

    public Player getCheckModerator(Player checkedPlayer) {
        return this.checkedPlayerModeratorMap.get(checkedPlayer);
    }

    private class CheckNotifier extends BukkitRunnable {
        private CheckNotifier() {
        }

        @Override
        public void run() {
            for (Map.Entry<Player, Player> entry : CheckManager.this.checkedPlayerModeratorMap.entrySet()) {
                Player checkedPlayer = entry.getKey();
                Player moderator = entry.getValue();
                CheckManager.this.config.getMessages().getCheckNotificationRepeat()
                        .send(checkedPlayer, ImmutableMap.of("moderator", moderator.getName()));
            }
        }
    }
}