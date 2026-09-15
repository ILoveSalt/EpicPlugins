package hgds.epicgrief.tools;

import java.lang.reflect.Constructor;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Title {
    public static void sendTitle(Player player, String text) {
        sendTitle(player, text, 15, 60, 15);
    }

    public static void sendTitle(Player player, String text, int fadein, int stay, int fadeout) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        String[] args = text.split("%nl%");
        try {
            String title = args[0];
            Object e1 = ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0].getField("TIMES").get(null);
            Object chatTitle = ((Class)Objects.requireNonNull((T)getNMS("IChatBaseComponent"))).getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, new Object[] { "{\"text\": \"" + title + "\"}" });
            Constructor<?> titleConstructor = ((Class)Objects.<Class<?>>requireNonNull(getNMS("PacketPlayOutTitle"))).getConstructor(new Class[] { ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0], getNMS("IChatBaseComponent"), int.class, int.class, int.class });
            Object titlepacket = titleConstructor.newInstance(new Object[] { e1, chatTitle, Integer.valueOf(fadein), Integer.valueOf(stay), Integer.valueOf(fadeout) });
            sendPacket(player, titlepacket);
            e1 = ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0].getField("TITLE").get(null);
            chatTitle = ((Class)Objects.requireNonNull((T)getNMS("IChatBaseComponent"))).getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, new Object[] { "{\"text\": \"" + title + "\"}" });
            titleConstructor = ((Class)Objects.<Class<?>>requireNonNull(getNMS("PacketPlayOutTitle"))).getConstructor(new Class[] { ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0], getNMS("IChatBaseComponent") });
            titlepacket = titleConstructor.newInstance(new Object[] { e1, chatTitle });
            sendPacket(player, titlepacket);
            if (args.length == 2) {
                String subtitle = args[1];
                Object e2 = ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0].getField("TIMES").get(null);
                Object chatSubtitle = ((Class)Objects.requireNonNull((T)getNMS("IChatBaseComponent"))).getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, new Object[] { "{\"text\": \"" + subtitle + "\"}" });
                Constructor<?> subtitleConstructor = ((Class)Objects.<Class<?>>requireNonNull(getNMS("PacketPlayOutTitle"))).getConstructor(new Class[] { ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0], getNMS("IChatBaseComponent"), int.class, int.class, int.class });
                Object subtitlepacket = subtitleConstructor.newInstance(new Object[] { e2, chatSubtitle, Integer.valueOf(fadein), Integer.valueOf(stay), Integer.valueOf(fadeout) });
                sendPacket(player, subtitlepacket);
                e2 = ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0].getField("SUBTITLE").get(null);
                chatSubtitle = ((Class)Objects.requireNonNull((T)getNMS("IChatBaseComponent"))).getDeclaredClasses()[0].getMethod("a", new Class[] { String.class }).invoke(null, new Object[] { "{\"text\": \"" + subtitle + "\"}" });
                subtitleConstructor = ((Class)Objects.<Class<?>>requireNonNull(getNMS("PacketPlayOutTitle"))).getConstructor(new Class[] { ((Class)Objects.requireNonNull((T)getNMS("PacketPlayOutTitle"))).getDeclaredClasses()[0], getNMS("IChatBaseComponent"), int.class, int.class, int.class });
                subtitlepacket = subtitleConstructor.newInstance(new Object[] { e2, chatSubtitle, Integer.valueOf(fadein), Integer.valueOf(stay), Integer.valueOf(fadeout) });
                sendPacket(player, subtitlepacket);
            }
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

