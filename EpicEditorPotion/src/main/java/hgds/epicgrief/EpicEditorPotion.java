package hgds.epicgrief;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EpicEditorPotion extends JavaPlugin {
    private PotionManager potionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        potionManager = new PotionManager(this);
        potionManager.reload();

        PotionCommand potionCommand = new PotionCommand(this, potionManager);
        PluginCommand command = getCommand("potions");
        if (command == null) {
            getLogger().severe("Команда potions не зарегистрирована в plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(potionCommand);
        command.setTabCompleter(potionCommand);
        getServer().getPluginManager().registerEvents(
                new PotionUseListener(potionManager),
                this
        );

        getLogger().info("Загружено кастомных зелий: " + potionManager.size());
    }

    @Override
    public void onDisable() {
        potionManager = null;
    }
}
