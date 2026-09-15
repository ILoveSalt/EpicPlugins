package hgds.epicgrief.market.command;

import org.bukkit.command.CommandSender;
import hgds.epicgrief.api.GalaxyAPI;
import hgds.epicgrief.api.commands.CommandSource;
import hgds.epicgrief.api.commands.depend.CommandIssuer;
import hgds.epicgrief.market.Market;

public final class ReloadConfigCommand implements CommandIssuer {
    private final Market market;

    public ReloadConfigCommand(Market market) {
        this.market = market;
        CommandSource command = GalaxyAPI.getCommandsAPI().register("auctionreload", this);
        command.setOnlyConsole(true);
    }

    @Override
    public void execute(CommandSender gamerEntity, String s, String[] strings) {
        gamerEntity.sendMessage("§e§lAUC §8▪ §aКонфигурация аукциона перезагружена");
        this.market.reloadConfig();
    }
}
