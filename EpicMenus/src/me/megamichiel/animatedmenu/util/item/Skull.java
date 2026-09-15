package me.megamichiel.animatedmenu.util.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.megamichiel.animationlib.Nagger;
import me.megamichiel.animationlib.bukkit.AnimLibPlugin;
import me.megamichiel.animationlib.placeholder.PlaceholderContext;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Skull implements MaterialSpecific.Action<SkullMeta> {

    private static final char[] USERNAME_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_".toCharArray();

    private static final Map<String, PlayerProfile> cachedProfiles = new ConcurrentHashMap<>();

    private static final Material SKULL = skullMaterial();

    public static Skull forName(String name) {
        Skull skull = new Skull(null, name);

        if (!cachedProfiles.containsKey(name)) {
            loadProfile(name, name);
        }

        return skull;
    }

    private final StringBundle name;
    
    public Skull(Nagger nagger, Object name) {
        this.name = StringBundle.parse(nagger, name.toString());
    }

    public Stream<?> stream() {
        return name.stream();
    }

    @Override
    public void apply(Player player, SkullMeta meta, PlaceholderContext context) {
        String name = this.name.toString(player, context);
        PlayerProfile profile = cachedProfiles.get(name);
        if ((profile == null ? (profile = loadProfile(name, name)) : profile) != null) {
            meta.setOwnerProfile(profile);
        }
    }

    public void apply(Player player, Map<String, Object> map, PlaceholderContext context) {
        String name = this.name.toString(player, context);
        PlayerProfile profile = cachedProfiles.get(name);
        if ((profile == null ? (profile = loadProfile(name, name)) : profile) != null && profile.getName() != null) {
            map.put("SkullOwner", profile.getName());
        }
    }

    public ItemStack toItemStack(Player player, int amount, Consumer<ItemMeta> meta, PlaceholderContext context) {
        ItemStack item = new ItemStack(SKULL, amount, AnimLibPlugin.IS_LEGACY ? (short) 3 : 0);
        ItemMeta im = item.getItemMeta();
        apply(player, (SkullMeta) im, context);
        meta.accept(im);
        item.setItemMeta(im);
        return item;
    }

    public ItemStack toItemStack(Player player, Consumer<ItemMeta> meta, PlaceholderContext context) {
        return toItemStack(player, 1, meta, context);
    }
    
    private static PlayerProfile loadProfile(String savedName, String name) {
        PlayerProfile profile = null;
        if (name.startsWith("hdb:")) {
            String id = name.substring(4).trim();
            try {
                ItemStack item = new HeadDatabaseAPI().getItemHead(id);
                if (item != null && item.getItemMeta() instanceof SkullMeta) {
                    profile = ((SkullMeta) item.getItemMeta()).getOwnerProfile();
                }
            } catch (NoClassDefFoundError err) {
                // No head database ;c
            }
            if (profile == null) {
                profile = dummyProfile();
            }
            cachedProfiles.put(savedName, profile);
            return profile;
        }
        int length = name.length();

        UUID uuid = null;
        switch (length) {
            case 32:
                try {
                    uuid = UUID.fromString(
                            name.substring(0, 8) + '-' +
                            name.substring(8, 12) + '-' +
                            name.substring(12, 16) + '-' +
                            name.substring(16, 20) + '-' +
                            name.substring(20, 32)
                    );
                } catch (IllegalArgumentException ex) {
                    // Not a UUID
                }
                break;
            case 36:
                try {
                    uuid = UUID.fromString(name);
                } catch (IllegalArgumentException ex) {
                    // Not a UUID
                }
        }
        if (uuid != null) {
            Player player = Bukkit.getPlayer(uuid);
            profile = player != null ? Bukkit.createPlayerProfile(player.getUniqueId(), player.getName())
                    : Bukkit.createPlayerProfile(uuid);
            cachedProfiles.put(savedName, profile);
            profile.update().thenAccept(updated -> cachedProfiles.put(savedName, updated));
            return profile;
        }
        if (length <= 16) {
            Player player = Bukkit.getPlayerExact(name);
            profile = player != null ? Bukkit.createPlayerProfile(player.getUniqueId(), player.getName())
                    : Bukkit.createPlayerProfile(name);
            cachedProfiles.put(savedName, profile);
            profile.update().thenAccept(updated -> cachedProfiles.put(savedName, updated));
            return profile;
        }

        try {
            profile = profileWithSkin(extractSkinUrl(name));
        } catch (Exception ex) {
            profile = dummyProfile();
        }
        cachedProfiles.put(savedName, profile);

        return profile;
    }

    private static Material skullMaterial() {
        if (AnimLibPlugin.IS_LEGACY) {
            return Material.valueOf("SKULL_ITEM");
        }
        Material material = Material.matchMaterial("PLAYER_HEAD");
        return material == null ? Material.SKELETON_SKULL : material;
    }

    private static PlayerProfile profileWithSkin(URL url) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), randomName());
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(url);
        profile.setTextures(textures);
        return profile;
    }

    private static URL extractSkinUrl(String value) throws Exception {
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return new URL(object.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString());
        } catch (IllegalArgumentException ex) {
            return new URL(value);
        }
    }

    private static PlayerProfile dummyProfile() {
        return Bukkit.createPlayerProfile(UUID.randomUUID(), "Dummy");
    }

    private static String randomName() {
        char[] chars = USERNAME_CHARS, username = new char[16];
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 16; ++i) {
            username[i] = chars[random.nextInt(chars.length)];
        }
        return new String(username);
    }
}
