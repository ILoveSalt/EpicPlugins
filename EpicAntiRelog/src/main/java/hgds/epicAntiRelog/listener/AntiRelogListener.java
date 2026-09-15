package hgds.epicAntiRelog.listener;

import hgds.epicAntiRelog.combat.CombatService;
import hgds.epicAntiRelog.command.CombatCommandService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.item.ItemService;
import hgds.epicAntiRelog.punishment.PunishmentService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class AntiRelogListener implements Listener {

    private final AntiRelogSettings settings;
    private final CombatService combatService;
    private final ItemService itemService;
    private final CombatCommandService commandService;
    private final PunishmentService punishmentService;

    public AntiRelogListener(
            AntiRelogSettings settings,
            CombatService combatService,
            ItemService itemService,
            CombatCommandService commandService,
            PunishmentService punishmentService
    ) {
        this.settings = settings;
        this.combatService = combatService;
        this.itemService = itemService;
        this.commandService = commandService;
        this.punishmentService = punishmentService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = findAttackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        if (settings.isDisabledWorld(attacker) || settings.isDisabledWorld(victim)) {
            return;
        }

        combatService.tagPlayer(attacker, true);
        combatService.tagPlayer(victim, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        commandService.handleCommand(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        itemService.handleInteract(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        itemService.handleConsume(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (settings.cancelInteractWithEntities() && combatService.isInCombat(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        itemService.handleTotem(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        itemService.handleTeleport(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (settings.hideLeaveMessage()) {
            event.setQuitMessage(null);
        }

        if (combatService.isInCombat(player)) {
            punishmentService.punishCombatLeave(player, false);
            combatService.clearCombat(player, false);
        }

        itemService.clearPlayerState(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();

        if (settings.hideLeaveMessage()) {
            event.setLeaveMessage(null);
        }

        if (combatService.isInCombat(player) && punishmentService.matchesKickReason(event.getReason())) {
            punishmentService.punishCombatLeave(player, true);
            combatService.clearCombat(player, false);
        }

        itemService.clearPlayerState(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (settings.hideJoinMessage()) {
            event.setJoinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (settings.hideDeathMessage()) {
            event.setDeathMessage(null);
        }

        combatService.clearCombat(player, false);
        itemService.clearPlayerState(player);
    }

    private Player findAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }
}
