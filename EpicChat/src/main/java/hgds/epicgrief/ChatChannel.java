package hgds.epicgrief;

import org.bukkit.entity.Player;

public final class ChatChannel {
    private final String name;
    private final boolean command;
    private final String prefix;
    private final String format;
    private final String permission;
    private final double range;

    public ChatChannel(String name, boolean command, String prefix, String format, String permission, double range) {
        this.name = name;
        this.command = command;
        this.prefix = emptyToNull(prefix);
        this.format = format;
        this.permission = emptyToNull(permission);
        this.range = range;
    }

    public String getName() {
        return name;
    }

    public boolean isCommand() {
        return command;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFormat() {
        return format;
    }

    public String getPermission() {
        return permission;
    }

    public boolean canUse(Player player) {
        return permission == null || player.hasPermission(permission);
    }

    public boolean canReceive(Player sender, Player recipient) {
        if (permission != null && !recipient.hasPermission(permission)) {
            return false;
        }
        if (range < 0) {
            return true;
        }
        if (!sender.getWorld().equals(recipient.getWorld())) {
            return false;
        }
        return sender.getLocation().distanceSquared(recipient.getLocation()) <= range * range;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
