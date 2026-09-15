package hgds.epicgrief;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

final class HologramBridge {
    private final JavaPlugin plugin;
    private final ItemFactory itemFactory;
    private final Consumer<String> debug;
    private final Map<String, ExternalHologram> holograms = new HashMap<>();

    HologramBridge(JavaPlugin plugin, ItemFactory itemFactory, Consumer<String> debug) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
        this.debug = debug;
    }

    boolean spawn(CasePoint point, Location location) {
        remove(point);
        String id = id(point);
        try {
            if (isEnabled("DecentHolograms")) {
                Object hologram = spawnDecent(id, location, point.getHologramLines());
                holograms.put(point.getId().toLowerCase(Locale.ROOT), new ExternalHologram(
                        hologram, () -> invokeStatic("eu.decentsoftware.holograms.api.DHAPI", "removeHologram", id)
                ));
                return true;
            }
            if (isEnabled("HolographicDisplays")) {
                Object hologram = spawnHolographicDisplays(location, point.getHologramLines());
                holograms.put(point.getId().toLowerCase(Locale.ROOT), new ExternalHologram(
                        hologram, () -> invoke(hologram, "delete")
                ));
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            debug.accept("External hologram failed for " + point.getId() + ": " + exception.getMessage());
        }
        return false;
    }

    void remove(CasePoint point) {
        ExternalHologram hologram = holograms.remove(point.getId().toLowerCase(Locale.ROOT));
        if (hologram != null) {
            try {
                hologram.remover().run();
            } catch (RuntimeException exception) {
                debug.accept("Failed to remove external hologram " + point.getId());
            }
        }
    }

    void removeAll() {
        for (ExternalHologram hologram : List.copyOf(holograms.values())) {
            try {
                hologram.remover().run();
            } catch (RuntimeException ignored) {
            }
        }
        holograms.clear();
    }

    private Object spawnDecent(String id, Location location, List<String> lines) throws ReflectiveOperationException {
        Class<?> api = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
        Object hologram = api.getMethod("createHologram", String.class, Location.class)
                .invoke(null, id, location);
        for (String line : lines) {
            if (isItemLine(line)) {
                invokeCompatibleStatic(api, "addHologramLine", hologram, itemFactory.fromSpec(stripPrefix(line)));
            } else {
                invokeCompatibleStatic(api, "addHologramLine", hologram, stripPrefix(line));
            }
        }
        return hologram;
    }

    private Object spawnHolographicDisplays(Location location, List<String> lines) throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
        Object api = apiClass.getMethod("get", Plugin.class).invoke(null, plugin);
        Object hologram = apiClass.getMethod("createHologram", Location.class).invoke(api, location);
        Object hologramLines = invoke(hologram, "getLines");
        for (String line : lines) {
            if (isItemLine(line)) {
                invokeCompatible(hologramLines, "appendItem", itemFactory.fromSpec(stripPrefix(line)));
            } else {
                appendText(hologramLines, stripPrefix(line));
            }
        }
        return hologram;
    }

    private void appendText(Object lines, String text) throws ReflectiveOperationException {
        for (Method method : lines.getClass().getMethods()) {
            if (!method.getName().equals("appendText") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter == String.class) {
                method.invoke(lines, text);
                return;
            }
            if (parameter.getName().equals("net.kyori.adventure.text.Component")) {
                method.invoke(lines, LegacyComponentSerializer.legacyAmpersand().deserialize(text));
                return;
            }
        }
        throw new NoSuchMethodException("HologramLines.appendText");
    }

    private Object invokeCompatible(Object target, String methodName, Object argument)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argument.getClass())) {
                return method.invoke(target, argument);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private Object invokeCompatibleStatic(Class<?> type, String methodName, Object first, Object second)
            throws ReflectiveOperationException {
        for (Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || !method.getName().equals(methodName)
                    || method.getParameterCount() != 2) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(first.getClass())
                    && method.getParameterTypes()[1].isAssignableFrom(second.getClass())) {
                return method.invoke(null, first, second);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void invokeStatic(String className, String methodName, String value) {
        try {
            Class.forName(className).getMethod(methodName, String.class).invoke(null, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isEnabled(String pluginName) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(pluginName);
        return dependency != null && dependency.isEnabled();
    }

    private String id(CasePoint point) {
        return ("epiccase_" + point.getId()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private boolean isItemLine(String line) {
        String normalized = line == null ? "" : line.toLowerCase(Locale.ROOT);
        return normalized.startsWith("i:") || normalized.startsWith("item:");
    }

    private String stripPrefix(String line) {
        if (line == null) {
            return "";
        }
        int separator = line.indexOf(':');
        return separator < 0 ? line : line.substring(separator + 1);
    }

    private record ExternalHologram(Object handle, Runnable remover) {
    }
}
