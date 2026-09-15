package hgds.epicgrief;

import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class EpicAnimationChat extends JavaPlugin {
    private TabAnimationService animationService;
    private BukkitTask fileWatcher;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();

            animationService = new TabAnimationService(this);
            animationService.reload();

            AnimationChatCommand commandHandler = new AnimationChatCommand(this, animationService);
            PluginCommand command = getCommand("epicanimationchat");
            if (command == null) {
                throw new IllegalStateException("Command 'epicanimationchat' is missing from plugin.yml");
            }
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);

            getServer().getPluginManager().registerEvents(
                    new AnimationChatListener(this, animationService),
                    this
            );
            restartFileWatcher();

            getLogger().info("Loaded " + animationService.size() + " animations from "
                    + animationService.getAnimationsFile());
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "EpicAnimationChat could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (fileWatcher != null) {
            fileWatcher.cancel();
            fileWatcher = null;
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        animationService.reload();
        restartFileWatcher();
    }

    public Component replaceAnimations(Component component, Player source) {
        if (animationService == null) {
            return component;
        }
        return animationService.replaceAnimations(component, source);
    }

    private void restartFileWatcher() {
        if (fileWatcher != null) {
            fileWatcher.cancel();
            fileWatcher = null;
        }

        if (!getConfig().getBoolean("tab.auto-reload.enabled", true)) {
            return;
        }

        long period = Math.max(20L, getConfig().getLong("tab.auto-reload.check-interval-ticks", 100L));
        fileWatcher = getServer().getScheduler().runTaskTimer(
                this,
                animationService::reloadIfChanged,
                period,
                period
        );
    }
}
