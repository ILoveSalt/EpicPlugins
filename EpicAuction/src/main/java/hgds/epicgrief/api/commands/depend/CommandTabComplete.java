package hgds.epicgrief.api.commands.depend;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface CommandTabComplete {
    List<String> getComplete(CommandSender paramCommandSender, String paramString, String[] paramArrayOfString);
}

