package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class ChatListener implements Listener {
    private final EpicChat plugin;

    public ChatListener(EpicChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();

        Runnable handler = () -> plugin.handlePublicChat(player, message);
        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(plugin, handler);
        } else {
            handler.run();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() <= 1) {
            return;
        }

        String label = message.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (plugin.isBlockedByPlayTimeCommand(label) && !plugin.isPlayTimeReady(event.getPlayer())) {
            event.setCancelled(true);
            plugin.sendPlayTimeBlocked(event.getPlayer());
        }
    }
}
