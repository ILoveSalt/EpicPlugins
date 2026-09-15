package hgds.epicgrief;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EpicSpawn extends JavaPlugin implements Listener, TabExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommand("spawn");
        registerCommand("setspawn");
        registerCommand("epicspawn");
        applyWorldSpawn();
        getLogger().info("EpicSpawn enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EpicSpawn disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "spawn" -> teleportToSpawn(sender, args);
            case "setspawn" -> setSpawn(sender);
            case "epicspawn" -> handleAdmin(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (args.length != 1 || !can(sender, "epicspawn.spawn.other")) {
                return Collections.emptyList();
            }

            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(playerName);
                }
            }
            return result;
        }

        if (!command.getName().equalsIgnoreCase("epicspawn") || args.length != 1 || !can(sender, "epicspawn.reload")) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            result.add("reload");
        }
        return result;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInitialSpawn(AsyncPlayerSpawnLocationEvent event) {
        if (!event.isNewPlayer() || !getConfig().getBoolean("join.enabled", true)) {
            return;
        }

        Location spawn = readSpawn();
        if (spawn != null) {
            event.setSpawnLocation(spawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!getConfig().getBoolean("respawn.enabled", true)) {
            return;
        }

        Location spawn = readSpawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    private boolean teleportToSpawn(CommandSender sender, String[] args) {
        if (args.length > 1) {
            send(sender, "messages.spawn-usage");
            return true;
        }

        if (args.length == 1) {
            return teleportPlayerToSpawn(sender, args[0]);
        }

        if (!(sender instanceof Player player)) {
            send(sender, "messages.only-players");
            return true;
        }

        if (!can(sender, "epicspawn.spawn")) {
            send(sender, "messages.no-permission");
            return true;
        }

        Location spawn = readSpawnWithMessages(sender);
        if (spawn == null) {
            return true;
        }

        send(player, "messages.teleport-start");
        player.teleport(spawn, PlayerTeleportEvent.TeleportCause.COMMAND);
        send(player, "messages.teleport-success");
        return true;
    }

    private boolean teleportPlayerToSpawn(CommandSender sender, String playerName) {
        if (!can(sender, "epicspawn.spawn.other")) {
            send(sender, "messages.no-permission");
            return true;
        }

        Player target = findOnlinePlayer(playerName);
        if (target == null) {
            sendPlayer(sender, "messages.player-not-found", playerName);
            return true;
        }

        Location spawn = readSpawnWithMessages(sender);
        if (spawn == null) {
            return true;
        }

        target.teleport(spawn, PlayerTeleportEvent.TeleportCause.COMMAND);
        sendPlayer(sender, "messages.teleport-other-success", target.getName());
        send(target, "messages.teleport-by-other");
        return true;
    }

    private boolean setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "messages.only-players");
            return true;
        }

        if (!can(sender, "epicspawn.setspawn")) {
            send(sender, "messages.no-permission");
            return true;
        }

        Location location = player.getLocation();
        FileConfiguration config = getConfig();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        saveConfig();
        applyWorldSpawn(location);

        send(player, "messages.setspawn-success", location);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!can(sender, "epicspawn.reload")) {
                send(sender, "messages.no-permission");
                return true;
            }

            reloadConfig();
            applyWorldSpawn();
            send(sender, "messages.reload-success");
            return true;
        }

        send(sender, "messages.unknown-command");
        return true;
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command " + name + " is not registered in plugin.yml.");
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void applyWorldSpawn() {
        Location spawn = readSpawn();
        if (spawn != null) {
            applyWorldSpawn(spawn);
        }
    }

    private void applyWorldSpawn(Location location) {
        if (!getConfig().getBoolean("spawn.set-world-spawn", true) || location.getWorld() == null) {
            return;
        }

        location.getWorld().setSpawnLocation(location);
    }

    private Location readSpawnWithMessages(CommandSender sender) {
        String worldName = getConfig().getString("spawn.world", "");
        if (worldName == null || worldName.isBlank()) {
            send(sender, "messages.spawn-not-set");
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            send(sender, "messages.world-not-found", worldName);
            return null;
        }

        return readSpawn(world);
    }

    private Location readSpawn() {
        String worldName = getConfig().getString("spawn.world", "");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        return world == null ? null : readSpawn(world);
    }

    private Location readSpawn(World world) {
        FileConfiguration config = getConfig();
        return new Location(
                world,
                config.getDouble("spawn.x", 0.5D),
                config.getDouble("spawn.y", 64.0D),
                config.getDouble("spawn.z", 0.5D),
                (float) config.getDouble("spawn.yaw", 0.0D),
                (float) config.getDouble("spawn.pitch", 0.0D)
        );
    }

    private Player findOnlinePlayer(String playerName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player;
            }
        }
        return null;
    }

    private boolean can(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("epicspawn.admin");
    }

    private void send(CommandSender sender, String path) {
        sender.sendMessage(color(getConfig().getString("messages.prefix", "") + getConfig().getString(path, "")));
    }

    private void send(CommandSender sender, String path, String worldName) {
        sender.sendMessage(color((getConfig().getString("messages.prefix", "") + getConfig().getString(path, ""))
                .replace("%world%", worldName == null ? "" : worldName)));
    }

    private void sendPlayer(CommandSender sender, String path, String playerName) {
        sender.sendMessage(color((getConfig().getString("messages.prefix", "") + getConfig().getString(path, ""))
                .replace("%player%", playerName == null ? "" : playerName)));
    }

    private void send(CommandSender sender, String path, Location location) {
        sender.sendMessage(color((getConfig().getString("messages.prefix", "") + getConfig().getString(path, ""))
                .replace("%world%", location.getWorld() == null ? "" : location.getWorld().getName())
                .replace("%x%", format(location.getX()))
                .replace("%y%", format(location.getY()))
                .replace("%z%", format(location.getZ()))
                .replace("%yaw%", format(location.getYaw()))
                .replace("%pitch%", format(location.getPitch()))));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
