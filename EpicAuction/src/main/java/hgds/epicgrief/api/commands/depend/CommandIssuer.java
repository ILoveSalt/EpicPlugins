package hgds.epicgrief.api.commands.depend;

import org.bukkit.command.CommandSender;

public interface CommandIssuer {
    void execute(CommandSender paramCommandSender, String paramString, String[] paramArrayOfString);
}
