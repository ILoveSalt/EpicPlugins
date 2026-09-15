package hgds.epicgrief;

import hgds.epicgrief.command.CheckCommand;
import hgds.epicgrief.listener.CheckEventListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Точка входа плагина EpicCheck (Paper 26.1.2, Java 25).
 */
public class Main extends JavaPlugin {
    private CheckConfig pluginConfig;

    private CheckManager checkManager;

    public CheckConfig getPluginConfig() {
        return this.pluginConfig;
    }

    public CheckManager getCheckManager() {
        return this.checkManager;
    }

    @Override
    public void onEnable() {
        // Создаём дефолтный config.yml, если его ещё нет, и загружаем конфигурацию.
        saveDefaultConfig();
        this.pluginConfig = new CheckConfig(getConfig());
        this.checkManager = new CheckManager(this);

        // Регистрируем обработчики событий (listener помечен как org.bukkit.event.Listener).
        getServer().getPluginManager().registerEvents(new CheckEventListener(this), this);

        // Навешиваем на команду /check исполнителя и автодополнение.
        PluginCommand command = getCommand("check");
        if (command != null) {
            CheckCommand checkCommand = new CheckCommand(this);
            command.setExecutor(checkCommand);
            command.setTabCompleter(checkCommand);
        }
    }

    @Override
    public void onDisable() {
        // Останавливаем периодический уведомитель проверяемых игроков.
        if (this.checkManager != null) {
            this.checkManager.dispose();
        }
    }
}