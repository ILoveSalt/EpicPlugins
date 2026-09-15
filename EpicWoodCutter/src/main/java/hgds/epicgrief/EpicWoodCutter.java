package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EpicWoodCutter extends JavaPlugin {

    private WoodConfigService configService;
    private WoodRepository repository;
    private EconomyService economyService;
    private RegionService regionService;
    private WoodScoreboardService scoreboardService;
    private BukkitTask saveTask;
    private final Map<UUID, Boolean> regionState = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configService = new WoodConfigService(this);
        repository = new WoodRepository(getLogger(), getDataFolder());
        economyService = new EconomyService(this);
        regionService = new RegionService(this);
        scoreboardService = new WoodScoreboardService(this);

        reloadPlugin();

        if (!economyService.hook()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new WoodListener(this), this);
        registerCommands();
        scoreboardService.start();
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, repository::save, 20L * 60L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }

        scoreboardService.stop();
        repository.save();
    }

    void reloadPlugin() {
        configService.reload();
        repository.load();
        regionService.loadRegions(configService.regions());
    }

    WoodConfigService config() {
        return configService;
    }

    WoodRepository repository() {
        return repository;
    }

    RegionService regions() {
        return regionService;
    }

    void handleBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        WoodRegionSettings region = regionService.findRegion(block.getLocation());

        if (region == null) {
            return;
        }

        if (!player.hasPermission("epicwoodcutter.use")) {
            return;
        }

        if (!TreeCutter.isWood(block.getType())) {
            event.setCancelled(true);
            send(player, "onlyWood", Map.of());
            return;
        }

        WoodGroupSettings group = configService.groups().resolve(player);
        WoodPlayerData data = repository.get(player.getUniqueId());

        if (data.getBackpack() >= group.maxBackpack()) {
            event.setCancelled(true);
            send(player, "full", Map.of());
            return;
        }

        long cooldownMillis = region.cooldownSeconds() * 1000L;
        long now = System.currentTimeMillis();
        if (cooldownMillis > 0 && now - data.getLastCutAt() < cooldownMillis) {
            event.setCancelled(true);
            return;
        }

        List<Block> treeBlocks = TreeCutter.collectTreeBlocks(block);
        if (treeBlocks.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        int remaining = treeBlocks.size() - 1;
        if (remaining > 0) {
            send(player, "cut", Map.of("number", Integer.toString(remaining)));
        }

        TreeCutter.breakTree(treeBlocks);

        double earn = region.earn() * group.booster();
        data.addTree(earn);
        data.setLastCutAt(now);

        send(player, "breaked", Map.of("earn", configService.formatMoney(earn)));

        if (configService.rollRandomBonus()) {
            double bonus = configService.randomBonusCoins();
            economyService.deposit(player, bonus);
            send(player, "random-change", Map.of());
        }

        scoreboardService.refresh(player);
    }

    void paySalary(Player player) {
        WoodPlayerData data = repository.get(player.getUniqueId());
        if (data.getBackpack() <= 0 || data.getSalary() <= 0.0) {
            send(player, "salary-zero", Map.of());
            return;
        }

        double payout = data.getSalary();
        if (!economyService.deposit(player, payout)) {
            player.sendMessage(configService.color(configService.message("prefix") + "Не удалось выдать зарплату."));
            return;
        }

        data.resetJobProgress();
        send(player, "salary-gived", Map.of("salary", configService.formatMoney(payout)));
        scoreboardService.refresh(player);
    }

    void handleRegionChange(Player player, org.bukkit.Location location) {
        boolean inRegion = regionService.isInWoodRegion(location);
        Boolean previous = regionState.put(player.getUniqueId(), inRegion);

        if (Boolean.TRUE.equals(previous) == inRegion) {
            return;
        }

        if (inRegion) {
            scoreboardService.handleRegionEnter(player);
        } else {
            scoreboardService.handleRegionLeave(player);
        }
    }

    void handleQuit(Player player) {
        regionState.remove(player.getUniqueId());
        scoreboardService.handleRegionLeave(player);
        repository.save();
    }

    private void send(Player player, String key, Map<String, String> placeholders) {
        configService.send(player, key, placeholders);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("woodcutter");
        if (command == null) {
            getLogger().warning("Команда /woodcutter не найдена в plugin.yml");
            return;
        }

        WoodCommand executor = new WoodCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
