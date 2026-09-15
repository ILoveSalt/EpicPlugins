package hgds.epicgrief;

import hgds.epicgrief.bosses.BossCommand;
import hgds.epicgrief.bosses.BossConfigService;
import hgds.epicgrief.bosses.BossService;
import hgds.epicgrief.bosses.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EpicBosses extends JavaPlugin {

    private BossConfigService configService;
    private BossService bossService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MessageService messages = new MessageService(this);
        configService = new BossConfigService(this);
        configService.reload();

        bossService = new BossService(this, configService, messages);
        getServer().getPluginManager().registerEvents(bossService, this);
        bossService.start();

        registerCommand(messages);

        getLogger().info("EpicBosses enabled. Loaded bosses: " + configService.bosses().size() + ".");
    }

    @Override
    public void onDisable() {
        if (bossService != null) {
            bossService.stop();
        }

        getLogger().info("EpicBosses disabled.");
    }

    private void registerCommand(MessageService messages) {
        PluginCommand command = getCommand("boss");
        if (command == null) {
            getLogger().warning("Command boss is not registered in plugin.yml.");
            return;
        }

        BossCommand executor = new BossCommand(configService, bossService, messages);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
