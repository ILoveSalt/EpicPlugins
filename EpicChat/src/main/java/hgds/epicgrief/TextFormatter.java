package hgds.epicgrief;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class TextFormatter {
    private final EpicChat plugin;
    private Method placeholderApiMethod;
    private boolean placeholderApiChecked;
    private Class<?> vaultChatClass;
    private Object vaultChatProvider;
    private boolean vaultChecked;

    public TextFormatter(EpicChat plugin) {
        this.plugin = plugin;
    }

    public void refreshHooks() {
        placeholderApiMethod = null;
        placeholderApiChecked = false;
        vaultChatClass = null;
        vaultChatProvider = null;
        vaultChecked = false;
    }

    public String preparePlayerMessage(Player player, String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage;
        if (player.hasPermission("chat.color")) {
            return color(message);
        }
        return stripColors(message);
    }

    public String formatChannelMessage(ChatChannel channel, Player sender, String message) {
        Map<String, String> replacements = baseReplacements(sender);
        replacements.put("{sender}", sender.getName());
        replacements.put("{message}", message);
        replacements.put("{msg}", message);
        return format(sender, channel.getFormat(), replacements);
    }

    public String formatPrivateMessage(Player sender, Player target, String message) {
        Map<String, String> replacements = baseReplacements(sender);
        replacements.put("{sender}", sender.getName());
        replacements.put("{player}", target.getName());
        replacements.put("{to}", target.getName());
        replacements.put("{message}", message);
        replacements.put("{msg}", message);
        return format(sender, plugin.getSettings().getPrivateMessageFormat(), replacements);
    }

    public String formatSpyMessage(Player sender, Player target, String message) {
        Map<String, String> replacements = baseReplacements(sender);
        replacements.put("{sender}", sender.getName());
        replacements.put("{player}", target.getName());
        replacements.put("{to}", target.getName());
        replacements.put("{message}", message);
        replacements.put("{msg}", message);
        return format(sender, plugin.getSettings().getMessage("SPY"), replacements);
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(color(message)));
    }

    public String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String stripColors(String text) {
        String stripped = ChatColor.stripColor(color(text));
        if (stripped == null) {
            return "";
        }
        return stripped.replace("§", "");
    }

    private String format(Player context, String template, Map<String, String> replacements) {
        String result = template == null ? "" : template;
        result = applyPlaceholderApi(context, result);
        result = applyVaultPlaceholders(context, result);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return color(result);
    }

    private Map<String, String> baseReplacements(Player player) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{prefix}", vaultValue("getPlayerPrefix", player));
        replacements.put("{suffix}", vaultValue("getPlayerSuffix", player));
        return replacements;
    }

    private String applyPlaceholderApi(Player player, String text) {
        if (!ensurePlaceholderApi()) {
            return text;
        }
        try {
            Object value = placeholderApiMethod.invoke(null, player, text);
            return value == null ? text : String.valueOf(value);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI hook failed", exception);
            placeholderApiMethod = null;
            placeholderApiChecked = true;
            return text;
        }
    }

    private boolean ensurePlaceholderApi() {
        if (placeholderApiChecked) {
            return placeholderApiMethod != null;
        }
        placeholderApiChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }
        try {
            Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderApiMethod = placeholderApiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI was found, but could not be hooked", exception);
            return false;
        }
    }

    private String applyVaultPlaceholders(Player player, String text) {
        if (text.indexOf("%vault_") < 0) {
            return text;
        }
        return text
                .replace("%vault_prefix%", vaultValue("getPlayerPrefix", player))
                .replace("%vault_suffix%", vaultValue("getPlayerSuffix", player));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean ensureVaultChat() {
        if (vaultChecked) {
            return vaultChatProvider != null;
        }
        vaultChecked = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }
        try {
            vaultChatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) vaultChatClass);
            if (registration == null) {
                return false;
            }
            vaultChatProvider = registration.getProvider();
            return vaultChatProvider != null;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Vault was found, but chat service could not be hooked", exception);
            return false;
        }
    }

    private String vaultValue(String methodName, Player player) {
        if (!ensureVaultChat()) {
            return "";
        }

        try {
            Method method = vaultChatClass.getMethod(methodName, Player.class);
            Object value = method.invoke(vaultChatProvider, player);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method method = vaultChatClass.getMethod(methodName, String.class, String.class);
                Object value = method.invoke(vaultChatProvider, player.getWorld().getName(), player.getName());
                return value == null ? "" : String.valueOf(value);
            } catch (ReflectiveOperationException exception) {
                return "";
            }
        }
    }
}
