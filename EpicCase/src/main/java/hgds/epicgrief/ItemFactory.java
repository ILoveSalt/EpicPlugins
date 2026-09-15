package hgds.epicgrief;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

final class ItemFactory {
    private final Consumer<String> debug;
    private final UnaryOperator<String> colorizer;

    ItemFactory(Consumer<String> debug, UnaryOperator<String> colorizer) {
        this.debug = debug;
        this.colorizer = colorizer;
    }

    ItemStack fromConfig(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        ItemStack stack = fromSpec(section.getString("item", "CHEST"));
        applyMeta(stack, section.getString("name", ""), section.getStringList("desc"));
        return stack;
    }

    ItemStack fromSpec(String itemSpec) {
        String materialName = "CHEST";
        if (itemSpec != null && !itemSpec.isBlank()) {
            materialName = itemSpec.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
        }
        if ("HEAD".equals(materialName)) {
            materialName = "PLAYER_HEAD";
        }
        Material material = Material.matchMaterial(materialName);
        ItemStack stack = new ItemStack(material == null ? Material.CHEST : material);
        if (stack.getType() == Material.PLAYER_HEAD) {
            applyHeadTexture(stack, itemSpec);
        }
        return stack;
    }

    void applyMeta(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        if (name != null) {
            meta.setDisplayName(colorizer.apply(name));
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(colorizer).toList());
        }
        stack.setItemMeta(meta);
    }

    private void applyHeadTexture(ItemStack stack, String itemSpec) {
        if (!(stack.getItemMeta() instanceof SkullMeta meta)) {
            return;
        }
        String encodedTexture = texturePayloadFromSpec(itemSpec);
        if (encodedTexture.isBlank()) {
            return;
        }
        try {
            String decodedTexture = new String(Base64.getDecoder().decode(encodedTexture), StandardCharsets.UTF_8);
            String skinUrl = skinUrlFromTextureJson(decodedTexture);
            if (skinUrl.isBlank()) {
                return;
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(
                    UUID.nameUUIDFromBytes(encodedTexture.getBytes(StandardCharsets.UTF_8))
            );
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            stack.setItemMeta(meta);
        } catch (Exception exception) {
            debug.accept("Failed to apply head texture from item spec.");
        }
    }

    private String texturePayloadFromSpec(String itemSpec) {
        if (itemSpec == null || itemSpec.isBlank()) {
            return "";
        }
        int fromIndex = itemSpec.toLowerCase(Locale.ROOT).indexOf("--from");
        if (fromIndex < 0) {
            return "";
        }
        String value = itemSpec.substring(fromIndex + "--from".length()).trim();
        int spaceIndex = value.indexOf(' ');
        return spaceIndex < 0 ? value : value.substring(0, spaceIndex);
    }

    private String skinUrlFromTextureJson(String textureJson) {
        JsonObject root = JsonParser.parseString(textureJson).getAsJsonObject();
        JsonObject textures = root.getAsJsonObject("textures");
        if (textures == null) {
            return "";
        }
        JsonObject skin = textures.getAsJsonObject("SKIN");
        return skin == null || !skin.has("url") ? "" : skin.get("url").getAsString();
    }
}
