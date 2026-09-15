package hgds.epicgrief;

import org.bukkit.plugin.java.JavaPlugin;

public final class EpicPotions extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PotionStackManager stackManager = new PotionStackManager(this);
        getServer().getPluginManager().registerEvents(stackManager, this);
        stackManager.start();

        getLogger().info("Potion stacking is enabled.");
    }
}
