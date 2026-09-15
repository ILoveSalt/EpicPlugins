package hgds.epicgrief;

import java.util.Locale;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import hgds.epicgrief.utils.CommandResolver;
import hgds.epicgrief.utils.Config;
import hgds.epicgrief.utils.Utils;

/**
 * EpicCooldown — кулдауны на команды для групп игроков.
 * Переписано под Paper 26.1.2 (Java 25): без NMS-рефлексии,
 * отображение через Adventure API.
 */
public final class CooldownPlugin extends JavaPlugin implements Listener {

    /** Право, дающее игроку иммунитет к кулдаунам. */
    public static final String BYPASS_PERMISSION = "epiccooldown.bypass";

    private Permission vaultPermission;
    private Cooldown cooldowns;
    private CommandResolver commandResolver;

    @Override
    public void onEnable() {
        Config.init(this);
        Utils.init(this);

        this.cooldowns = new Cooldown(this);
        this.cooldowns.load();

        this.commandResolver = new CommandResolver();

        setupPermission();

        getServer().getPluginManager().registerEvents(this, this);

        EpicCooldownCommand adminCommand = new EpicCooldownCommand(this);
        PluginCommand command = getCommand("epiccooldowns");
        if (command != null) {
            command.setExecutor(adminCommand);
            command.setTabCompleter(adminCommand);
        }

        this.cooldowns.startAutoSave();

        getLogger().info("EpicCooldown v" + getPluginMeta().getVersion() + " включён. Автор: WinLocker");
    }

    @Override
    public void onDisable() {
        if (this.cooldowns != null) {
            this.cooldowns.stopAutoSave();
            this.cooldowns.save();
        }
        HandlerList.unregisterAll((Listener) this);
    }

    /** Подключение к VaultUnlocked/Vault (права). При отсутствии — fallback по правам group.<имя>. */
    private void setupPermission() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault/VaultUnlocked не найден! Группы будут определяться по праву 'group.<имя>'.");
            this.vaultPermission = null;
            return;
        }
        RegisteredServiceProvider<Permission> registration =
                getServer().getServicesManager().getRegistration(Permission.class);
        if (registration == null) {
            getLogger().warning("Vault установлен, но поставщик прав не зарегистрирован.");
            this.vaultPermission = null;
            return;
        }
        this.vaultPermission = registration.getProvider();
        getLogger().info("Vault найден, провайдер прав: " + this.vaultPermission.getName());
    }

    /** Основная группа игрока: через Vault, либо по праву group.<имя группы>. */
    public String getPrimaryGroup(Player player) {
        if (this.vaultPermission != null) {
            try {
                String group = this.vaultPermission.getPrimaryGroup(player);
                if (group != null && !group.isEmpty()) {
                    return group.toLowerCase(Locale.ROOT);
                }
                return null;
            } catch (Throwable t) {
                getLogger().warning("Ошибка Vault getPrimaryGroup: " + t.getMessage());
                return null;
            }
        }
        ConfigurationSection groups = Utils.getConfig().getConfigurationSection("groups");
        if (groups == null) {
            return null;
        }
        for (String group : groups.getKeys(false)) {
            if (player.hasPermission("group." + group.toLowerCase(Locale.ROOT))) {
                return group.toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    public Cooldown getCooldowns() {
        return this.cooldowns;
    }

    public CommandResolver getCommandResolver() {
        return this.commandResolver;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.length() < 2) {
            return;
        }
        String label = message.split(" ")[0];
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        if (label.isEmpty()) {
            return;
        }

        String group = getPrimaryGroup(player);
        if (group == null) {
            return;
        }

        ConfigurationSection section = Utils.getConfig().getConfigurationSection("groups." + group);
        if (section == null) {
            return;
        }

        for (String commandKey : section.getKeys(false)) {
            if (!this.commandResolver.matches(commandKey, label)) {
                continue;
            }

            int seconds = section.getInt(commandKey, 0);
            if (seconds <= 0) {
                return; // кулдаун для этой команды отключён (0 или меньше)
            }

            String key = player.getName() + "-" + commandKey;
            if (this.cooldowns.has(key)) {
                String time = Utils.format(this.cooldowns.secondsLeft(key));
                Utils.sendMessage(player, Utils.getMessage("cooldown").replace("%time%", time));
                event.setCancelled(true);
            } else {
                this.cooldowns.add(key, seconds);
            }
            return;
        }
    }
}