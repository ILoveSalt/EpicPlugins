package hgds.epicgrief;

import hgds.epicgrief.api.EpicPluginApi;
import hgds.epicgrief.api.EpicPluginConnection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class EpicLoadCustomPlugin extends JavaPlugin implements Listener {

    private static EpicLoadCustomPlugin instance;

    private final EpicPluginApiImpl epicPluginApi = new EpicPluginApiImpl(this);

    public static EpicLoadCustomPlugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EpicLoadCustomPlugin is not enabled");
        }

        return instance;
    }

    public static EpicPluginApi getApi() {
        return getInstance().api();
    }

    public EpicPluginApi api() {
        return epicPluginApi;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getServicesManager().register(EpicPluginApi.class, epicPluginApi, this, ServicePriority.Normal);

        getLogger().info("Запускается главный плагин для запуска плагинов от разработчика hgds для сервера EpicGrief.");
        logServerCoreVersion();
        getLogger().info("Полная проверка плагинов будет выполнена после запуска сервера.");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        epicPluginApi.disconnectAll();
        getServer().getServicesManager().unregisterAll(this);
        instance = null;
        getLogger().info("Главный плагин EpicGrief от hgds остановлен.");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        getLogger().info("Сервер запущен. Проверяю плагины на сервере EpicGrief...");
        logPluginStatus();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof JavaPlugin plugin) {
            epicPluginApi.disconnect(plugin);
        }
    }

    private void logServerCoreVersion() {
        getLogger().info("Ядро сервера: " + getServer().getName());
        getLogger().info("Версия ядра сервера: " + getServer().getVersion());
        getLogger().info("Версия Bukkit/Paper API: " + getServer().getBukkitVersion());
    }

    private void logPluginStatus() {
        Plugin[] installedPlugins = getServer().getPluginManager().getPlugins();
        List<String> installed = new ArrayList<>();
        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();

        for (Plugin plugin : installedPlugins) {
            installed.add(plugin.getName());

            if (plugin.isEnabled()) {
                enabled.add(plugin.getName());
            } else {
                disabled.add(plugin.getName());
            }
        }

        logList("Установленные плагины", installed);
        logList("Запущенные плагины", enabled);
        logList("Не запущенные плагины", disabled);
        logRequiredPlugins();
        logConnectedEpicPlugins();
    }

    private void logRequiredPlugins() {
        List<String> requiredPlugins = getConfig().getStringList("required-plugins");

        if (requiredPlugins.isEmpty()) {
            getLogger().info("Список required-plugins пуст. Добавьте туда нужные плагины, чтобы видеть, какие из них не установлены.");
            return;
        }

        List<String> installedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();

        for (String pluginName : requiredPlugins) {
            if (pluginName == null || pluginName.isBlank()) {
                continue;
            }

            Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);

            if (plugin == null) {
                missingRequired.add(pluginName);
            } else if (plugin.isEnabled()) {
                installedRequired.add(pluginName + " - установлен и запущен");
            } else {
                installedRequired.add(pluginName + " - установлен, но не запущен");
            }
        }

        logList("Плагины из required-plugins, которые установлены", installedRequired);
        logList("Плагины из required-plugins, которые не установлены", missingRequired);
    }

    private void logList(String title, List<String> values) {
        if (values.isEmpty()) {
            getLogger().info(title + ": нет");
            return;
        }

        getLogger().info(title + ": " + String.join(", ", values));
    }

    private void logConnectedEpicPlugins() {
        List<String> connectedEpicPlugins = new ArrayList<>();

        for (EpicPluginConnection connection : epicPluginApi.getConnections()) {
            connectedEpicPlugins.add(connection.getName());
        }

        logList("Epic-плагины, подключенные через API", connectedEpicPlugins);
    }
}
