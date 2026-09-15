package hgds.epicgrief.arrows;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ArrowListener implements Listener {

    private static final int TICKS_PER_SECOND = 20;

    private final JavaPlugin plugin;
    private final ArrowConfigService configService;
    private final ArrowItemService itemService;
    private final MessageService messages;
    private final Map<UUID, UUID> trackedProjectiles = new HashMap<>();
    private BukkitTask homingTask;

    public ArrowListener(
            JavaPlugin plugin,
            ArrowConfigService configService,
            ArrowItemService itemService,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.itemService = itemService;
        this.messages = messages;
    }

    public void start() {
        homingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickProjectiles, 1L, 1L);
    }

    public void stop() {
        if (homingTask != null) {
            homingTask.cancel();
            homingTask = null;
        }

        trackedProjectiles.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }

        ItemStack consumable = event.getConsumable();
        ArrowDefinition arrow = itemService.matchItem(consumable);
        if (arrow == null) {
            return;
        }

        if (configService.isDisabledWorld(shooter.getWorld().getName())) {
            event.setCancelled(true);
            sendDisabledWorldMessage(shooter);
            return;
        }

        if (configService.isDisabledArrow(arrow)) {
            event.setCancelled(true);
            messages.sendConfigured(shooter, "messages.disable.usage");
            return;
        }

        if (event.getProjectile() instanceof AbstractArrow projectile) {
            itemService.tagProjectile(projectile, arrow);
            trackedProjectiles.put(projectile.getUniqueId(), arrow.uuid());
            spawnParticle(arrow, projectile.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow projectile)) {
            return;
        }

        ArrowDefinition arrow = itemService.matchProjectile(projectile);
        if (arrow == null) {
            return;
        }

        trackedProjectiles.remove(projectile.getUniqueId());

        if (configService.isDisabledWorld(projectile.getWorld().getName()) || configService.isDisabledArrow(arrow)) {
            projectile.remove();
            return;
        }

        Player shooter = projectile.getShooter() instanceof Player player ? player : null;
        Location hitLocation = projectile.getLocation();
        spawnParticle(arrow, hitLocation);

        switch (arrow.type()) {
            case AIM -> {
                // Homing happens while the arrow is flying.
            }
            case EXPLODE -> explode(arrow, hitLocation);
            case TELEPORT -> teleportShooter(arrow, shooter, event, hitLocation);
            case CLEAR -> clearEffects(event.getHitEntity());
            case MIX_ITEMS -> mixItems(event.getHitEntity());
            case CHANGE -> changePlaces(shooter, event.getHitEntity());
            case ICE -> freezeTarget(arrow, event.getHitEntity());
        }

        projectile.remove();
    }

    private void tickProjectiles() {
        trackedProjectiles.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof AbstractArrow projectile) || !projectile.isValid() || projectile.isDead() || projectile.isOnGround()) {
                return true;
            }

            ArrowDefinition arrow = configService.arrow(entry.getValue());
            if (arrow == null) {
                return true;
            }

            if (arrow.type() == ArrowType.AIM) {
                guideArrow(projectile, arrow);
            }

            spawnParticle(arrow, projectile.getLocation());
            return false;
        });
    }

    private void guideArrow(AbstractArrow projectile, ArrowDefinition arrow) {
        if (!(projectile.getShooter() instanceof Player shooter)) {
            return;
        }

        Player target = findTarget(projectile, shooter, arrow.aimRadius());
        if (target == null) {
            return;
        }

        Vector current = projectile.getVelocity();
        double speed = Math.max(0.7D, current.length());
        Vector desired = target.getEyeLocation().toVector()
                .subtract(projectile.getLocation().toVector())
                .normalize()
                .multiply(speed);

        projectile.setVelocity(current.multiply(0.65D).add(desired.multiply(0.35D)));
    }

    private Player findTarget(AbstractArrow projectile, Player shooter, double radius) {
        Location location = projectile.getLocation();
        double maxDistanceSquared = radius * radius;
        Player nearest = null;

        for (Player candidate : projectile.getWorld().getPlayers()) {
            if (candidate.equals(shooter)
                    || candidate.isDead()
                    || candidate.getGameMode() == GameMode.SPECTATOR
                    || configService.isDisabledWorld(candidate.getWorld().getName())) {
                continue;
            }

            double distanceSquared = candidate.getLocation().distanceSquared(location);
            if (distanceSquared > maxDistanceSquared) {
                continue;
            }

            nearest = candidate;
            maxDistanceSquared = distanceSquared;
        }

        return nearest;
    }

    private void explode(ArrowDefinition arrow, Location location) {
        ExplosionOptions options = arrow.explosion();
        location.getWorld().createExplosion(location, options.power(), options.setFire(), options.breakBlocks());
    }

    private void teleportShooter(ArrowDefinition arrow, Player shooter, ProjectileHitEvent event, Location hitLocation) {
        if (shooter == null || shooter.isDead()) {
            return;
        }

        Location destination = teleportDestination(event, hitLocation);
        Material blockedMaterial = arrow.teleport().blockedMaterial();
        if (blockedMaterial != null && destination.getBlock().getType() == blockedMaterial) {
            return;
        }

        destination.setYaw(shooter.getLocation().getYaw());
        destination.setPitch(shooter.getLocation().getPitch());

        Location safeDestination = safeDestination(destination);
        if (safeDestination == null) {
            return;
        }

        shooter.teleport(safeDestination);
        if (arrow.teleport().sound() != null) {
            shooter.getWorld().playSound(shooter.getLocation(), arrow.teleport().sound(), 1.0F, 1.0F);
        }
    }

    private Location teleportDestination(ProjectileHitEvent event, Location fallback) {
        Block hitBlock = event.getHitBlock();
        BlockFace face = event.getHitBlockFace();
        if (hitBlock == null || face == null) {
            return fallback.clone();
        }

        Block relative = hitBlock.getRelative(face);
        return relative.getLocation().add(0.5D, 0.0D, 0.5D);
    }

    private Location safeDestination(Location destination) {
        Location candidate = destination.clone();
        for (int i = 0; i <= 2; i++) {
            if (candidate.getBlock().isPassable() && candidate.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()) {
                return candidate;
            }

            candidate.add(0.0D, 1.0D, 0.0D);
        }

        return null;
    }

    private void clearEffects(Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }
    }

    private void mixItems(Entity entity) {
        if (!(entity instanceof Player target)) {
            return;
        }

        ItemStack mainHand = target.getInventory().getItemInMainHand();
        ItemStack offHand = target.getInventory().getItemInOffHand();
        target.getInventory().setItemInMainHand(offHand);
        target.getInventory().setItemInOffHand(mainHand);
        target.updateInventory();
    }

    private void changePlaces(Player shooter, Entity entity) {
        if (shooter == null || !(entity instanceof Player target) || shooter.equals(target)) {
            return;
        }

        Location shooterLocation = shooter.getLocation().clone();
        Location targetLocation = target.getLocation().clone();
        shooter.teleport(targetLocation);
        target.teleport(shooterLocation);
    }

    private void freezeTarget(ArrowDefinition arrow, Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        int ticks = arrow.ice().seconds() * TICKS_PER_SECOND;
        PotionEffectType slowness = PotionEffectType.getByName("SLOW");
        if (slowness == null) {
            slowness = PotionEffectType.getByName("SLOWNESS");
        }

        if (slowness != null) {
            target.addPotionEffect(new PotionEffect(slowness, ticks, arrow.ice().amplifier(), false, true, true));
        }

        target.setFreezeTicks(Math.max(target.getFreezeTicks(), ticks));
    }

    private void spawnParticle(ArrowDefinition arrow, Location location) {
        ParticleOptions particle = arrow.particle();
        if (particle == null || !particle.enabled() || particle.particle() == null || location.getWorld() == null) {
            return;
        }

        location.getWorld().spawnParticle(particle.particle(), location, 2, 0.05D, 0.05D, 0.05D, 0.0D);
    }

    private void sendDisabledWorldMessage(Player shooter) {
        String path = plugin.getConfig().getString("messages.disable_regions.usage", "").isBlank()
                ? "messages.disable.usage"
                : "messages.disable_regions.usage";
        messages.sendConfigured(shooter, path);
    }
}
