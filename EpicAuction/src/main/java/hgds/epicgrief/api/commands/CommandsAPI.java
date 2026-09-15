package hgds.epicgrief.api.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import hgds.epicgrief.api.commands.depend.CommandIssuer;

public interface CommandsAPI {
    CommandSource register(String paramString, CommandIssuer paramCommandIssuer, String... paramVarArgs);

    CommandSource getCommand(String paramString);

    Map<String, CommandSource> getCommands();

    List<String> getCompleteString(Collection<String> paramCollection, String... paramVarArgs);

    void disableCommand(String paramString);

    void disableCommand(CommandSource paramCommandSource);

    void disableAllCommands(JavaPlugin paramJavaPlugin);
}
