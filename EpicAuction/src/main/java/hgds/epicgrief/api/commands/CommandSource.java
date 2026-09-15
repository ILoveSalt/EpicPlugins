package hgds.epicgrief.api.commands;

import java.util.List;
import org.bukkit.command.Command;
import hgds.epicgrief.api.commands.depend.CommandIssuer;
import hgds.epicgrief.api.commands.depend.CommandTabComplete;

public interface CommandSource {
    String getName();

    Command getCommand();

    List<String> getAliases();

    void setCommandIssuer(CommandIssuer paramCommandIssuer);

    void setCommandTabComplete(CommandTabComplete paramCommandTabComplete);

    void setOnlyConsole(boolean paramBoolean);

    void setOnlyPlayers(boolean paramBoolean);

    void setCooldown(String paramString, int paramInt);

    void disable();
}