package hgds.epicgrief.util.message;

import hgds.epicgrief.util.StringUtil;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

public final class Message {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final String chatContent;

    private final String titleContent;

    private final String actionbarContent;

    public Message(String chatContent, String titleContent, String actionbarContent) {
        this.chatContent = (chatContent != null) ? StringUtil.colorize(chatContent) : null;
        this.titleContent = (titleContent != null) ? StringUtil.colorize(titleContent) : null;
        this.actionbarContent = (actionbarContent != null) ? StringUtil.colorize(actionbarContent) : null;
    }

    public void send(Player player) {
        this.send(player, Map.of());
    }

    public void send(Player player, Map<String, String> args) {
        trySendChat(player, args);
        trySendTitle(player, args);
        trySendActionbar(player, args);
    }

    public Message format(Map<String, String> args) {
        return new Message(
                (this.chatContent != null) ? StringUtil.format(this.chatContent, args) : null,
                (this.titleContent != null) ? StringUtil.format(this.titleContent, args) : null,
                (this.actionbarContent != null) ? StringUtil.format(this.actionbarContent, args) : null);
    }

    private void trySendChat(Player player, Map<String, String> args) {
        if (this.chatContent != null) {
            player.sendMessage(LEGACY.deserialize(StringUtil.format(this.chatContent, args)));
        }
    }

    private void trySendTitle(Player player, Map<String, String> args) {
        if (this.titleContent != null) {
            Component title = LEGACY.deserialize(StringUtil.format(this.titleContent, args));
            player.showTitle(Title.title(title, Component.empty()));
        }
    }

    private void trySendActionbar(Player player, Map<String, String> args) {
        if (this.actionbarContent != null) {
            player.sendActionBar(LEGACY.deserialize(StringUtil.format(this.actionbarContent, args)));
        }
    }
}