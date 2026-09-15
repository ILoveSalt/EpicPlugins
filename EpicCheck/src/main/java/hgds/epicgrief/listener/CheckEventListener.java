package hgds.epicgrief.listener;

import com.google.common.collect.ImmutableList;
import hgds.epicgrief.CheckConfig;
import hgds.epicgrief.CheckManager;
import hgds.epicgrief.Main;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;

public class CheckEventListener implements Listener {
    private static final String IAMCHEATER_CMD = "/iamcheater";

    private final CheckConfig config;

    private final CheckConfig.CheckRestrictions restrictions;

    private final CheckManager checkManager;

    public CheckEventListener(Main plugin) {
        this.config = plugin.getPluginConfig();
        this.restrictions = this.config.getRestrictions();
        this.checkManager = plugin.getCheckManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (this.restrictions.shouldRestrictMovement() && this.checkManager.isBeingChecked(event.getPlayer())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (this.restrictions.shouldRestrictChat() && this.checkManager.isBeingChecked(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        boolean beingChecked = false;
        if (this.config.isIAmCheaterCmdEnabled() && this.checkManager.isBeingChecked(player)) {
            beingChecked = true;
            if (message.startsWith("/iamcheater")) {
                event.setCancelled(true);
                this.checkManager.punishFair(player);
                return;
            }
        }
        if (this.restrictions.shouldRestrictCommands() && (
                beingChecked || this.checkManager.isBeingChecked(player))) {
            ImmutableList<String> allowedCommands = this.restrictions.getAllowedCommands();
            if (!allowedCommands.isEmpty()) {
                StringBuilder commandName = new StringBuilder(6);
                for (int i = 1; i < message.length(); i++) {
                    char character = message.charAt(i);
                    if (character == ' ') {
                        if (allowedCommands.contains(commandName.toString()))
                            return;
                        break;
                    }
                    commandName.append(character);
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (this.config.isIAmCheaterCmdEnabled()) {
            CommandSender sender = event.getSender();
            if (sender instanceof Player) {
                Player senderPlayer = (Player)sender;
                if (this.checkManager.isBeingChecked(senderPlayer) &&
                        "/iamcheater".startsWith(event.getBuffer()))
                    event.getCompletions().add("/iamcheater");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.restrictions.shouldRestrictBlockPlacing() && this.checkManager.isBeingChecked(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.restrictions.shouldRestrictBlockBreaking() && this.checkManager.isBeingChecked(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (this.restrictions.shouldRestrictDroppingItems() && this.checkManager.isBeingChecked(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (this.restrictions.shouldRestrictInventoryInteraction()) {
            Player player = (Player)event.getPlayer();
            if (this.checkManager.isBeingChecked(player))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.restrictions.shouldRestrictInventoryInteraction()) {
            Player player = (Player)event.getWhoClicked();
            if (this.checkManager.isBeingChecked(player))
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {
        if (this.restrictions.shouldRestrictAttacking()) {
            Entity damager = event.getDamager();
            if (damager instanceof Player) {
                Player player = (Player)damager;
                if (this.checkManager.isBeingChecked(player))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageEvent event) {
        if (this.config.shouldPlayerBeInvulnerableWhileChecking()) {
            Entity damagedEntity = event.getEntity();
            if (damagedEntity instanceof Player) {
                Player player = (Player)damagedEntity;
                if (this.checkManager.isBeingChecked(player))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.checkManager.isBeingChecked(player))
            this.checkManager.punishQuit(player);
    }
}
