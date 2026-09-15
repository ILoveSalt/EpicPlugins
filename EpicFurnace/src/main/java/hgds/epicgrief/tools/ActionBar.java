package hgds.epicgrief.tools;

import java.lang.reflect.Constructor;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ActionBar {
    public static void sendActionBar(Player player, String text) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        try {
            Object chat = ((Class)Objects.requireNonNull((T)getNMS("IChatBaseComponent"))).getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, new Object[] { "{\"text\": \"" + text + "\"}" });
            Object chattype = ((Class)Objects.<Class<?>>requireNonNull(getNMS("ChatMessageType"))).getField("GAME_INFO").get(null);
            Constructor<?> actionConstructor = ((Class)Objects.<Class<?>>requireNonNull(getNMS("PacketPlayOutChat"))).getConstructor(new Class[] { getNMS("IChatBaseComponent"), getNMS("ChatMessageType") });
            Object actionpacket = actionConstructor.newInstance(new Object[] { chat, chattype });
            sendPacket(player, actionpacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle", new Class[0]).invoke(player, new Object[0]);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", new Class[] { getNMS("Packet") }).invoke(playerConnection, new Object[] { packet });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Class<?> getNMS(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}