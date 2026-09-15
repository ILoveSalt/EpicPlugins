package hgds.epicgrief.api.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Головы игроков без NMS-рефлексии: через Paper PlayerProfile API
 * (совместимо с новыми версиями Paper, где CraftMetaSkull.profile отсутствует).
 */
public final class Head {
    private Head() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final ItemStack DENY = getHeadByValue("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZkZTNiZmNlMmQ4Y2I3MjRkZTg1NTZlNWVjMjFiN2YxNWY1ODQ2ODRhYjc4NTIxNGFkZDE2NGJlNzYyNGIifX19");

    private static final ItemStack ALLOW = getHeadByValue("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTllNGJkY2YxNzJkNWRjNzdjMmJkNGUzN2FkOTg1Mzk5YTlmMmNkZWJmNzI0NjM5MjllYTRiNjY2ZWY2ZjgwIn19fQ==");

    private static final ItemStack SELECTED = getHeadByValue("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTJjZDI3MmVlYjM4YmY3ODNhOThhNDZmYTFlMmU4ZDQ2MmQ4NTJmYmFhZWRlZjBkY2UyYzFmNzE3YTJhIn19fQ==");

    public static ItemStack getDeny() {
        return DENY.clone();
    }

    public static ItemStack getAllow() {
        return ALLOW.clone();
    }

    public static ItemStack getSelected() {
        return SELECTED.clone();
    }

    public static ItemStack getPlayerHead() {
        return new ItemStack(Material.PLAYER_HEAD);
    }

    public static ItemStack getHeadByName(String playerName) {
        if (playerName == null || playerName.isEmpty())
            throw new IllegalArgumentException("Player name is null or empty");
        ItemStack itemStack = getPlayerHead();
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setPlayerProfile(Bukkit.createProfile(playerName));
        itemStack.setItemMeta(skullMeta);
        return itemStack;
    }

    public static ItemStack getHeadByTextures(String signature) {
        if (signature == null || signature.isEmpty())
            throw new IllegalArgumentException("Signature is null or empty");
        String texture = "http://textures.minecraft.net/texture/" + signature;
        String encoded = Base64.getEncoder().encodeToString(
                String.format("{textures:{SKIN:{url:\"%s\"}}}", texture).getBytes(StandardCharsets.UTF_8));
        return getHeadByValue(encoded);
    }

    public static ItemStack getHeadByValue(String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Texture value is null or empty");
        ItemStack head = getPlayerHead();
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        applyProfile(skullMeta, value);
        head.setItemMeta(skullMeta);
        return head;
    }

    private static void applyProfile(SkullMeta skullMeta, String textureValue) {
        PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(textureValue.getBytes(StandardCharsets.UTF_8)));
        profile.setProperty(new ProfileProperty("textures", textureValue));
        skullMeta.setPlayerProfile(profile);
    }
}
