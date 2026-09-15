package hgds.epicgrief.util.command;

import java.util.Map;
import hgds.epicgrief.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class ExecutableCommand {
    private final String cmd;

    public ExecutableCommand(String cmd) {
        this.cmd = cmd;
    }

    public String getCmd() {
        return this.cmd;
    }

    public void execute(CommandSender executor, Map<String, String> args) {
        Bukkit.dispatchCommand(executor, StringUtil.format(this.cmd, args));
    }
}