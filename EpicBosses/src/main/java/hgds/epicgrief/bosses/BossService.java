package hgds.epicgrief.bosses;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public final class BossService implements Listener {

    private static final double ABILITY_RADIUS = 12.0D;
    private static final String BOSS_TAG = "epicbosses.boss";
    private static final String MINION_TAG = "epicbosses.minion";

    private final JavaPlugin plugin;
    private final BossConfigService configService;
    private final MessageService messages;
    private final NamespacedKey bossIdKey;
    private final NamespacedKey minionKey;
    private final Random random = new Random();
    private final List<BossBar> endBossBars = new ArrayList<>();

    private BukkitTask tickTask;
    private ActiveBoss activeBoss;
    private int autoSpawnCountdown;

    public BossService(JavaPlugin plugin, BossConfigService configService, MessageService messages) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
        this.minionKey = new NamespacedKey(plugin, "minion");
    }

    public void start() {
        cleanupMarkedEntities();
        autoSpawnCountdown = configService.spawnIntervalSeconds();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        for (BossBar bossBar : endBossBars) {
            bossBar.removeAll();
        }
        endBossBars.clear();

        if (activeBoss != null) {
            activeBoss.bossBar.removeAll();
            if (activeBoss.entity.isValid()) {
                activeBoss.entity.remove();
            }
            activeBoss = null;
        }

        removeMinions();
    }

    public void reload() {
        configService.reload();
        autoSpawnCountdown = configService.spawnIntervalSeconds();
        if (activeBoss != null) {
            updateBossDisplay(activeBoss);
        }
    }

    public SpawnResult spawn(String id) {
        BossDefinition definition = configService.boss(id);
        if (definition == null) {
            return SpawnResult.NOT_FOUND;
        }

        if (isActiveAlive()) {
            return SpawnResult.ALREADY_ACTIVE;
        }

        return spawn(definition) ? SpawnResult.SPAWNED : SpawnResult.FAILED;
    }

    public boolean despawnActive() {
        if (activeBoss == null) {
            return false;
        }

        activeBoss.bossBar.removeAll();
        if (activeBoss.entity.isValid()) {
            activeBoss.entity.remove();
        }
        activeBoss = null;
        removeMinions();
        autoSpawnCountdown = configService.spawnIntervalSeconds();
        return true;
    }

    public BossSnapshot snapshot() {
        if (!isActiveAlive()) {
            return null;
        }

        BossDefinition definition = activeBoss.definition;
        LivingEntity entity = activeBoss.entity;
        return new BossSnapshot(
                definition.id(),
                messages.color(applyPlaceholders(definition.name(), definition, entity, null)),
                definition.type().name(),
                formatNumber(Math.max(0.0D, entity.getHealth())),
                formatNumber(definition.maxHealth())
        );
    }

    public void giveRewards(BossDefinition definition, Player player) {
        runCommands(definition.rewardCommands(), placeholders(definition, player, null));
        giveItemRewards(definition, player.getLocation());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (activeBoss != null) {
            activeBoss.bossBar.addPlayer(event.getPlayer());
        }

        for (BossBar bossBar : endBossBars) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = resolveDamager(event.getDamager());
        if (damager != null && isActiveBoss(damager)) {
            applyBossAttack(event);
        }

        if (isActiveBoss(event.getEntity())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (activeBoss != null) {
                    updateBossDisplay(activeBoss);
                    applyStage(activeBoss);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isActiveBoss(event.getEntity())) {
            return;
        }

        BossDefinition definition = activeBoss.definition;
        event.getDrops().clear();
        giveItemRewards(definition, event.getEntity().getLocation());

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            runCommands(definition.rewardCommands(), placeholders(definition, killer, event.getEntity()));
        }

        finishActiveBoss();
    }

    private void tick() {
        if (activeBoss == null) {
            tickAutoSpawn();
            return;
        }

        if (!isActiveAlive()) {
            activeBoss.bossBar.removeAll();
            activeBoss = null;
            removeMinions();
            autoSpawnCountdown = configService.spawnIntervalSeconds();
            return;
        }

        if (activeBoss.lifeTimeSeconds > 0) {
            activeBoss.lifeTimeSeconds--;
            if (activeBoss.lifeTimeSeconds <= 0) {
                despawnBossByTimeout();
                return;
            }
        }

        updateBossDisplay(activeBoss);
        applyStage(activeBoss);
        tickMinions(activeBoss);
        tickAbilities(activeBoss);
    }

    private void tickAutoSpawn() {
        if (configService.bosses().isEmpty()) {
            return;
        }

        autoSpawnCountdown--;
        if (autoSpawnCountdown > 0) {
            return;
        }

        List<BossDefinition> definitions = new ArrayList<>(configService.bosses().values());
        BossDefinition definition = definitions.get(random.nextInt(definitions.size()));
        if (!spawn(definition)) {
            autoSpawnCountdown = Math.max(60, configService.spawnIntervalSeconds());
        }
    }

    private boolean spawn(BossDefinition definition) {
        Location location = definition.location().toLocation();
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Failed to spawn boss " + definition.id() + ": world is not loaded.");
            return false;
        }

        Entity spawned = location.getWorld().spawnEntity(location, definition.type());
        if (!(spawned instanceof LivingEntity livingEntity)) {
            spawned.remove();
            return false;
        }

        livingEntity.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, definition.id());
        livingEntity.addScoreboardTag(BOSS_TAG);
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setPersistent(true);
        setAttribute(livingEntity, definition.maxHealth(), "MAX_HEALTH", "GENERIC_MAX_HEALTH");
        livingEntity.setHealth(Math.min(definition.maxHealth(), maxHealth(livingEntity)));
        livingEntity.setCustomNameVisible(true);

        BossBarSettings settings = configService.bossBarSettings();
        BossBar bossBar = Bukkit.createBossBar(
                messages.color(applyPlaceholders(settings.title(), definition, livingEntity, null)),
                settings.color(),
                settings.style()
        );
        bossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        ActiveBoss boss = new ActiveBoss(definition, livingEntity, bossBar);
        boss.minionCountdown = Math.max(1, definition.spawnMob().intervalSeconds());
        for (AbilityDefinition ability : definition.abilities()) {
            boss.abilityCountdowns.put(ability, randomDelay(ability));
        }
        activeBoss = boss;

        updateBossDisplay(boss);
        runCommands(definition.spawnCommands(), placeholders(definition, null, livingEntity));
        autoSpawnCountdown = configService.spawnIntervalSeconds();
        return true;
    }

    private void finishActiveBoss() {
        if (activeBoss == null) {
            return;
        }

        BossDefinition definition = activeBoss.definition;
        LivingEntity entity = activeBoss.entity;
        activeBoss.bossBar.removeAll();
        activeBoss = null;
        removeMinions();
        autoSpawnCountdown = configService.spawnIntervalSeconds();

        BossBarSettings settings = configService.endBossBarSettings();
        if (!settings.title().isBlank()) {
            BossBar endBossBar = Bukkit.createBossBar(
                    messages.color(applyPlaceholders(settings.title(), definition, entity, null)),
                    settings.color(),
                    settings.style()
            );
            endBossBar.setProgress(1.0D);
            endBossBar.setVisible(true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                endBossBar.addPlayer(player);
            }
            endBossBars.add(endBossBar);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                endBossBar.removeAll();
                endBossBars.remove(endBossBar);
            }, settings.timeToDeleteSeconds() * 20L);
        }
    }

    private void despawnBossByTimeout() {
        if (activeBoss == null) {
            return;
        }

        BossDefinition definition = activeBoss.definition;
        LivingEntity entity = activeBoss.entity;

        activeBoss.bossBar.removeAll();
        if (entity.isValid()) {
            entity.remove();
        }
        activeBoss = null;
        removeMinions();
        autoSpawnCountdown = configService.spawnIntervalSeconds();

        runCommands(definition.timeoutCommands(), placeholders(definition, null, entity));
    }

    private void updateBossDisplay(ActiveBoss boss) {
        LivingEntity entity = boss.entity;
        BossDefinition definition = boss.definition;
        String bossName = messages.color(applyPlaceholders(definition.name(), definition, entity, null));
        entity.setCustomName(bossName);

        BossBarSettings settings = configService.bossBarSettings();
        boss.bossBar.setTitle(messages.color(applyPlaceholders(settings.title(), definition, entity, null)));
        boss.bossBar.setColor(settings.color());
        boss.bossBar.setStyle(settings.style());
        boss.bossBar.setProgress(progress(entity, definition.maxHealth()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            boss.bossBar.addPlayer(player);
        }
    }

    private void applyStage(ActiveBoss boss) {
        double percent = progress(boss.entity, boss.definition.maxHealth()) * 100.0D;
        BossDefinition.StageMatch stage = boss.definition.stageFor(percent);
        Integer threshold = stage == null ? null : stage.threshold();
        if (Objects.equals(threshold, boss.appliedStage)) {
            return;
        }

        boss.appliedStage = threshold;
        if (stage == null) {
            return;
        }

        StageOptions options = stage.options();
        if (options.speed() >= 0.0D) {
            setAttribute(boss.entity, options.speed(), "MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
        }
        if (options.damage() >= 0.0D) {
            setAttribute(boss.entity, options.damage(), "ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
        }
    }

    private void tickMinions(ActiveBoss boss) {
        SpawnMobOptions spawnMob = boss.definition.spawnMob();
        if (spawnMob.intervalSeconds() <= 0 || spawnMob.minions().isEmpty()) {
            return;
        }

        boss.minionCountdown--;
        if (boss.minionCountdown > 0) {
            return;
        }

        for (MinionDefinition minion : spawnMob.minions()) {
            if (roll(minion.chance())) {
                spawnMinion(boss.entity.getLocation(), minion);
            }
        }

        boss.minionCountdown = Math.max(1, spawnMob.intervalSeconds());
    }

    private void spawnMinion(Location base, MinionDefinition definition) {
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 2.0D + random.nextDouble() * 3.0D;
        Location location = base.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
        Entity spawned = world.spawnEntity(location, definition.type());
        if (!(spawned instanceof LivingEntity livingEntity)) {
            spawned.remove();
            return;
        }

        livingEntity.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);
        livingEntity.addScoreboardTag(MINION_TAG);
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setPersistent(false);
        if (definition.health() > 0.0D) {
            setAttribute(livingEntity, definition.health(), "MAX_HEALTH", "GENERIC_MAX_HEALTH");
            livingEntity.setHealth(Math.min(definition.health(), maxHealth(livingEntity)));
        }
    }

    private void tickAbilities(ActiveBoss boss) {
        for (AbilityDefinition ability : boss.definition.abilities()) {
            int countdown = boss.abilityCountdowns.getOrDefault(ability, randomDelay(ability)) - 1;
            if (countdown <= 0) {
                activateAbility(boss, ability);
                countdown = randomDelay(ability);
            }
            boss.abilityCountdowns.put(ability, countdown);
        }
    }

    private void activateAbility(ActiveBoss boss, AbilityDefinition ability) {
        switch (ability.type().toUpperCase(Locale.ROOT)) {
            case "FREEZE" -> freezeNearbyPlayers(boss.entity);
            case "JUMP", "THROW" -> throwNearbyPlayers(boss.entity);
            default -> plugin.getLogger().warning("Unknown boss effect type: " + ability.type());
        }
    }

    private void freezeNearbyPlayers(LivingEntity boss) {
        FreezeSettings settings = configService.freezeSettings();
        for (Player player : nearbyPlayers(boss, ABILITY_RADIUS)) {
            PotionSpec spec = settings.potion();
            PotionEffectType type = PotionEffectType.getByName(spec.type());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, spec.durationSeconds() * 20, spec.amplifier()));
            }
            setFreezeTicks(player, settings.timeSeconds() * 20);
            sendTitle(player, settings.title());
        }
    }

    private void throwNearbyPlayers(LivingEntity boss) {
        ThrowSettings settings = configService.throwSettings();
        for (Player player : nearbyPlayers(boss, ABILITY_RADIUS)) {
            Vector direction = player.getLocation().toVector().subtract(boss.getLocation().toVector());
            if (direction.lengthSquared() <= 0.001D) {
                direction = new Vector(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
            }

            direction.setY(0.0D).normalize();
            double strength = randomBetween(settings.min(), settings.max()) / 10.0D;
            player.setVelocity(direction.multiply(strength).setY(0.8D + strength * 0.35D));
            sendTitle(player, settings.title());
        }
    }

    private void applyBossAttack(EntityDamageByEntityEvent event) {
        BossDefinition definition = activeBoss.definition;
        DamageOptions damage = definition.damage();
        if (roll(damage.chance())) {
            event.setDamage(event.getDamage() * damage.multiply());
        }

        PushOptions push = definition.push();
        if (roll(push.chance())) {
            Vector velocity = new Vector(push.x(), push.y(), push.z());
            if (velocity.lengthSquared() > 0.0D) {
                event.getEntity().setVelocity(velocity);
            }
        }
    }

    private List<Player> nearbyPlayers(LivingEntity boss, double radius) {
        List<Player> players = new ArrayList<>();
        for (Entity entity : boss.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player && !player.isDead()) {
                players.add(player);
            }
        }
        return players;
    }

    private void giveItemRewards(BossDefinition definition, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        for (ItemReward reward : definition.itemRewards()) {
            if (roll(reward.chance())) {
                world.dropItemNaturally(location, new ItemStack(reward.material(), reward.amount()));
            }
        }
    }

    private void runCommands(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }

            String prepared = replace(command, placeholders).trim();
            if (prepared.regionMatches(true, 0, "broadcast:", 0, "broadcast:".length())) {
                String message = prepared.substring("broadcast:".length()).trim();
                Bukkit.broadcastMessage(messages.color(message));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
            }
        }
    }

    private Map<String, String> placeholders(BossDefinition definition, Player player, LivingEntity entity) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{id}", definition.id());
        placeholders.put("{name}", messages.color(definition.name()));
        placeholders.put("{type}", definition.type().name());
        placeholders.put("{maxHealth}", formatNumber(definition.maxHealth()));
        if (entity != null) {
            placeholders.put("{health}", formatNumber(Math.max(0.0D, entity.getHealth())));
        } else {
            placeholders.put("{health}", formatNumber(definition.maxHealth()));
        }
        if (player != null) {
            placeholders.put("{player}", player.getName());
        }
        return placeholders;
    }

    private String applyPlaceholders(String text, BossDefinition definition, LivingEntity entity, Player player) {
        return replace(text, placeholders(definition, player, entity));
    }

    private String replace(String value, Map<String, String> placeholders) {
        String result = value == null ? "" : value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void cleanupMarkedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(BOSS_TAG)
                        || entity.getScoreboardTags().contains(MINION_TAG)
                        || entity.getPersistentDataContainer().has(bossIdKey, PersistentDataType.STRING)
                        || entity.getPersistentDataContainer().has(minionKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeMinions() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(MINION_TAG)
                        || entity.getPersistentDataContainer().has(minionKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    private boolean isActiveAlive() {
        return activeBoss != null && activeBoss.entity.isValid() && !activeBoss.entity.isDead();
    }

    private boolean isActiveBoss(Entity entity) {
        return activeBoss != null && entity != null && entity.getUniqueId().equals(activeBoss.entity.getUniqueId());
    }

    private Entity resolveDamager(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity sourceEntity) {
                return sourceEntity;
            }
        }
        return damager;
    }

    private void setAttribute(LivingEntity entity, double value, String... names) {
        Attribute attribute = attribute(names);
        if (attribute == null) {
            return;
        }

        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private double maxHealth(LivingEntity entity) {
        Attribute attribute = attribute("MAX_HEALTH", "GENERIC_MAX_HEALTH");
        if (attribute == null) {
            return 20.0D;
        }

        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 20.0D : instance.getValue();
    }

    private Attribute attribute(String... names) {
        for (String name : names) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private double progress(LivingEntity entity, double maxHealth) {
        if (maxHealth <= 0.0D) {
            return 0.0D;
        }

        return Math.max(0.0D, Math.min(1.0D, entity.getHealth() / maxHealth));
    }

    private int randomDelay(AbilityDefinition ability) {
        if (ability.maxDelaySeconds() <= ability.minDelaySeconds()) {
            return ability.minDelaySeconds();
        }
        return ability.minDelaySeconds() + random.nextInt(ability.maxDelaySeconds() - ability.minDelaySeconds() + 1);
    }

    private boolean roll(double chance) {
        return chance >= 100.0D || (chance > 0.0D && random.nextDouble() * 100.0D <= chance);
    }

    private double randomBetween(double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextDouble() * (max - min);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.01D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void sendTitle(Player player, String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        player.sendTitle(messages.color(title), "", 10, 40, 10);
    }

    private void setFreezeTicks(Player player, int ticks) {
        try {
            Method method = player.getClass().getMethod("setFreezeTicks", int.class);
            method.invoke(player, ticks);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public enum SpawnResult {
        SPAWNED,
        NOT_FOUND,
        ALREADY_ACTIVE,
        FAILED
    }

    public record BossSnapshot(String id, String name, String type, String health, String maxHealth) {
    }

    private static final class ActiveBoss {

        private final BossDefinition definition;
        private final LivingEntity entity;
        private final BossBar bossBar;
        private final Map<AbilityDefinition, Integer> abilityCountdowns = new HashMap<>();
        private int minionCountdown;
        private Integer appliedStage;
        private int lifeTimeSeconds;

        private ActiveBoss(BossDefinition definition, LivingEntity entity, BossBar bossBar) {
            this.definition = definition;
            this.entity = entity;
            this.bossBar = bossBar;
            this.lifeTimeSeconds = definition.lifeTimeSeconds(); // 0 = таймер отключён
        }
    }
}
