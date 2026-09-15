package hgds.epicgrief;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class PotionManager {
    private static final int TICKS_PER_SECOND = 20;

    private final EpicEditorPotion plugin;
    private final NamespacedKey potionIdKey;
    private final Map<String, CustomPotion> potions = new LinkedHashMap<>();

    PotionManager(EpicEditorPotion plugin) {
        this.plugin = plugin;
        this.potionIdKey = new NamespacedKey(plugin, "potion_id");
    }

    void reload() {
        potions.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("potions");
        if (root == null) {
            plugin.getLogger().warning("В config.yml отсутствует раздел potions.");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                plugin.getLogger().warning("Пропущено зелье '" + id + "': настройки должны быть разделом.");
                continue;
            }

            try {
                CustomPotion potion = loadPotion(id, section);
                potions.put(potion.id(), potion);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Не удалось загрузить зелье '" + id + "': " + exception.getMessage());
            }
        }
    }

    int size() {
        return potions.size();
    }

    Collection<String> ids() {
        return List.copyOf(potions.keySet());
    }

    boolean contains(String id) {
        return id != null && potions.containsKey(id.toLowerCase(Locale.ROOT));
    }

    Optional<CustomPotion> fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        return fromDataHolder(item.getItemMeta());
    }

    Optional<CustomPotion> fromDataHolder(PersistentDataHolder holder) {
        String id = holder.getPersistentDataContainer().get(potionIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(id).map(potions::get);
    }

    void mark(PersistentDataHolder holder, CustomPotion potion) {
        holder.getPersistentDataContainer().set(
                potionIdKey,
                PersistentDataType.STRING,
                potion.id()
        );
    }

    ItemStack createItem(String id) {
        CustomPotion potion = potions.get(id.toLowerCase(Locale.ROOT));
        if (potion == null) {
            throw new IllegalArgumentException("Неизвестное зелье: " + id);
        }

        ItemStack item = new ItemStack(potion.material());
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof PotionMeta meta)) {
            throw new IllegalStateException("Материал " + potion.material() + " не поддерживает эффекты зелий.");
        }

        meta.setDisplayName(colorize(potion.displayName()));
        meta.setLore(potion.lore().stream().map(PotionManager::colorize).toList());
        meta.setColor(potion.color());
        meta.setEnchantmentGlintOverride(potion.glint());
        meta.getPersistentDataContainer().set(potionIdKey, PersistentDataType.STRING, potion.id());

        for (PotionEffect effect : potion.effects()) {
            meta.addCustomEffect(effect, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private CustomPotion loadPotion(String rawId, ConfigurationSection section) {
        String id = rawId.toLowerCase(Locale.ROOT);
        Material material = parseMaterial(section.getString("material", "SPLASH_POTION"));
        String displayName = section.getString("display_name", "&f" + rawId);
        List<String> lore = section.getStringList("lore");
        Color color = parseColor(section.getString("color", "#FFFFFF"));
        boolean glint = section.getBoolean("glint", true);

        List<PotionEffect> effects = new ArrayList<>();
        List<CustomPotion.SpecialEffect> specialEffects = new ArrayList<>();
        for (String entry : readEffectEntries(section)) {
            parseEffect(entry, effects, specialEffects);
        }

        return new CustomPotion(
                id,
                material,
                displayName,
                List.copyOf(lore),
                color,
                glint,
                List.copyOf(effects),
                List.copyOf(specialEffects)
        );
    }

    private Material parseMaterial(String value) {
        Material material = Material.matchMaterial(value);
        if (material == null || !List.of(
                Material.POTION,
                Material.SPLASH_POTION,
                Material.LINGERING_POTION
        ).contains(material)) {
            throw new IllegalArgumentException(
                    "material должен быть POTION, SPLASH_POTION или LINGERING_POTION."
            );
        }
        return material;
    }

    private Color parseColor(String value) {
        String normalized = value.trim().replace("#", "");
        try {
            if (normalized.matches("[0-9a-fA-F]{6}")) {
                return Color.fromRGB(Integer.parseInt(normalized, 16));
            }

            String[] rgb = normalized.split(",");
            if (rgb.length == 3) {
                return Color.fromRGB(
                        Integer.parseInt(rgb[0].trim()),
                        Integer.parseInt(rgb[1].trim()),
                        Integer.parseInt(rgb[2].trim())
                );
            }
        } catch (IllegalArgumentException ignored) {
            // The clear configuration error below is more useful than a parsing stack trace.
        }
        throw new IllegalArgumentException("color должен быть в формате #RRGGBB или R,G,B.");
    }

    private List<String> readEffectEntries(ConfigurationSection section) {
        List<String> entries = new ArrayList<>(section.getStringList("effects"));
        if (section.isList("effect")) {
            entries.addAll(section.getStringList("effect"));
        }

        ConfigurationSection legacy = section.getConfigurationSection("effect");
        if (legacy != null) {
            for (String effectName : legacy.getKeys(false)) {
                Object value = legacy.get(effectName);
                if (value == null && effectName.contains(":")) {
                    entries.add(effectName);
                } else if (value != null) {
                    entries.add(effectName + ":" + value);
                }
            }
        }
        return entries;
    }

    private void parseEffect(
            String entry,
            List<PotionEffect> effects,
            List<CustomPotion.SpecialEffect> specialEffects
    ) {
        String[] parts = entry.replace(" ", "").split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "эффект '" + entry + "' должен иметь формат ЭФФЕКТ:СЕКУНДЫ:УРОВЕНЬ."
            );
        }

        String name = parts[0].toLowerCase(Locale.ROOT);
        int durationSeconds = parsePositiveInt(parts[1], "время эффекта");
        int level = parsePositiveInt(parts[2], "уровень эффекта");
        int durationTicks = Math.multiplyExact(durationSeconds, TICKS_PER_SECOND);

        if (name.equals("freezing") || name.equals("freeze")) {
            specialEffects.add(new CustomPotion.SpecialEffect(
                    CustomPotion.SpecialEffect.Type.FREEZING,
                    durationTicks
            ));
            return;
        }

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(name));
        if (type == null) {
            throw new IllegalArgumentException("неизвестный эффект '" + name + "'.");
        }
        effects.add(new PotionEffect(type, durationTicks, level - 1, false, true, true));
    }

    private int parsePositiveInt(String value, String fieldName) {
        try {
            int number = Integer.parseInt(value);
            if (number < 1) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " должно быть целым числом больше нуля.");
        }
    }

    static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
