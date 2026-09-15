package hgds.epicgrief;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;

public final class ConfiguredChannelCommand extends Command {
    private final ConfiguredChannelExecutor executor;

    public ConfiguredChannelCommand(String name, ConfiguredChannelExecutor executor) {
        super(name);
        this.executor = executor;
        setDescription("Configured EpicChat channel");
        setUsage("/" + name + " <message>");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return Collections.emptyList();
    }
}
