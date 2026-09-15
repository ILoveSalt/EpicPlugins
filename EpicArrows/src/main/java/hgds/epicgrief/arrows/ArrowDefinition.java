package hgds.epicgrief.arrows;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.UUID;

public record ArrowDefinition(
        String key,
        UUID uuid,
        String title,
        Material material,
        int amount,
        List<String> lore,
        Color color,
        List<String> flags,
        List<PotionEffect> effects,
        ArrowType type,
        ParticleOptions particle,
        double aimRadius,
        ExplosionOptions explosion,
        TeleportOptions teleport,
        IceOptions ice
) {
}
