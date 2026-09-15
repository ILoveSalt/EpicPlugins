package hgds.epicgrief;

import hgds.epicgrief.arrows.ArrowConfigService;
import hgds.epicgrief.arrows.ArrowItemService;
import hgds.epicgrief.arrows.ArrowListener;
import hgds.epicgrief.arrows.ArrowsCommand;
import hgds.epicgrief.arrows.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EpicArrows extends JavaPlugin {

    private ArrowConfigService configService;
    private ArrowListener arrowListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("arrows.yml", false);

        MessageService messages = new MessageService(this);
        configService = new ArrowConfigService(this);
        configService.reload();

        ArrowItemService itemService = new ArrowItemService(this, configService, messages);
        arrowListener = new ArrowListener(this, configService, itemService, messages);

        getServer().getPluginManager().registerEvents(arrowListener, this);
        arrowListener.start();
        registerCommand(messages, itemService);

        getLogger().info("EpicArrows enabled. Loaded arrows: " + configService.arrows().size() + ".");
    }

    @Override
    public void onDisable() {
        if (arrowListener != null) {
            arrowListener.stop();
        }

        getLogger().info("EpicArrows disabled.");
    }

    private void registerCommand(MessageService messages, ArrowItemService itemService) {
        PluginCommand command = getCommand("arrows");
        if (command == null) {
            getLogger().warning("Command arrows is not registered in plugin.yml.");
            return;
        }

        ArrowsCommand executor = new ArrowsCommand(this, configService, itemService, messages);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
