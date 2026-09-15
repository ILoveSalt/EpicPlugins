package hgds.epicgrief;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

final class PotionUseListener implements Listener {
    private final PotionManager potionManager;

    PotionUseListener(PotionManager potionManager) {
        this.potionManager = potionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        potionManager.fromItem(event.getItem()).ifPresent(potion ->
                applySpecialEffects(event.getPlayer(), potion, 1.0)
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        potionManager.fromItem(event.getPotion().getItem()).ifPresent(potion -> {
            for (LivingEntity entity : event.getAffectedEntities()) {
                applySpecialEffects(entity, potion, event.getIntensity(entity));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionLingering(LingeringPotionSplashEvent event) {
        potionManager.fromItem(event.getEntity().getItem()).ifPresent(potion ->
                potionManager.mark(event.getAreaEffectCloud(), potion)
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        potionManager.fromDataHolder(event.getEntity()).ifPresent(potion -> {
            for (LivingEntity entity : event.getAffectedEntities()) {
                applySpecialEffects(entity, potion, 1.0);
            }
        });
    }

    private void applySpecialEffects(LivingEntity entity, CustomPotion potion, double intensity) {
        for (CustomPotion.SpecialEffect effect : potion.specialEffects()) {
            if (effect.type() == CustomPotion.SpecialEffect.Type.FREEZING) {
                int freezeTicks = (int) Math.round(effect.durationTicks() * intensity);
                entity.setFreezeTicks(Math.max(entity.getFreezeTicks(), freezeTicks));
            }
        }
    }
}
