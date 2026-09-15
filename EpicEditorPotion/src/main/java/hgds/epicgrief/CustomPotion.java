package hgds.epicgrief;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import java.util.List;

record CustomPotion(
        String id,
        Material material,
        String displayName,
        List<String> lore,
        Color color,
        boolean glint,
        List<PotionEffect> effects,
        List<SpecialEffect> specialEffects
) {
    record SpecialEffect(Type type, int durationTicks) {
        enum Type {
            FREEZING
        }
    }
}
