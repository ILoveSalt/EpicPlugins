package hgds.epicgrief;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EpicRegionsBar extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final String GLOBAL_REGION_ID = "__global__";
    private static final long ACTION_BAR_PERIOD_TICKS = 20L;

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    private WorldGuardPlugin worldGuardPlugin;
    private RegionContainer regionContainer;
    private BukkitTask actionBarTask;
    private boolean worldGuardWarningShown;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadWorldGuard();
        registerCommand();
        startActionBarTask();
    }

    @Override
    public void onDisable() {
        stopActionBarTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            reloadWorldGuard();
            sender.sendMessage(color(getConfig().getString("messages.reload", "&aКонфигурация перезагружена.")));
            return true;
        }

        sender.sendMessage(color(getConfig().getString("messages.command-not-found", "&cНеизвестная команда.")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "reload").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("vregbar");
        if (command == null) {
            getLogger().warning("Команда /vregbar не найдена в plugin.yml.");
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private void reloadWorldGuard() {
        if (!(Bukkit.getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin plugin) || !plugin.isEnabled()) {
            worldGuardPlugin = null;
            regionContainer = null;

            if (!worldGuardWarningShown) {
                getLogger().warning("WorldGuard не найден. Информация о регионах не будет отображаться.");
                worldGuardWarningShown = true;
            }

            return;
        }

        worldGuardPlugin = plugin;
        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        worldGuardWarningShown = false;
    }

    private void startActionBarTask() {
        stopActionBarTask();

        actionBarTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (regionContainer == null || worldGuardPlugin == null) {
                reloadWorldGuard();
            }

            if (regionContainer == null || worldGuardPlugin == null) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(buildRegionBar(player));
            }
        }, 0L, ACTION_BAR_PERIOD_TICKS);
    }

    private void stopActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    private Component buildRegionBar(Player player) {
        RegionQuery query = regionContainer.createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        String regionName = resolveRegionName(regions);
        String pvpStatus = resolvePvpStatus(player, regions);
        String message = getConfig().getString("region-viewer", "%region% | %pvp%")
                .replace("%region%", regionName)
                .replace("%pvp%", pvpStatus);

        return color(message);
    }

    private String resolveRegionName(ApplicableRegionSet regions) {
        List<ProtectedRegion> sortedRegions = new ArrayList<>();
        regions.forEach(sortedRegions::add);

        if (sortedRegions.isEmpty()) {
            return getConfig().getString("no-rg", "&4&lНет");
        }

        sortedRegions.sort(Comparator
                .comparingInt(ProtectedRegion::getPriority)
                .reversed()
                .thenComparing(ProtectedRegion::getId));

        return formatRegionName(sortedRegions.get(0).getId());
    }

    private String formatRegionName(String regionId) {
        if (regionId.equalsIgnoreCase(GLOBAL_REGION_ID)) {
            return getConfig().getString("global-name", regionId);
        }

        String placeholder = getConfig().getString("region-placeholder." + regionId);
        if (placeholder != null) {
            return placeholder;
        }

        ConfigurationSection placeholders = getConfig().getConfigurationSection("region-placeholder");
        if (placeholders == null) {
            return regionId;
        }

        for (Map.Entry<String, Object> entry : placeholders.getValues(false).entrySet()) {
            if (entry.getKey().equalsIgnoreCase(regionId) && entry.getValue() instanceof String value) {
                return value;
            }
        }

        return regionId;
    }

    private String resolvePvpStatus(Player player, ApplicableRegionSet regions) {
        StateFlag.State pvpState = regions.queryState(worldGuardPlugin.wrapPlayer(player), Flags.PVP);
        boolean pvpEnabled = pvpState == null ? player.getWorld().getPVP() : pvpState == StateFlag.State.ALLOW;

        return getConfig().getString(pvpEnabled ? "pvp-on" : "pvp-off", pvpEnabled ? "&2&lВключено" : "&4&lВыключено");
    }

    private void sendHelp(CommandSender sender) {
        for (String line : getConfig().getStringList("messages.help")) {
            sender.sendMessage(color(line));
        }
    }

    private Component color(String message) {
        return legacySerializer.deserialize(message == null ? "" : message);
    }
}
