package hgds.epicgrief;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class EpicStaffControl extends JavaPlugin {
    private StaffService staffService;

    @Override
    public void onEnable() {
        try {
            staffService = new StaffService(this);

            PluginCommand staffCommand = getCommand("staff");
            if (staffCommand == null) {
                throw new IllegalStateException("Command 'staff' is missing from plugin.yml");
            }

            StaffCommand commandHandler = new StaffCommand(staffService);
            staffCommand.setExecutor(commandHandler);
            staffCommand.setTabCompleter(commandHandler);
            registerCommand("spec", new SpecCommand(staffService));
            getServer().getPluginManager().registerEvents(new StaffListener(staffService), this);

            getLogger().info("Loaded " + staffService.getRanks().size() + " staff ranks.");
        } catch (Exception exception) {
            getLogger().severe("EpicStaffControl could not be enabled: " + exception.getMessage());
            getLogger().log(Level.SEVERE, "Startup failure", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (staffService != null) {
            staffService.shutdown();
        }
    }

    private void registerCommand(String name, Object handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' is missing from plugin.yml");
        }
        if (handler instanceof org.bukkit.command.CommandExecutor executor) {
            command.setExecutor(executor);
        }
        if (handler instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
