package hgds.epicgrief.api;

import hgds.epicgrief.api.commands.CommandsAPI;
import hgds.epicgrief.api.configuration.ConfigAPI;
import hgds.epicgrief.api.inventory.InventoryAPI;
import hgds.epicgrief.api.permission.PermissionsAPI;
import hgds.epicgrief.lib.commands.CommandsAPIImpl;
import hgds.epicgrief.lib.configuration.ConfigAPIImpl;
import hgds.epicgrief.lib.permissions.LuckPermsImpl;
import hgds.epicgrief.market.Market;
import hgds.epicgrief.packetlib.inventory.InventoryAPIImpl;

public final class GalaxyAPI {
    private static CommandsAPI commandsAPI;

    private static PermissionsAPI permissionsAPI;

    private static InventoryAPI inventoryAPI;

    private static ConfigAPI configAPI;

    private GalaxyAPI() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CommandsAPI getCommandsAPI() {
        if (commandsAPI == null)
            commandsAPI = (CommandsAPI)new CommandsAPIImpl();
        return commandsAPI;
    }

    public static PermissionsAPI getPermissionsAPI() {
        if (permissionsAPI == null)
            permissionsAPI = (PermissionsAPI)new LuckPermsImpl();
        return permissionsAPI;
    }

    public static InventoryAPI getInventoryAPI() {
        if (inventoryAPI == null)
            inventoryAPI = (InventoryAPI)new InventoryAPIImpl(Market.getInstance());
        return inventoryAPI;
    }

    public static ConfigAPI getConfigAPI() {
        if (configAPI == null)
            configAPI = (ConfigAPI)new ConfigAPIImpl();
        return configAPI;
    }
}

