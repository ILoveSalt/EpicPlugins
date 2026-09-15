package hgds.epicgrief.api.permission;

import org.bukkit.entity.Player;

public interface PermissionsAPI {
    String getPlayerGroup(Player paramPlayer);

    String getPlayerGroup(String paramString);

    void setPlayerGroup(Player paramPlayer, String paramString);

    void setPlayerGroup(String paramString1, String paramString2);
}
