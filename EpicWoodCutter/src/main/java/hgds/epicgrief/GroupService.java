package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

final class GroupService {

    private final JavaPlugin plugin;
    private final List<WoodGroupSettings> groups = new ArrayList<>();
    private Object permissionProvider;
    private boolean permissionChecked;

    GroupService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void load(ConfigurationSection section) {
        groups.clear();
        if (section == null) {
            groups.add(new WoodGroupSettings("default", 10, 1.0));
            return;
        }

        for (String groupName : section.getKeys(false)) {
            ConfigurationSection groupSection = section.getConfigurationSection(groupName);
            if (groupSection == null) {
                continue;
            }

            int max = groupSection.getInt("max", 10);
            double booster = groupSection.getDouble("booster", 1.0);
            groups.add(new WoodGroupSettings(groupName.toLowerCase(Locale.ROOT), max, booster));
        }

        groups.sort(Comparator.comparingInt(WoodGroupSettings::maxBackpack).reversed());
    }

    WoodGroupSettings resolve(Player player) {
        WoodGroupSettings fallback = groups.stream()
                .filter(group -> group.name().equals("default"))
                .findFirst()
                .orElseGet(() -> groups.isEmpty()
                        ? new WoodGroupSettings("default", 10, 1.0)
                        : groups.get(groups.size() - 1));

        if (!ensurePermission()) {
            return fallback;
        }

        for (WoodGroupSettings group : groups) {
            if (playerInGroup(player, group.name())) {
                return group;
            }
        }

        return fallback;
    }

    private boolean playerInGroup(Player player, String group) {
        Boolean direct = invokeBoolean("playerInGroup", new Class<?>[]{Player.class, String.class}, player, group);
        if (Boolean.TRUE.equals(direct)) {
            return true;
        }

        Boolean offline = invokeBoolean(
                "playerInGroup",
                new Class<?>[]{String.class, String.class, String.class},
                player.getWorld().getName(),
                player.getName(),
                group
        );
        if (Boolean.TRUE.equals(offline)) {
            return true;
        }

        String primary = invokeString("getPrimaryGroup", new Class<?>[]{Player.class}, player);
        if (primary == null) {
            primary = invokeString(
                    "getPrimaryGroup",
                    new Class<?>[]{String.class, String.class},
                    player.getWorld().getName(),
                    player.getName()
            );
        }

        return primary != null && primary.equalsIgnoreCase(group);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean ensurePermission() {
        if (permissionChecked) {
            return permissionProvider != null;
        }

        permissionChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }

        try {
            Class<?> permissionClass = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) permissionClass);
            if (registration == null) {
                return false;
            }

            permissionProvider = registration.getProvider();
            return permissionProvider != null;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Vault permission service недоступен", exception);
            return false;
        }
    }

    private Boolean invokeBoolean(String methodName, Class<?>[] parameterTypes, Object... args) {
        Object value = invoke(methodName, parameterTypes, args);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    private String invokeString(String methodName, Class<?>[] parameterTypes, Object... args) {
        Object value = invoke(methodName, parameterTypes, args);
        return value == null ? null : String.valueOf(value);
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (permissionProvider == null) {
            return null;
        }

        try {
            Method method = permissionProvider.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(permissionProvider, args);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
