package hgds.epicAntiRelog;

import hgds.epicgrief.api.EpicPlugin;
import hgds.epicgrief.api.EpicPluginApi;
import hgds.epicgrief.api.EpicPluginContext;
import hgds.epicAntiRelog.combat.CombatService;
import hgds.epicAntiRelog.command.CombatCommandService;
import hgds.epicAntiRelog.command.ConfigItemCommand;
import hgds.epicAntiRelog.command.ConsoleCommandService;
import hgds.epicAntiRelog.config.AntiRelogSettings;
import hgds.epicAntiRelog.item.ItemService;
import hgds.epicAntiRelog.listener.AntiRelogListener;
import hgds.epicAntiRelog.message.MessageService;
import hgds.epicAntiRelog.punishment.PunishmentService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EpicAntiRelog extends JavaPlugin implements EpicPlugin {

    private AntiRelogSettings settings;
    private CombatService combatService;
    private ItemService itemService;
    private EpicPluginApi epicPluginApi;
    private boolean connectedToEpicApi;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!isEpicLoadCustomPluginAvailable()) {
            getLogger().severe("EpicLoadCustomPlugin is not enabled. EpicAntiRelog will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!connectToEpicApi()) {
            getLogger().severe("EpicLoadCustomPlugin API is not available. EpicAntiRelog will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        settings = new AntiRelogSettings(this);
        settings.reload();

        MessageService messageService = new MessageService(this);
        ConsoleCommandService consoleCommandService = new ConsoleCommandService(this);
        combatService = new CombatService(this, settings, messageService, consoleCommandService);
        itemService = new ItemService(this, settings, messageService, combatService);
        PunishmentService punishmentService = new PunishmentService(settings, messageService, combatService, consoleCommandService);
        CombatCommandService commandService = new CombatCommandService(settings, messageService, combatService);
        registerConfigItemCommand(messageService);

        getServer().getPluginManager().registerEvents(
                new AntiRelogListener(settings, combatService, itemService, commandService, punishmentService),
                this
        );
        combatService.start();

        getLogger().info("EpicAntiRelog enabled. PVP duration: " + settings.pvpTimeSeconds() + " seconds.");
    }

    @Override
    public void onDisable() {
        if (combatService != null) {
            combatService.stop();
        }

        if (itemService != null) {
            itemService.clear();
        }

        if (connectedToEpicApi && epicPluginApi != null) {
            epicPluginApi.disconnect(this);
            connectedToEpicApi = false;
        }

        getLogger().info("EpicAntiRelog disabled.");
    }

    @Override
    public void onEpicPluginConnected(EpicPluginContext context) {
        getLogger().info("Connected to EpicLoadCustomPlugin API.");
    }

    @Override
    public void onEpicPluginDisconnected(EpicPluginContext context) {
        getLogger().info("Disconnected from EpicLoadCustomPlugin API.");
    }

    private boolean isEpicLoadCustomPluginAvailable() {
        Plugin plugin = getServer().getPluginManager().getPlugin("EpicLoadCustomPlugin");
        return plugin != null && plugin.isEnabled();
    }

    private boolean connectToEpicApi() {
        RegisteredServiceProvider<EpicPluginApi> provider = getServer()
                .getServicesManager()
                .getRegistration(EpicPluginApi.class);

        if (provider == null) {
            return false;
        }

        epicPluginApi = provider.getProvider();
        epicPluginApi.connect(this, this);
        connectedToEpicApi = true;
        return true;
    }

    private void registerConfigItemCommand(MessageService messageService) {
        PluginCommand command = getCommand("epicantirelog");
        if (command == null) {
            getLogger().warning("Command epicantirelog is not registered in plugin.yml.");
            return;
        }

        ConfigItemCommand executor = new ConfigItemCommand(this, settings, messageService, itemService);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
