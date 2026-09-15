package hgds.epicgrief.lib.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.entity.Player;
import hgds.epicgrief.api.permission.PermissionsAPI;

/**
 * Реализация через LuckPerms API. Если LuckPerms не установлен,
 * все игроки считаются группой "default".
 */
public class LuckPermsImpl implements PermissionsAPI {
    private UserManager userManager;

    private UserManager getUserManager() {
        if (this.userManager == null) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            this.userManager = luckPerms.getUserManager();
        }
        return this.userManager;
    }

    @Override
    public String getPlayerGroup(Player player) {
        return getPlayerGroup(player.getName());
    }

    @Override
    public String getPlayerGroup(String playerName) {
        try {
            User user = getUserManager().getUser(playerName);
            if (user == null)
                return "default";
            return user.getPrimaryGroup();
        } catch (IllegalStateException | NullPointerException e) {
            return "default";
        }
    }

    @Override
    public void setPlayerGroup(Player player, String groupName) {
        setPlayerGroup(player.getName(), groupName);
    }

    @Override
    public void setPlayerGroup(String playerName, String groupName) {
        try {
            User user = getUserManager().getUser(playerName);
            if (user != null)
                user.setPrimaryGroup(groupName);
        } catch (IllegalStateException | NullPointerException ignored) {
        }
    }
}
