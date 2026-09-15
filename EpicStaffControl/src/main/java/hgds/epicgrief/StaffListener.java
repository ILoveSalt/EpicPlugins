package hgds.epicgrief;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class StaffListener implements Listener {
    private final StaffService service;

    public StaffListener(StaffService service) {
        this.service = service;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.stopWork(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPunishmentCommand(PlayerCommandPreprocessEvent event) {
        service.recordPunishment(event.getPlayer(), event.getMessage());
    }
}
