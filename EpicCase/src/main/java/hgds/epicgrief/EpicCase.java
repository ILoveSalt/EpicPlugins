package hgds.epicgrief;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class EpicCase extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String HOLOGRAM_TAG = "epiccase_hologram";
    private static final String ANIMATION_TAG = "epiccase_animation";

    private static final List<String> ROOT_COMMANDS = List.of(
            "open", "give", "take", "set", "givekey", "takekey", "setkey",
            "syncgive", "remove", "simulate", "forceopen", "generateResources", "points", "service", "reload", "list"
    );
    private static final List<String> POINT_COMMANDS = List.of(
            "create", "remove", "list", "setOpenByDefault", "setAvailable",
            "showOnlyAvailable", "setSelector", "setHologram", "setLine", "removeLine", "setHeight",
            "setEffector", "setParameter", "copy", "paste"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Random random = new Random();
    private final CaseCatalog catalog = new CaseCatalog();
    private final Map<String, CaseBox> boxes = catalog.boxes();
    private final Map<String, CaseKey> keys = catalog.keys();
    private final Map<UUID, PlayerCaseData> playerData = new HashMap<>();
    private final Map<String, CasePoint> pointsById = new LinkedHashMap<>();
    private final Map<String, CasePoint> pointsByBlock = new HashMap<>();
    private final Set<UUID> openingPlayers = new HashSet<>();
    private final Map<UUID, String> openingPointByPlayer = new HashMap<>();
    private final Map<String, ActivePointOpening> activePointOpenings = new HashMap<>();
    private final Map<String, List<Entity>> hologramEntities = new HashMap<>();
    private final Set<UUID> animationEntityIds = new HashSet<>();
    private final Map<UUID, CasePoint.Settings> pointClipboard = new HashMap<>();
    private final Map<UUID, Map<String, String>> holographicSelections = new HashMap<>();

    private File boxesFolder;
    private File keysFolder;
    private File pointsFolder;
    private File playerDataFolder;
    private YamlConfiguration messages;
    private YamlConfiguration inventoryConfig;
    private YamlConfiguration effectorConfig;
    private CaseDataStorage dataStorage;
    private ActionExecutor actionExecutor;
    private ItemFactory itemFactory;
    private HologramBridge hologramBridge;

    @Override
    public void onEnable() {
        boxesFolder = new File(getDataFolder(), "boxes");
        keysFolder = new File(getDataFolder(), "keys");
        pointsFolder = new File(getDataFolder(), "points");
        playerDataFolder = new File(getDataFolder(), "playerdata");
        actionExecutor = new ActionExecutor(this, this::debug);
        itemFactory = new ItemFactory(this::debug, this::color);
        hologramBridge = new HologramBridge(this, itemFactory, this::debug);

        prepareDataFolder();
        reloadRuntimeData();
        if (!setupDataStorage()) {
            return;
        }

        PluginCommand command = getCommand("ecase");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        } else {
            getLogger().warning("Command /ecase is not registered in plugin.yml.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Loaded " + boxes.size() + " cases, " + keys.size()
                + " keys and " + pointsById.size() + " case points.");
    }

    @Override
    public void onDisable() {
        saveCachedPlayerData();
        restoreAllOpeningPoints();
        removeSpawnedHolograms();
        removeSpawnedAnimationEntities();
        playerData.clear();
        openingPlayers.clear();
        openingPointByPlayer.clear();
        activePointOpenings.clear();
        pointClipboard.clear();
        holographicSelections.clear();
        closeDataStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "open" -> handleOpen(sender, args);
            case "openpoint" -> handleOpenPoint(sender, args);
            case "give", "syncgive" -> handleGenericBalanceCommand(sender, args, BalanceOperation.GIVE);
            case "take" -> handleGenericBalanceCommand(sender, args, BalanceOperation.TAKE);
            case "set" -> handleGenericBalanceCommand(sender, args, BalanceOperation.SET);
            case "givekey" -> handleBalanceCommand(sender, args, BalanceKind.KEY, BalanceOperation.GIVE);
            case "takekey" -> handleBalanceCommand(sender, args, BalanceKind.KEY, BalanceOperation.TAKE);
            case "setkey" -> handleBalanceCommand(sender, args, BalanceKind.KEY, BalanceOperation.SET);
            case "remove" -> handleRemove(sender, args);
            case "simulate" -> handleSimulate(sender, args);
            case "forceopen" -> handleForceOpen(sender, args);
            case "generateresources" -> handleGenerateResources(sender, args);
            case "points" -> handlePoints(sender, args);
            case "service" -> handleService(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> sendBoxList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return complete(args[0], ROOT_COMMANDS);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("points".equals(subCommand)) {
            if (args.length == 2) {
                return complete(args[1], POINT_COMMANDS);
            }
            if (args.length >= 3 && (args[1].equalsIgnoreCase("setOpenByDefault")
                    || args[1].equalsIgnoreCase("setAvailable"))) {
                return complete(args[args.length - 1], boxes.keySet());
            }
            if (args.length == 3 && "showOnlyAvailable".equalsIgnoreCase(args[1])) {
                return complete(args[2], List.of("true", "false"));
            }
            if (args.length == 3 && "setSelector".equalsIgnoreCase(args[1])) {
                return complete(args[2], List.of("INVENTORY", "BOOK", "HOLOGRAPHIC"));
            }
            if (args.length == 3 && "setHologram".equalsIgnoreCase(args[1])) {
                return complete(args[2], List.of("NORMAL", "INDIVIDUAL", "EXTERNAL"));
            }
            return Collections.emptyList();
        }

        if (List.of("open", "give", "syncgive", "take", "set").contains(subCommand)
                && args.length == 2) {
            List<String> units = new ArrayList<>(boxes.keySet());
            units.addAll(keys.keySet());
            return complete(args[1], units);
        }
        if (List.of("givekey", "takekey", "setkey").contains(subCommand) && args.length == 2) {
            return complete(args[1], keys.keySet());
        }
        if ("open".equals(subCommand) && args.length == 3) {
            CaseBox box = boxes.get(CasePoint.normalizeBoxId(args[1]));
            return box == null ? List.of() : complete(args[2], catalog.keysFor(box).stream().map(CaseKey::getId).toList());
        }

        if (List.of("give", "syncgive", "take", "set", "givekey", "takekey", "setkey", "remove").contains(subCommand)
                && args.length == 3) {
            return complete(args[2], onlinePlayerNames());
        }

        if ("simulate".equals(subCommand) && args.length == 2) {
            return complete(args[1], onlinePlayerNames());
        }

        if ("simulate".equals(subCommand) && args.length == 3) {
            return complete(args[2], boxes.keySet());
        }

        if ("forceopen".equals(subCommand) && args.length == 2) {
            return complete(args[1], onlinePlayerNames());
        }
        if ("forceopen".equals(subCommand) && args.length == 3) {
            return complete(args[2], boxes.keySet());
        }
        if ("generateresources".equals(subCommand) && args.length == 2) {
            return complete(args[1], List.of("all", "boxes", "keys", "points", "gui"));
        }

        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerCaseData data = getPlayerData(event.getPlayer());
        if (data != null) {
            savePlayerData(data);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerCaseData data = playerData.remove(event.getPlayer().getUniqueId());
        if (data != null) {
            savePlayerData(data);
        }
        endCaseOpening(event.getPlayer().getUniqueId());
        holographicSelections.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        for (CasePoint point : pointsById.values()) {
            if (point.getWorld().equals(event.getWorld().getName())
                    && !activePointOpenings.containsKey(point.blockKey())) {
                refreshPointHologram(point);
            }
        }
    }

    @EventHandler
    public void onPointClick(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
                || event.getClickedBlock() == null) {
            return;
        }

        CasePoint point = pointsByBlock.get(CasePoint.blockKey(event.getClickedBlock()));
        if (point == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!hasUsePermission(player)) {
            return;
        }

        if ("HOLOGRAPHIC".equalsIgnoreCase(point.getSelector())) {
            handleHolographicSelector(player, point, event.getAction());
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        String defaultBox = point.getOpenByDefault();
        if (!defaultBox.isBlank()) {
            CaseBox box = boxes.get(defaultBox);
            if (box == null) {
                sendMessage(player, "commands.open.invalidbox", "&cCase not found.", Map.of("%box%", defaultBox));
                return;
            }
            requestOpenCase(player, box, point);
            return;
        }

        if ("BOOK".equalsIgnoreCase(point.getSelector())) {
            openBookSelection(player, point);
        } else {
            openSelection(player, point);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof SelectionHolder)
                && !(holder instanceof KeySelectionHolder)
                && !(holder instanceof ConfirmationHolder)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= topInventory.getSize()) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder instanceof SelectionHolder selectionHolder) {
            String boxId = selectionHolder.boxSlots.get(event.getRawSlot());
            if (boxId == null) {
                return;
            }

            CaseBox box = boxes.get(boxId);
            if (box != null) {
                requestOpenCase(player, box, selectionHolder.point);
            }
            return;
        }

        if (holder instanceof KeySelectionHolder keySelectionHolder) {
            String keyId = keySelectionHolder.keySlots.get(event.getRawSlot());
            if (keyId == null) {
                return;
            }
            CaseBox box = boxes.get(keySelectionHolder.boxId);
            if (box != null) {
                requestOpenCase(player, box, keySelectionHolder.point, keyId);
            }
            return;
        }

        ConfirmationHolder confirmationHolder = (ConfirmationHolder) holder;
        if (confirmationHolder.noSlots.contains(event.getRawSlot())) {
            player.closeInventory();
            return;
        }

        if (confirmationHolder.yesSlots.contains(event.getRawSlot())) {
            CaseBox box = boxes.get(confirmationHolder.boxId);
            if (box != null) {
                openCase(player, box, confirmationHolder.point, confirmationHolder.keyId);
            }
        }
    }

    private void prepareDataFolder() {
        saveDefaultConfig();
        boxesFolder.mkdirs();
        keysFolder.mkdirs();
        pointsFolder.mkdirs();
        playerDataFolder.mkdirs();

        saveResourceIfMissing("message.yml");
        saveResourceIfMissing("invgui.yml");
        saveResourceIfMissing("bookgui.yml");
        saveResourceIfMissing("holographicselector.yml");
        saveResourceIfMissing("effectors.yml");
        copyBundledDirectory("boxes");
        copyBundledDirectory("keys");
        copyBundledDirectory("points");
    }

    private void reloadRuntimeData() {
        reloadConfig();
        removeSpawnedHolograms();
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
        inventoryConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "invgui.yml"));
        effectorConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "effectors.yml"));
        loadBoxes();
        loadPoints();
        spawnAllHolograms();
    }

    private boolean setupDataStorage() {
        saveCachedPlayerData();
        playerData.clear();
        closeDataStorage();

        String storageType = getConfig().getString("datastorage", "default").toLowerCase(Locale.ROOT);
        try {
            if (storageType.equals("sql") || storageType.equals("mysql") || storageType.equals("database")) {
                dataStorage = new SqlCaseDataStorage(this, gson);
                getLogger().info("Player case data storage: MySQL");
            } else {
                dataStorage = new JsonCaseDataStorage(playerDataFolder, gson);
                getLogger().info("Player case data storage: JSON");
            }
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize player data storage: " + storageType, exception);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            getPlayerData(player);
        }
        return true;
    }

    private void saveCachedPlayerData() {
        if (dataStorage == null) {
            return;
        }

        for (PlayerCaseData data : playerData.values()) {
            savePlayerData(data);
        }
    }

    private void closeDataStorage() {
        if (dataStorage == null) {
            return;
        }

        try {
            dataStorage.close();
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to close player data storage.", exception);
        } finally {
            dataStorage = null;
        }
    }

    private void loadBoxes() {
        catalog.reload(boxesFolder, keysFolder, getLogger());
    }

    private void loadPoints() {
        pointsById.clear();
        pointsByBlock.clear();

        File[] files = pointsFolder.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                CasePoint point = CasePoint.fromFile(file);
                pointsById.put(point.getId().toLowerCase(Locale.ROOT), point);
                pointsByBlock.put(point.blockKey(), point);
            } catch (RuntimeException exception) {
                getLogger().log(Level.WARNING, "Failed to load point file " + file.getName(), exception);
            }
        }
    }

    private void spawnAllHolograms() {
        for (CasePoint point : pointsById.values()) {
            refreshPointHologram(point);
        }
    }

    private void refreshPointHologram(CasePoint point) {
        removePointHologram(point);
        if (activePointOpenings.containsKey(point.blockKey())) {
            return;
        }
        spawnPointHologram(point);
    }

    private void spawnPointHologram(CasePoint point) {
        Location base = pointHologramLocation(point);
        if (base == null) {
            debug("Cannot spawn hologram for " + point.getId() + ": world is not loaded.");
            return;
        }
        if ("external".equalsIgnoreCase(point.getHologramType()) && hologramBridge.spawn(point, base)) {
            return;
        }

        removeTaggedPointHolograms(point, base);
        List<String> lines = point.getHologramLines();
        if (lines.isEmpty()) {
            return;
        }

        List<Entity> spawned = new ArrayList<>();
        double offset = 0.0D;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            if (line.isBlank()) {
                offset += 0.28D;
                continue;
            }

            Location lineLocation = base.clone().subtract(0.0D, offset, 0.0D);
            if (isItemHologramLine(line)) {
                spawned.add(spawnHologramItem(point, lineLocation, stripHologramPrefix(line)));
                offset += 0.48D;
            } else {
                spawned.add(spawnHologramText(point, lineLocation, stripHologramPrefix(line)));
                offset += 0.28D;
            }
        }

        if (!spawned.isEmpty()) {
            hologramEntities.put(point.getId().toLowerCase(Locale.ROOT), spawned);
        }
    }

    private Location pointHologramLocation(CasePoint point) {
        World world = Bukkit.getWorld(point.getWorld());
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                point.getX() + 0.5D,
                point.getY() + 1.0D + point.getHologramHeight(),
                point.getZ() + 0.5D
        );
    }

    private ArmorStand spawnHologramText(CasePoint point, Location location, String text) {
        ArmorStand stand = spawnManagedArmorStand(location, point.getHologramDirection(), HOLOGRAM_TAG, pointTag(point));
        stand.setCustomName(color(text));
        stand.setCustomNameVisible(true);
        return stand;
    }

    private ArmorStand spawnHologramItem(CasePoint point, Location location, String itemSpec) {
        ArmorStand stand = spawnManagedArmorStand(location, point.getHologramDirection(), HOLOGRAM_TAG, pointTag(point));
        stand.getEquipment().setHelmet(createItemFromSpec(itemSpec));
        return stand;
    }

    private ArmorStand spawnManagedArmorStand(Location location, String direction, String... tags) {
        location.setYaw(directionYaw(direction));
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setCanPickupItems(false);
        for (String tag : tags) {
            stand.addScoreboardTag(tag);
        }
        return stand;
    }



    private float directionYaw(String direction) {
        if (direction == null) {
            return 180.0F;
        }
        return switch (direction.toUpperCase(Locale.ROOT)) {
            case "SOUTH" -> 0.0F;
            case "WEST" -> 90.0F;
            case "EAST" -> -90.0F;
            default -> 180.0F; // NORTH
        };
    }

    private boolean isItemHologramLine(String line) {
        String lowerLine = line.toLowerCase(Locale.ROOT);
        return lowerLine.startsWith("i:") || lowerLine.startsWith("item:");
    }

    private String stripHologramPrefix(String line) {
        String trimmed = line == null ? "" : line.trim();
        String lowerLine = trimmed.toLowerCase(Locale.ROOT);
        if (lowerLine.startsWith("t:") || lowerLine.startsWith("i:")) {
            return trimmed.substring(2).trim();
        }
        if (lowerLine.startsWith("text:")) {
            return trimmed.substring("text:".length()).trim();
        }
        if (lowerLine.startsWith("item:")) {
            return trimmed.substring("item:".length()).trim();
        }
        return trimmed;
    }

    private void removeSpawnedHolograms() {
        if (hologramBridge != null) {
            hologramBridge.removeAll();
        }
        for (List<Entity> entities : hologramEntities.values()) {
            removeEntities(entities);
        }
        hologramEntities.clear();
    }

    private void removePointHologram(CasePoint point) {
        if (hologramBridge != null) {
            hologramBridge.remove(point);
        }
        List<Entity> entities = hologramEntities.remove(point.getId().toLowerCase(Locale.ROOT));
        if (entities != null) {
            removeEntities(entities);
        }

        Location base = pointHologramLocation(point);
        if (base != null) {
            removeTaggedPointHolograms(point, base);
        }
    }

    private void removeTaggedPointHolograms(CasePoint point, Location base) {
        String pointTag = pointTag(point);
        double yRadius = Math.max(2.0D, point.getHologramHeight() + 2.0D);
        for (Entity entity : base.getWorld().getNearbyEntities(base, 2.0D, yRadius, 2.0D)) {
            if (entity.getScoreboardTags().contains(HOLOGRAM_TAG)
                    && entity.getScoreboardTags().contains(pointTag)) {
                entity.remove();
            }
        }
    }

    private String pointTag(CasePoint point) {
        return "epiccase_point_" + safeId(point.getId());
    }

    private void saveResourceIfMissing(String resourceName) {
        File target = new File(getDataFolder(), resourceName);
        if (!target.exists()) {
            saveResource(resourceName, false);
        }
    }

    private void copyBundledDirectory(String resourceFolder) {
        URL resourceUrl = getClassLoader().getResource(resourceFolder);
        if (resourceUrl == null) {
            return;
        }

        try {
            if ("jar".equals(resourceUrl.getProtocol())) {
                copyBundledDirectoryFromJar(resourceFolder, resourceUrl);
            } else if ("file".equals(resourceUrl.getProtocol())) {
                copyBundledDirectoryFromFile(resourceFolder, resourceUrl.toURI());
            }
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Failed to copy default " + resourceFolder + " resources.", exception);
        }
    }

    private void copyBundledDirectoryFromJar(String resourceFolder, URL resourceUrl) throws IOException {
        String path = resourceUrl.getPath();
        int separator = path.indexOf('!');
        if (separator < 0) {
            return;
        }

        String jarPath = path.substring("file:".length(), separator);
        try (JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
            String prefix = resourceFolder + "/";
            for (JarEntry entry : Collections.list(jarFile.entries())) {
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                    continue;
                }

                File target = new File(getDataFolder(), entry.getName());
                if (!target.exists()) {
                    saveResource(entry.getName(), false);
                }
            }
        }
    }

    private void copyBundledDirectoryFromFile(String resourceFolder, URI resourceUri) throws IOException {
        Path sourceRoot = Path.of(resourceUri);
        Path targetRoot = new File(getDataFolder(), resourceFolder).toPath();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path source : paths.filter(Files::isRegularFile).toList()) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative);
                if (Files.notExists(target)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "commands.onlyfromplayer", "&cOnly players can use this command.", Map.of());
            return;
        }

        if (!hasUsePermission(player)) {
            return;
        }

        if (args.length == 1) {
            openSelection(player, null);
            return;
        }

        String boxId = CasePoint.normalizeBoxId(args[1]);
        CaseBox box = boxes.get(boxId);
        if (box == null) {
            sendMessage(player, "commands.open.invalidbox", "&cCase not found.", Map.of("%box%", boxId));
            return;
        }

        String keyId = args.length >= 3 ? CasePoint.normalizeBoxId(args[2]) : null;
        requestOpenCase(player, box, null, keyId);
    }

    private void handleOpenPoint(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 3) {
            return;
        }
        CasePoint point = pointsById.get(CasePoint.normalizeBoxId(args[1]));
        CaseBox box = boxes.get(CasePoint.normalizeBoxId(args[2]));
        if (point == null || box == null) {
            sendMessage(player, "commands.open.invalidbox", "&cCase or point not found.", Map.of());
            return;
        }
        World world = Bukkit.getWorld(point.getWorld());
        if (world == null || !player.getWorld().equals(world)
                || player.getLocation().distanceSquared(new Location(
                world, point.getX() + 0.5D, point.getY() + 0.5D, point.getZ() + 0.5D
        )) > 64.0D) {
            sendMessage(player, "errors.invalidpoint", "&cYou are too far from this opening point.", Map.of());
            return;
        }
        String keyId = args.length >= 4 ? CasePoint.normalizeBoxId(args[3]) : null;
        requestOpenCase(player, box, point, keyId);
    }

    private void handleBalanceCommand(
            CommandSender sender,
            String[] args,
            BalanceKind kind,
            BalanceOperation operation
    ) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        if (args.length < 4) {
            sendMessage(sender, "commands." + operation.messageKey + ".usage",
                    "&e/ecase " + args[0] + " <case> <player> <amount>", Map.of());
            return;
        }

        String unitId = CasePoint.normalizeBoxId(args[1]);
        CaseBox box = kind == BalanceKind.BOX ? boxes.get(unitId) : null;
        CaseKey key = kind == BalanceKind.KEY ? keys.get(unitId) : null;
        if ((kind == BalanceKind.BOX && box == null) || (kind == BalanceKind.KEY && key == null)) {
            sendMessage(sender, "commands.gts.invalidunit", "&cUnit not found.", Map.of("%item%", unitId));
            return;
        }

        int amount = parseNonNegativeInt(args[3]);
        if (amount < 0) {
            sendMessage(sender, "commands.gts.invalidnumber", "&cInvalid number: %number%",
                    Map.of("%number%", args[3]));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        PlayerCaseData data = getPlayerData(target);
        if (data == null) {
            sendMessage(sender, "commands.magicerror", "&cFailed to load player data.", Map.of("%throwable%", "IOException"));
            return;
        }

        int total;
        if (kind == BalanceKind.BOX) {
            total = switch (operation) {
                case GIVE -> data.addCases(unitId, amount);
                case TAKE -> data.addCases(unitId, -amount);
                case SET -> data.setCases(unitId, amount);
            };
        } else {
            total = switch (operation) {
                case GIVE -> data.addKeys(unitId, amount);
                case TAKE -> data.addKeys(unitId, -amount);
                case SET -> data.setKeys(unitId, amount);
            };
        }
        savePlayerData(data);

        Map<String, String> replacements = balancePlaceholders(sender, target, unitId, amount, total, kind);
        String successPath = "commands." + operation.messageKey + "." + (kind == BalanceKind.BOX ? "successbox" : "successkey");
        sendMessage(sender, successPath, "&aDone. Total: %total%", replacements);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && !Objects.equals(onlineTarget, sender)) {
            String transactionPath = "transaction." + (kind == BalanceKind.BOX ? "box" : "key") + "." + operation.messageKey;
            Map<String, String> transactionReplacements = new HashMap<>(replacements);
            transactionReplacements.put("%player%", sender.getName());
            transactionReplacements.put("%target%", displayName(target));
            sendMessage(onlineTarget, transactionPath, "", transactionReplacements);
        }
    }

    private void handleGenericBalanceCommand(
            CommandSender sender,
            String[] args,
            BalanceOperation operation
    ) {
        if (args.length >= 2 && keys.containsKey(CasePoint.normalizeBoxId(args[1]))
                && !boxes.containsKey(CasePoint.normalizeBoxId(args[1]))) {
            handleBalanceCommand(sender, args, BalanceKind.KEY, operation);
        } else {
            handleBalanceCommand(sender, args, BalanceKind.BOX, operation);
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "commands.remove.usage", "&e/ecase remove <player>", Map.of());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        playerData.remove(target.getUniqueId());
        try {
            if (dataStorage != null) {
                dataStorage.delete(target);
            }
            sendMessage(sender, "commands.remove.success", "&aPlayer data removed.",
                    Map.of("%player%", displayName(target)));
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to delete player data for " + target.getUniqueId(), exception);
            sendMessage(sender, "commands.magicerror", "&c%throwable%",
                    Map.of("%throwable%", exception.getClass().getSimpleName()));
        }
    }

    private void handleSimulate(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }
        if (args.length < 4) {
            sendMessage(sender, "commands.simulate.usage", "&e/ecase simulate <player> <case> <amount>", Map.of());
            return;
        }

        String boxId = CasePoint.normalizeBoxId(args[2]);
        CaseBox box = boxes.get(boxId);
        if (box == null) {
            sendMessage(sender, "commands.simulate.box", "&cCase not found.", Map.of("%box%", boxId));
            return;
        }

        int amount = parseNonNegativeInt(args[3]);
        if (amount <= 0) {
            sendMessage(sender, "commands.simulate.number", "&cInvalid number: %number%",
                    Map.of("%number%", args[3]));
            return;
        }

        long started = System.currentTimeMillis();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, CaseAward> awardsById = new HashMap<>();
        for (CaseAward award : box.getAwards()) {
            awardsById.put(award.getId(), award);
        }

        for (int i = 0; i < amount; i++) {
            CaseAward award = box.roll(random);
            if (award == null) {
                break;
            }
            counts.merge(award.getId(), 1, Integer::sum);
        }

        Map<String, String> replacements = Map.of(
                "%amount%", String.valueOf(amount),
                "%item%", box.getId(),
                "%player%", args[1],
                "%time%", String.valueOf(System.currentTimeMillis() - started)
        );
        sendMessage(sender, "commands.simulate.output", "&eSimulation results:", replacements);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            CaseAward award = awardsById.get(entry.getKey());
            String chance = award == null ? "0" : String.valueOf(award.getChance());
            sender.sendMessage(color("&e" + entry.getKey() + " &7| chance: &f" + chance + " &7| count: &f" + entry.getValue()));
        }
    }

    private void handleForceOpen(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(color("&e/ecase forceopen <player> <case>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        CaseBox box = boxes.get(CasePoint.normalizeBoxId(args[2]));
        if (target == null) {
            sendMessage(sender, "errors.player", "&cPlayer is offline.", Map.of());
            return;
        }
        if (box == null) {
            sendMessage(sender, "commands.open.invalidbox", "&cCase not found.", Map.of());
            return;
        }
        if (openingPlayers.contains(target.getUniqueId())) {
            sendMessage(sender, "session.opener.concurrent", "&cPlayer is already opening a case.", Map.of());
            return;
        }
        CaseAward award = box.roll(random, target);
        PlayerCaseData data = getPlayerData(target);
        if (award == null || data == null) {
            sendMessage(sender, "session.opener.error", "&cUnable to force-open this case.", Map.of());
            return;
        }
        beginCaseOpening(target, null);
        runActions(box.getOnOpenActions(), target, box, award);
        if (box.getAnimationSettings().isEnabled()) {
            playCaseAnimation(target, box, award, data, null, null);
        } else {
            finishCaseOpening(target, box, award, data, null, true);
        }
        sender.sendMessage(color("&aCase &e" + box.getId() + " &aforce-opened for &e" + target.getName()));
    }

    private void handleGenerateResources(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }
        String component = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
        switch (component) {
            case "all" -> {
                copyBundledDirectory("boxes");
                copyBundledDirectory("keys");
                copyBundledDirectory("points");
                saveResourceIfMissing("invgui.yml");
                saveResourceIfMissing("bookgui.yml");
                saveResourceIfMissing("holographicselector.yml");
                saveResourceIfMissing("effectors.yml");
            }
            case "boxes" -> copyBundledDirectory("boxes");
            case "keys" -> copyBundledDirectory("keys");
            case "points" -> copyBundledDirectory("points");
            case "gui" -> {
                saveResourceIfMissing("invgui.yml");
                saveResourceIfMissing("bookgui.yml");
                saveResourceIfMissing("holographicselector.yml");
                saveResourceIfMissing("effectors.yml");
            }
            default -> {
                sender.sendMessage(color("&cUnknown component. Use: all, boxes, keys, points, gui."));
                return;
            }
        }
        sender.sendMessage(color("&aResources generated: &e" + component));
    }

    private void handleService(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&e/ecase service <reload|toggleDebug|errors|history|executeAction|viewBlock>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "toggledebug" -> {
                boolean enabled = !getConfig().getBoolean("debug.enabled", false);
                getConfig().set("debug.enabled", enabled);
                saveConfig();
                sender.sendMessage(color("&aDebug mode: &e" + enabled));
            }
            case "errors" -> {
                if (catalog.loadErrors().isEmpty()) {
                    sender.sendMessage(color("&aNo unit loading errors."));
                } else {
                    catalog.loadErrors().forEach(error -> sender.sendMessage(color("&c- " + error)));
                }
            }
            case "history" -> showHistory(sender, args);
            case "executeaction" -> executeServiceAction(sender, args);
            case "viewblock" -> {
                if (!(sender instanceof Player player)) {
                    sendMessage(sender, "commands.onlyfromplayer", "&cOnly players can use this command.", Map.of());
                    return;
                }
                Block block = getTargetBlock(player);
                player.sendMessage(block == null
                        ? color("&cNo target block.")
                        : color("&eBlock: &f" + block.getType() + " &7at &f"
                        + block.getX() + " " + block.getY() + " " + block.getZ()));
            }
            default -> sender.sendMessage(color("&cUnknown service command."));
        }
    }

    private void showHistory(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&e/ecase service history <player> [limit]"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        PlayerCaseData data = getPlayerData(target);
        if (data == null) {
            sender.sendMessage(color("&cCould not load player history."));
            return;
        }
        int limit = args.length >= 4 ? Math.max(1, parseNonNegativeInt(args[3])) : 10;
        List<PlayerCaseData.HistoryEntry> history = data.getHistory();
        sender.sendMessage(color("&eHistory of &f" + displayName(target) + "&e:"));
        for (int i = 0; i < Math.min(limit, history.size()); i++) {
            PlayerCaseData.HistoryEntry entry = history.get(i);
            long ageSeconds = Math.max(0L, (System.currentTimeMillis() - entry.getTime()) / 1000L);
            sender.sendMessage(color("&7#" + (i + 1) + " &e" + entry.getBox() + " &7-> &f"
                    + entry.getText() + " &8(" + ageSeconds + "s ago"
                    + (entry.getKey().isBlank() ? "" : ", key " + entry.getKey()) + ")"));
        }
    }

    private void executeServiceAction(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(color("&e/ecase service executeAction <player> <case> <action...>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        CaseBox box = boxes.get(CasePoint.normalizeBoxId(args[3]));
        if (target == null || box == null) {
            sender.sendMessage(color("&cPlayer or case not found."));
            return;
        }
        String action = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        actionExecutor.execute(action, new ActionExecutor.Context(
                target, box, null, target.getLocation(), target.getLocation()
        ));
        sender.sendMessage(color("&aAction executed."));
    }

    private void handlePoints(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        if (args.length == 1) {
            sendMessage(sender, "commands.points.help", "&e/ecase points create/remove/list", Map.of());
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if ("list".equals(action)) {
            sendPointList(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sendMessage(sender, "commands.onlyfromplayer", "&cOnly players can use this command.", Map.of());
            return;
        }

        switch (action) {
            case "create" -> createPoint(player, args);
            case "remove" -> removePoint(player);
            case "setopenbydefault" -> setPointDefaultBox(player, args);
            case "setavailable" -> setPointAvailable(player, args);
            case "showonlyavailable" -> setPointShowOnlyAvailable(player, args);
            case "setselector" -> setPointSelector(player, args);
            case "sethologram" -> setPointHologram(player, args);
            case "setline" -> setPointLine(player, args);
            case "removeline" -> removePointLine(player, args);
            case "setheight" -> setPointHeight(player, args);
            case "seteffector" -> setPointEffector(player, args);
            case "setparameter" -> setPointParameter(player, args);
            case "copy" -> copyPoint(player);
            case "paste" -> pastePoint(player);
            default -> sendMessage(sender, "commands.points.help", "&e/ecase points create/remove/list", Map.of());
        }
    }

    private void handleReload(CommandSender sender) {
        if (!hasAdminPermission(sender)) {
            return;
        }

        try {
            reloadRuntimeData();
            if (!setupDataStorage()) {
                return;
            }
            sendMessage(sender, "commands.reload.success", "&aEpicCase reloaded.", Map.of());
        } catch (RuntimeException exception) {
            getLogger().log(Level.WARNING, "Failed to reload EpicCase.", exception);
            sendMessage(sender, "commands.reload.errors", "&cReload failed.", Map.of());
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "commands.help.main", "&e/ecase open &7- open case menu.", Map.of());
    }

    private void sendBoxList(CommandSender sender) {
        if (boxes.isEmpty()) {
            sender.sendMessage(color("&cNo cases loaded."));
            return;
        }

        sender.sendMessage(color("&eLoaded cases: &f" + String.join("&7, &f", boxes.keySet())));
    }

    private void createPoint(Player player, String[] args) {
        Block block = getTargetBlock(player);
        if (block == null) {
            sendMessage(player, "commands.points.void", "&cLook at a block first.", Map.of());
            return;
        }

        if (pointsByBlock.containsKey(CasePoint.blockKey(block))) {
            sendMessage(player, "commands.points.create.failed", "&cThis block is already a case point.", Map.of());
            return;
        }

        String pointId = args.length >= 3 ? safeId(args[2]) : defaultPointId(block);
        CasePoint point = CasePoint.fromBlock(pointId, block);
        if (args.length >= 4) {
            String boxId = CasePoint.normalizeBoxId(args[3]);
            if (!boxes.containsKey(boxId)) {
                sendMessage(player, "commands.points.openbydefault.failed", "&cCase not found.", Map.of("%box%", boxId));
                return;
            }
            point.setOpenByDefault(boxId);
        }

        savePoint(point);
        sendMessage(player, "commands.points.create.success", "&aCase point created.", Map.of("%point%", pointId));
    }

    private void removePoint(Player player) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        File file = pointFile(point);
        pointsById.remove(point.getId().toLowerCase(Locale.ROOT));
        pointsByBlock.remove(point.blockKey());
        removePointHologram(point);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to delete point " + point.getId(), exception);
        }
        sendMessage(player, "commands.points.remove", "&aCase point removed.", Map.of("%point%", point.getId()));
    }

    private void setPointDefaultBox(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            point.setOpenByDefault("");
            savePoint(point);
            sendMessage(player, "commands.points.openbydefault.clear", "&aDefault case cleared.", Map.of());
            return;
        }

        String boxId = CasePoint.normalizeBoxId(args[2]);
        if (!boxes.containsKey(boxId)) {
            sendMessage(player, "commands.points.openbydefault.failed", "&cCase not found.", Map.of("%box%", boxId));
            return;
        }

        point.setOpenByDefault(boxId);
        savePoint(point);
        sendMessage(player, "commands.points.openbydefault.success", "&aDefault case changed.", Map.of("%box%", boxId));
    }

    private void setPointAvailable(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            point.setAvailable(List.of());
            savePoint(point);
            sendMessage(player, "commands.points.available.clear", "&aPoint can now open every case.", Map.of());
            return;
        }

        List<String> available = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String boxId = CasePoint.normalizeBoxId(args[i].replace(",", ""));
            if (!boxId.isBlank()) {
                if (!boxes.containsKey(boxId)) {
                    sendMessage(player, "commands.points.available.failed", "&cCase not found.", Map.of("%box%", boxId));
                    return;
                }
                available.add(boxId);
            }
        }

        point.setAvailable(available);
        savePoint(point);
        sendMessage(player, "commands.points.available.success", "&aAvailable cases changed.", Map.of());
    }

    private void setPointShowOnlyAvailable(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3 || !List.of("true", "false").contains(args[2].toLowerCase(Locale.ROOT))) {
            sendMessage(player, "commands.points.showonlyavailable.usage",
                    "&e/ecase points showOnlyAvailable <true|false>", Map.of());
            return;
        }

        point.setShowOnlyAvailable(Boolean.parseBoolean(args[2]));
        savePoint(point);
        sendMessage(player, "commands.points.showonlyavailable.success", "&aParameter changed.", Map.of());
    }

    private void setPointSelector(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            sendMessage(player, "commands.points.selector.usage", "&e/ecase points setSelector <inventory|book|holographic>", Map.of());
            return;
        }

        String selector = args[2].toUpperCase(Locale.ROOT);
        if (!List.of("INVENTORY", "BOOK", "HOLOGRAPHIC").contains(selector)) {
            sendMessage(player, "commands.points.selector.invalid", "&cUnknown selector.", Map.of());
            return;
        }
        point.setSelector(selector);
        savePoint(point);
        sendMessage(player, "commands.points.selector.success", "&aSelector changed.", Map.of());
    }

    private void setPointHologram(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            sendMessage(player, "commands.points.hologram.usage",
                    "&e/ecase points setHologram <normal|individual|external>", Map.of());
            return;
        }

        String hologramType = args[2].toLowerCase(Locale.ROOT);
        if (!List.of("normal", "individual", "external").contains(hologramType)) {
            sendMessage(player, "commands.points.hologram.invalid", "&cUnknown hologram type.", Map.of());
            return;
        }
        point.setHologramType(hologramType);
        savePoint(point);
        sendMessage(player, "commands.points.hologram.success", "&aHologram type changed.", Map.of());
    }

    private void setPointLine(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 5) {
            sendMessage(player, "commands.points.setline.usage",
                    "&e/ecase points setLine <line> <text|item> <value>", Map.of());
            return;
        }

        int lineIndex = parseNonNegativeInt(args[2]);
        if (lineIndex <= 0) {
            sendMessage(player, "commands.points.setline.line", "&cInvalid line: %number%",
                    Map.of("%number%", args[2]));
            return;
        }

        String type = args[3].toLowerCase(Locale.ROOT);
        String value = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        if (type.equals("text") || type.equals("t")) {
            point.setHologramLine(lineIndex - 1, "t:" + value);
        } else if (type.equals("item") || type.equals("i")) {
            point.setHologramLine(lineIndex - 1, "i:" + value.toUpperCase(Locale.ROOT));
        } else {
            sendMessage(player, "commands.points.setline.invalid", "&cUnknown line type.", Map.of());
            return;
        }

        savePoint(point);
        sendMessage(player, "commands.points.setline.success", "&aHologram line changed.", Map.of());
    }

    private void removePointLine(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            sendMessage(player, "commands.points.removeline.usage",
                    "&e/ecase points removeLine <line>", Map.of());
            return;
        }

        int lineIndex = parseNonNegativeInt(args[2]);
        if (lineIndex <= 0) {
            sendMessage(player, "commands.points.removeline.line", "&cInvalid line: %number%",
                    Map.of("%number%", args[2]));
            return;
        }

        point.removeHologramLine(lineIndex - 1);
        savePoint(point);
        sendMessage(player, "commands.points.removeline.success", "&aHologram line removed.", Map.of());
    }

    private void setPointHeight(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }

        if (args.length < 3) {
            sendMessage(player, "commands.points.height.usage", "&e/ecase points setHeight <height>", Map.of());
            return;
        }

        try {
            point.setHologramHeight(Double.parseDouble(args[2]));
            savePoint(point);
            sendMessage(player, "commands.points.height.success", "&aHeight changed.", Map.of());
        } catch (NumberFormatException exception) {
            sendMessage(player, "commands.points.height.number", "&cInvalid number: %number%", Map.of("%number%", args[2]));
        }
    }

    private void setPointEffector(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }
        if (args.length < 3) {
            point.setEffectorKit(List.of());
            savePoint(point);
            sendMessage(player, "commands.points.effector.clear", "&aEffectors cleared.", Map.of());
            return;
        }
        List<String> effectors = Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 2, args.length)).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        point.setEffectorKit(effectors);
        savePoint(point);
        sendMessage(player, "commands.points.effector.success", "&aEffectors changed.", Map.of());
    }

    private void setPointParameter(Player player, String[] args) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }
        if (args.length < 3) {
            sendMessage(player, "commands.points.params.usage",
                    "&e/ecase points setParameter <parameter> [value]", Map.of());
            return;
        }
        String parameter = args[2];
        if (args.length < 4) {
            point.setParameter(parameter, null);
            savePoint(point);
            sendMessage(player, "commands.points.params.clear", "&aParameter cleared.", Map.of());
            return;
        }
        point.setParameter(parameter, parseConfigurationValue(
                String.join(" ", Arrays.copyOfRange(args, 3, args.length))
        ));
        savePoint(point);
        sendMessage(player, "commands.points.params.success", "&aParameter changed.", Map.of());
    }

    private void copyPoint(Player player) {
        CasePoint point = getTargetPoint(player);
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }
        pointClipboard.put(player.getUniqueId(), point.copySettings());
        sendMessage(player, "commands.points.copy", "&aPoint settings copied.", Map.of());
    }

    private void pastePoint(Player player) {
        CasePoint point = getTargetPoint(player);
        CasePoint.Settings settings = pointClipboard.get(player.getUniqueId());
        if (point == null) {
            sendMessage(player, "commands.points.notapoint", "&cThis block is not a case point.", Map.of());
            return;
        }
        if (settings == null) {
            sendMessage(player, "commands.points.paste", "&cCopy a point first.", Map.of());
            return;
        }
        point.applySettings(settings);
        savePoint(point);
        sendMessage(player, "commands.points.paste", "&aPoint settings pasted.", Map.of());
    }

    private void sendPointList(CommandSender sender) {
        if (pointsById.isEmpty()) {
            sender.sendMessage(color("&cNo case points created."));
            return;
        }

        for (CasePoint point : pointsById.values()) {
            sender.sendMessage(color("&e" + point.getId() + " &7-> &f" + point.blockKey()));
        }
    }

    private void savePoint(CasePoint point) {
        try {
            point.save(pointFile(point));
            pointsById.put(point.getId().toLowerCase(Locale.ROOT), point);
            pointsByBlock.put(point.blockKey(), point);
            refreshPointHologram(point);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to save point " + point.getId(), exception);
        }
    }

    private File pointFile(CasePoint point) {
        return new File(pointsFolder, safeId(point.getId()) + ".yml");
    }

    private Block getTargetBlock(Player player) {
        return player.getTargetBlockExact(6);
    }

    private CasePoint getTargetPoint(Player player) {
        Block block = getTargetBlock(player);
        if (block == null) {
            return null;
        }
        return pointsByBlock.get(CasePoint.blockKey(block));
    }

    private void openSelection(Player player, CasePoint point) {
        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            sendMessage(player, "session.opener.error", "&cFailed to load your case data.", Map.of());
            return;
        }

        List<String> schematic = inventoryConfig.getStringList("all_selection.schematic");
        if (schematic.isEmpty()) {
            schematic = List.of(
                    "a a a a a a a a a",
                    "a a a a a a a a a",
                    "a a a a X a a a a",
                    "a a a a a a a a a",
                    "a a a a a a a a a",
                    "b b b b b b b b b"
            );
        }

        int size = Math.min(54, Math.max(9, schematic.size() * 9));
        SelectionHolder holder = new SelectionHolder(point);
        Inventory inventory = Bukkit.createInventory(holder, size, color(inventoryConfig.getString(
                "all_selection.title", "&0Cases")));
        holder.inventory = inventory;

        List<Integer> caseSlots = new ArrayList<>();
        List<Integer> historySlots = new ArrayList<>();
        int emptySlot = Math.min(22, size - 1);
        for (int rowIndex = 0; rowIndex < schematic.size() && rowIndex * 9 < size; rowIndex++) {
            List<String> row = parseSchematicRow(schematic.get(rowIndex));
            for (int column = 0; column < 9 && column < row.size(); column++) {
                int slot = rowIndex * 9 + column;
                String token = row.get(column);
                if ("a".equalsIgnoreCase(token)) {
                    caseSlots.add(slot);
                    continue;
                }
                if ("X".equals(token)) {
                    emptySlot = slot;
                    continue;
                }
                if ("H".equals(token)) {
                    historySlots.add(slot);
                    continue;
                }

                ItemStack configured = createConfiguredItem("all_selection.items." + token);
                if (configured != null) {
                    inventory.setItem(slot, configured);
                }
            }
        }

        int slotIndex = 0;
        for (CaseBox box : boxes.values()) {
            boolean pointAllowed = point == null || point.allowsBox(box.getId());
            if (point != null && point.isShowOnlyAvailable() && !pointAllowed) {
                continue;
            }

            int amount = data.getCases(box.getId());
            BoxDisplayState state;
            if (!pointAllowed) {
                state = BoxDisplayState.WRONG_POINT;
            } else if (amount <= 0) {
                state = BoxDisplayState.AMOUNT;
            } else if (openingPlayers.contains(player.getUniqueId())) {
                state = BoxDisplayState.CONCURRENT;
            } else if (box.isUseKeys() && availableKeys(player, box).isEmpty()) {
                state = BoxDisplayState.KEYS;
            } else {
                state = BoxDisplayState.NORMAL;
            }
            if (state == BoxDisplayState.AMOUNT
                    && inventoryConfig.getBoolean("all_selection.dynamic", true)) {
                continue;
            }

            if (slotIndex >= caseSlots.size()) {
                break;
            }

            int slot = caseSlots.get(slotIndex++);
            inventory.setItem(slot, buildBoxItem(box, amount, state));
            if (state == BoxDisplayState.NORMAL) {
                holder.boxSlots.put(slot, box.getId());
            }
        }

        if (holder.boxSlots.isEmpty()) {
            ItemStack emptyItem = createConfiguredItem("all_selection.items.X");
            if (emptyItem != null) {
                inventory.setItem(emptySlot, emptyItem);
            }
        }
        List<PlayerCaseData.HistoryEntry> history = data.getHistory();
        for (int i = 0; i < historySlots.size() && i < history.size(); i++) {
            inventory.setItem(historySlots.get(i), buildHistoryItem(history.get(i), data.getName(), i + 1));
        }

        player.openInventory(inventory);
    }

    private void openBookSelection(Player player, CasePoint point) {
        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            sendMessage(player, "session.opener.error", "&cFailed to load your case data.", Map.of());
            return;
        }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        if (!(book.getItemMeta() instanceof BookMeta meta)) {
            openSelection(player, point);
            return;
        }
        meta.setTitle(color("&6Cases"));
        meta.setAuthor("EpicCase");
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        List<Component> pages = new ArrayList<>();
        Component page = legacy.deserialize("&6&lВыберите кейс\n\n");
        int rows = 0;
        for (CaseBox box : availableBoxes(player, point)) {
            int amount = data.getCases(box.getId());
            String command = "/ecase openpoint " + point.getId() + " " + box.getId();
            Component row = legacy.deserialize(box.getName() + " &7x&e" + amount + "\n")
                    .clickEvent(ClickEvent.runCommand(command));
            if (rows == 6) {
                pages.add(page);
                page = Component.empty();
                rows = 0;
            }
            page = page.append(row);
            rows++;
        }
        if (rows == 0 && pages.isEmpty()) {
            page = legacy.deserialize("&cУ вас нет доступных кейсов.");
        }
        pages.add(page);
        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private void handleHolographicSelector(Player player, CasePoint point, Action action) {
        List<CaseBox> available = availableBoxes(player, point);
        if (available.isEmpty()) {
            sendMessage(player, "session.opener.notenoughboxes", "&cYou do not have an available case.", Map.of());
            return;
        }
        Map<String, String> selections = holographicSelections.computeIfAbsent(
                player.getUniqueId(), ignored -> new HashMap<>()
        );
        String selectedId = selections.get(point.getId());
        int index = 0;
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).getId().equals(selectedId)) {
                index = i;
                break;
            }
        }
        if (action == Action.LEFT_CLICK_BLOCK) {
            index = (index + 1) % available.size();
        }
        CaseBox selected = available.get(index);
        selections.put(point.getId(), selected.getId());
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&6" + selected.getName() + " &7x&e" + getPlayerData(player).getCases(selected.getId())
                        + (action == Action.LEFT_CLICK_BLOCK ? " &8| &fПКМ: открыть" : "")
        ));
        if (action == Action.RIGHT_CLICK_BLOCK) {
            requestOpenCase(player, selected, point);
        }
    }

    private List<CaseBox> availableBoxes(Player player, CasePoint point) {
        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            return List.of();
        }
        return boxes.values().stream()
                .filter(box -> point == null || point.allowsBox(box.getId()))
                .filter(box -> data.getCases(box.getId()) > 0)
                .toList();
    }

    private void requestOpenCase(Player player, CaseBox box, CasePoint point) {
        requestOpenCase(player, box, point, null);
    }

    private void requestOpenCase(Player player, CaseBox box, CasePoint point, String requestedKeyId) {
        if (!canStartOpeningBase(player, box, point)) {
            return;
        }
        String keyId = requestedKeyId;
        if (box.isUseKeys()) {
            List<CaseKey> availableKeys = availableKeys(player, box);
            if (keyId != null && !keyId.isBlank()) {
                CaseKey requestedKey = keys.get(CasePoint.normalizeBoxId(keyId));
                if (requestedKey == null || !requestedKey.canOpen(box.getId())) {
                    sendMessage(player, "commands.open.invalidkey", "&cThis key cannot open the case.", Map.of());
                    return;
                }
                keyId = requestedKey.getId();
            } else if (availableKeys.size() == 1) {
                keyId = availableKeys.getFirst().getId();
            } else if (availableKeys.size() > 1) {
                openKeySelection(player, box, point, availableKeys);
                return;
            }
        }

        if (!canStartOpening(player, box, point, keyId)) {
            return;
        }

        if (!inventoryConfig.getBoolean("confirmation.enabled", true)) {
            openCase(player, box, point, keyId);
            return;
        }

        openConfirmation(player, box, point, keyId);
    }

    private List<CaseKey> availableKeys(Player player, CaseBox box) {
        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            return List.of();
        }
        return catalog.keysFor(box).stream()
                .filter(key -> data.getKeys(key.getId()) >= box.getKeyWithdrawalAmount())
                .toList();
    }

    private boolean canStartOpening(Player player, CaseBox box, CasePoint point, String keyId) {
        if (!canStartOpeningBase(player, box, point)) {
            return false;
        }
        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            return false;
        }
        if (box.isUseKeys() && (keyId == null || keyId.isBlank())) {
            sendMessage(player, "session.opener.notenoughkeys", "&cYou do not have a key for this case.", Map.of());
            return false;
        }
        if (box.isUseKeys()) {
            CaseKey key = keys.get(CasePoint.normalizeBoxId(keyId));
            if (key == null || !key.canOpen(box.getId())
                    || data.getKeys(key.getId()) < box.getKeyWithdrawalAmount()) {
                sendMessage(player, "session.opener.notenoughkeys", "&cYou do not have enough selected keys.", Map.of());
                return false;
            }
        }
        return true;
    }

    private boolean canStartOpeningBase(Player player, CaseBox box, CasePoint point) {
        if (openingPlayers.contains(player.getUniqueId())) {
            sendMessage(player, "session.opener.concurrent", "&cYou are already opening a case.", Map.of());
            return false;
        }

        if (point != null && activePointOpenings.containsKey(point.blockKey())) {
            sendMessage(player, "session.opener.concurrent", "&cThis case point is already in use.", Map.of());
            return false;
        }

        if (point != null && !point.allowsBox(box.getId())) {
            sendMessage(player, "errors.invalidpoint", "&cThis case cannot be opened at this point.", Map.of());
            return false;
        }

        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            sendMessage(player, "session.opener.error", "&cFailed to load your case data.", Map.of());
            return false;
        }

        if (data.getCases(box.getId()) <= 0) {
            sendMessage(player, "session.opener.notenoughboxes", "&cYou do not have this case.", Map.of());
            return false;
        }

        return true;
    }

    private void openKeySelection(Player player, CaseBox box, CasePoint point, List<CaseKey> availableKeys) {
        List<String> schematic = inventoryConfig.getStringList("key_selection.schematic");
        if (schematic.isEmpty()) {
            schematic = List.of(
                    "b b b b Q b b b b",
                    "b a a a a a a a b",
                    "b b b b b b b b b"
            );
        }
        int size = Math.min(54, Math.max(9, schematic.size() * 9));
        KeySelectionHolder holder = new KeySelectionHolder(box.getId(), point);
        Inventory inventory = Bukkit.createInventory(holder, size, color(inventoryConfig.getString(
                "key_selection.title", "&0Select a key"
        )));
        holder.inventory = inventory;
        List<Integer> keySlots = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < schematic.size() && rowIndex * 9 < size; rowIndex++) {
            List<String> row = parseSchematicRow(schematic.get(rowIndex));
            for (int column = 0; column < 9 && column < row.size(); column++) {
                int slot = rowIndex * 9 + column;
                String token = row.get(column);
                if ("a".equalsIgnoreCase(token)) {
                    keySlots.add(slot);
                } else {
                    ItemStack configured = createConfiguredItem("key_selection.items." + token);
                    if (configured != null) {
                        inventory.setItem(slot, configured);
                    }
                }
            }
        }
        PlayerCaseData data = getPlayerData(player);
        for (int i = 0; i < availableKeys.size() && i < keySlots.size(); i++) {
            CaseKey key = availableKeys.get(i);
            int slot = keySlots.get(i);
            inventory.setItem(slot, buildKeyItem(key, data.getKeys(key.getId())));
            holder.keySlots.put(slot, key.getId());
        }
        player.openInventory(inventory);
    }

    private void openConfirmation(Player player, CaseBox box, CasePoint point, String keyId) {
        List<String> schematic = inventoryConfig.getStringList("confirmation.schematic");
        if (schematic.isEmpty()) {
            schematic = List.of(
                    "s s s s Q s s s s",
                    "s s s s s s s s s",
                    "s A A A s D D D s",
                    "s A A A s D D D s",
                    "s A A A s D D D s",
                    "s s s s s s s s s"
            );
        }

        int size = Math.min(54, Math.max(9, schematic.size() * 9));
        ConfirmationHolder holder = new ConfirmationHolder(box.getId(), point, keyId);
        Inventory inventory = Bukkit.createInventory(holder, size, color(inventoryConfig.getString(
                "confirmation.title", "&0Confirm")));
        holder.inventory = inventory;

        for (int rowIndex = 0; rowIndex < schematic.size() && rowIndex * 9 < size; rowIndex++) {
            List<String> row = parseSchematicRow(schematic.get(rowIndex));
            for (int column = 0; column < 9 && column < row.size(); column++) {
                int slot = rowIndex * 9 + column;
                String token = row.get(column);
                ItemStack configured = createConfiguredItem("confirmation.items." + token);
                if (configured != null) {
                    inventory.setItem(slot, configured);
                }
                if ("A".equals(token)) {
                    holder.yesSlots.add(slot);
                } else if ("D".equals(token)) {
                    holder.noSlots.add(slot);
                }
            }
        }

        player.openInventory(inventory);
    }

    private void openCase(Player player, CaseBox box, CasePoint point, String keyId) {
        if (!canStartOpening(player, box, point, keyId)) {
            return;
        }

        CaseAward award = box.roll(random, player);
        if (award == null) {
            sendMessage(player, "session.opener.error", "&cThis case has no awards.", Map.of());
            return;
        }

        PlayerCaseData data = getPlayerData(player);
        if (data == null) {
            sendMessage(player, "session.opener.error", "&cFailed to load your case data.", Map.of());
            return;
        }

        data.addCases(box.getId(), -1);
        if (box.isUseKeys()) {
            data.addKeys(keyId, -box.getKeyWithdrawalAmount());
        }
        savePlayerData(data);

        beginCaseOpening(player, point);
        player.closeInventory();
        try {
            runActions(box.getOnOpenActions(), player, box, award);
            runPointEffectors(point, player, box, award);
            if (box.getAnimationSettings().isEnabled()) {
                playCaseAnimation(player, box, award, data, point, keyId);
            } else {
                finishCaseOpening(player, box, award, data, keyId, true);
            }
        } catch (RuntimeException exception) {
            getLogger().log(Level.WARNING, "Failed to execute case actions for " + box.getId(), exception);
            sendMessage(player, "session.opener.error", "&cAn error occurred while opening this case.", Map.of());
            savePlayerData(data);
            endCaseOpening(player.getUniqueId());
        }
    }

    private void playCaseAnimation(
            Player player,
            CaseBox box,
            CaseAward award,
            PlayerCaseData data,
            CasePoint point,
            String keyId
    ) {
        AnimationSettings settings = box.getAnimationSettings();
        Location center = caseAnimationLocation(player, point);
        if (center == null || center.getWorld() == null) {
            finishCaseOpening(player, box, award, data, keyId, true);
            return;
        }
        center.add(0.0D, Math.max(0.0D, settings.getWheelRadius() - 1.45D), 0.0D);
        int totalTicks = Math.max(1, settings.getDurationTicks());
        int rotationStart = settings.getAscensionTime();
        int rotationEnd = rotationStart + settings.getRotationTime();
        double finalSpin = animationFinalSpin(settings);
        int firstVisibleTick = settings.getAscensionTime() > 0 ? Math.min(1, totalTicks) : 0;
        WheelAnimationGeometry.Frame initialWheelFrame = animationWheelFrame(player, center);
        double initialRadius = animationRadius(settings, firstVisibleTick);
        double initialSpin = animationSpin(settings, firstVisibleTick, finalSpin);

        List<AnimationDisplay> displays = spawnAnimationWheel(
                player,
                center,
                box,
                award,
                settings,
                initialWheelFrame,
                initialRadius,
                initialSpin
        );
        if (displays.isEmpty()) {
            finishCaseOpening(player, box, award, data, keyId, true);
            return;
        }

        List<Integer> removalOrder = animationRemovalOrder(displays.size());
        new BukkitRunnable() {
            private int tick = firstVisibleTick;
            private int lastClickSector = Integer.MIN_VALUE;
            private int removedElements;

            @Override
            public void run() {
                Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
                if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                    savePlayerData(data);
                    endCaseOpening(player.getUniqueId());
                    removeAnimationDisplays(displays);
                    cancel();
                    return;
                }

                WheelAnimationGeometry.Frame wheelFrame = initialWheelFrame;
                double spin = animationSpin(settings, tick, finalSpin);
                double radius = animationRadius(settings, tick);
                if (tick == firstVisibleTick || tick % 2 == 0 || tick >= totalTicks) {
                    updateAnimationWheel(displays, center, wheelFrame, radius, spin);
                }

                if (tick >= rotationStart && tick <= rotationEnd && settings.getWheelCapacity() > 1) {
                    int clickSector = animationClickSector(spin, settings.getWheelCapacity());
                    if (lastClickSector != Integer.MIN_VALUE
                            && clickSector != lastClickSector
                            && !settings.getClickingSound().isBlank()) {
                        playSoundAction(center, settings.getClickingSound());
                    }
                    lastClickSector = clickSector;
                }

                if (!settings.getTrailParticle().isBlank()
                        && settings.getWheelTrails() > 0
                        && radius > 0.05D
                        && tick % 2 == 0) {
                    spawnAnimationTrails(onlinePlayer, center, wheelFrame, radius, spin, settings);
                }

                if (tick > rotationEnd && settings.getDescensionTime() > 0) {
                    int descensionTick = tick - rotationEnd;
                    int shouldBeRemoved = Math.min(
                            removalOrder.size(),
                            descensionTick / settings.getRemovalInterrupt()
                    );
                    while (removedElements < shouldBeRemoved) {
                        removeAnimationDisplay(displays.get(removalOrder.get(removedElements)));
                        removedElements++;
                    }
                }

                if (tick >= totalTicks) {
                    cancel();
                    for (int i = 1; i < displays.size(); i++) {
                        removeAnimationDisplay(displays.get(i));
                    }
                    displays.getFirst().move(finalAwardLocation(center, settings));
                    Bukkit.getScheduler().runTaskLater(EpicCase.this, () -> {
                        removeAnimationDisplays(displays);
                        Player finishedPlayer = Bukkit.getPlayer(player.getUniqueId());
                        if (finishedPlayer == null || !finishedPlayer.isOnline()) {
                            savePlayerData(data);
                            endCaseOpening(player.getUniqueId());
                            return;
                        }
                        finishCaseOpening(finishedPlayer, box, award, data, keyId, true);
                    }, 24L);
                    return;
                }

                tick++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private Location caseAnimationLocation(Player player, CasePoint point) {
        if (point != null) {
            World world = Bukkit.getWorld(point.getWorld());
            if (world != null) {
                return new Location(world, point.getX() + 0.5D, point.getY() + 1.45D, point.getZ() + 0.5D);
            }
        }

        Location location = player.getLocation().clone();
        Vector direction = location.getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.01D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        }
        return location.add(direction.normalize().multiply(2.0D)).add(0.0D, 1.2D, 0.0D);
    }

    private WheelAnimationGeometry.Frame animationWheelFrame(Player player, Location center) {
        // Always face north (negative Z direction) instead of facing the player
        Vector northDirection = new Vector(0.0D, 0.0D, -1.0D);
        Vector viewerPosition = center.clone().add(northDirection.multiply(2)).toVector();
        return WheelAnimationGeometry.verticalFacing(
                center.toVector(),
                viewerPosition,
                northDirection
        );
    }

    private List<AnimationDisplay> spawnAnimationWheel(
            Player viewer,
            Location center,
            CaseBox box,
            CaseAward winner,
            AnimationSettings settings,
            WheelAnimationGeometry.Frame wheelFrame,
            double radius,
            double spin
    ) {
        List<AnimationDisplay> displays = new ArrayList<>();
        int capacity = settings.getWheelCapacity();
        List<CaseAward> sequence = animationAwardSequence(
                box,
                winner,
                capacity,
                settings.getRecurringPrizeLimit()
        );
        for (int i = 0; i < capacity; i++) {
            CaseAward visibleAward = sequence.get(i);
            Location location = wheelLocation(center, wheelFrame, radius, spin, i, capacity);
            displays.add(spawnAnimationDisplay(
                    center.getWorld(),
                    location,
                    createAwardDisplayItem(box, visibleAward),
                    visibleAward == null ? box.getName() : visibleAward.getHologramText()
            ));
        }
        return displays;
    }

    private void updateAnimationWheel(
            List<AnimationDisplay> displays,
            Location center,
            WheelAnimationGeometry.Frame wheelFrame,
            double radius,
            double spin
    ) {
        int count = displays.size();
        for (int i = 0; i < count; i++) {
            Location location = wheelLocation(
                    center,
                    wheelFrame,
                    radius,
                    spin,
                    i,
                    count
            );
            displays.get(i).move(location);
        }
    }

    private Location wheelLocation(
            Location center,
            WheelAnimationGeometry.Frame wheelFrame,
            double radius,
            double spin,
            int index,
            int count
    ) {
        double angle = spin + Math.PI * 2.0D * index / Math.max(1, count);
        return center.clone().add(WheelAnimationGeometry.offset(wheelFrame, radius, angle));
    }

    private AnimationDisplay spawnAnimationDisplay(
            World world,
            Location location,
            ItemStack item,
            String text
    ) {
        ItemDisplay itemDisplay = world.spawn(location, ItemDisplay.class, false, display -> {
            configureAnimationEntity(display);
            configureDisplay(display, 0.72F);
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        });
        TextDisplay textDisplay = world.spawn(
                location.clone().add(0.0D, 0.62D, 0.0D),
                TextDisplay.class,
                false,
                display -> {
                    configureAnimationEntity(display);
                    configureDisplay(display, 0.78F);
                    display.text(LegacyComponentSerializer.legacySection().deserialize(color(text)));
                    display.setLineWidth(220);
                    display.setShadowed(true);
                    display.setSeeThrough(true);
                    display.setDefaultBackground(false);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                }
        );
        showPrivateAnimationEntity(null, itemDisplay);
        showPrivateAnimationEntity(null, textDisplay);
        return new AnimationDisplay(itemDisplay, textDisplay);
    }

    private void configureAnimationEntity(Entity entity) {
        entity.setVisibleByDefault(true);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setPersistent(false);
        entity.addScoreboardTag(ANIMATION_TAG);
    }

    private void configureDisplay(Display display, float scale) {
        display.setBillboard(Display.Billboard.VERTICAL);
        display.setInterpolationDuration(1);
        display.setTeleportDuration(2);
        display.setViewRange(32.0F);
        display.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()
        ));
    }

    private void showPrivateAnimationEntity(Player viewer, Entity entity) {
        trackAnimationEntity(entity);
        // Show animation to all nearby players, not just the opener
        for (Player nearbyPlayer : entity.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(entity.getLocation()) <= 32.0) {
                nearbyPlayer.showEntity(this, entity);
            }
        }
    }

    private List<CaseAward> animationAwardSequence(
            CaseBox box,
            CaseAward winner,
            int capacity,
            int recurringPrizeLimit
    ) {
        List<CaseAward> visibleAwards = box.getAwards().stream()
                .filter(visibleAward -> !visibleAward.isPhantom())
                .toList();
        List<CaseAward> sequence = new ArrayList<>(capacity);
        sequence.add(winner);
        if (visibleAwards.isEmpty()) {
            while (sequence.size() < capacity) {
                sequence.add(winner);
            }
            return sequence;
        }

        while (sequence.size() < capacity) {
            List<CaseAward> allowed = visibleAwards.stream()
                    .filter(candidate -> !wouldExceedRecurringLimit(sequence, candidate, recurringPrizeLimit))
                    .toList();
            List<CaseAward> choices = allowed.isEmpty() ? visibleAwards : allowed;
            sequence.add(choices.get(random.nextInt(choices.size())));
        }
        return sequence;
    }

    private boolean wouldExceedRecurringLimit(
            List<CaseAward> sequence,
            CaseAward candidate,
            int recurringPrizeLimit
    ) {
        int recurring = 0;
        for (int i = sequence.size() - 1; i >= 0; i--) {
            CaseAward existing = sequence.get(i);
            if (existing == null || candidate == null || !existing.getId().equals(candidate.getId())) {
                break;
            }
            recurring++;
        }
        return recurring >= recurringPrizeLimit;
    }

    private double animationRadius(AnimationSettings settings, int tick) {
        int ascensionTime = settings.getAscensionTime();
        if (ascensionTime > 0 && tick < ascensionTime) {
            double progress = tick / (double) ascensionTime;
            return settings.getWheelRadius() * settings.getAscensionMethod().movement(progress);
        }

        int rotationEnd = ascensionTime + settings.getRotationTime();
        int descensionTime = settings.getDescensionTime();
        if (descensionTime > 0 && tick > rotationEnd) {
            double progress = Math.min(1.0D, (tick - rotationEnd) / (double) descensionTime);
            return settings.getWheelRadius()
                    * (1.0D - settings.getDescensionMethod().movement(progress));
        }
        return settings.getWheelRadius();
    }

    private double animationSpin(AnimationSettings settings, int tick, double finalSpin) {
        int rotationTime = settings.getRotationTime();
        if (tick <= settings.getAscensionTime()) {
            return 0.0D;
        }
        if (rotationTime <= 0) {
            return finalSpin;
        }
        double progress = Math.min(
                1.0D,
                (tick - settings.getAscensionTime()) / (double) rotationTime
        );
        return finalSpin * normalizedMovement(settings.getRotationMethod(), progress);
    }

    private double normalizedMovement(AnimationSettings.MotionMethod method, double progress) {
        double start = method.movement(0.0D);
        double end = method.movement(1.0D);
        if (Math.abs(end - start) < 0.000001D) {
            return Math.max(0.0D, Math.min(1.0D, progress));
        }
        return (method.movement(progress) - start) / (end - start);
    }

    private double animationFinalSpin(AnimationSettings settings) {
        double averageSpeed = averageAnimationSpeed(settings.getRotationMethod());
        double naturalDistance = settings.getPeakRotationSpeed()
                * settings.getRotationTime()
                * averageSpeed;
        double selectorAngle = 270.0D;
        long turns = Math.max(0L, Math.round((naturalDistance - selectorAngle) / 360.0D));
        return Math.toRadians(selectorAngle + turns * 360.0D);
    }

    private double averageAnimationSpeed(AnimationSettings.MotionMethod method) {
        int samples = 256;
        double total = 0.0D;
        for (int i = 0; i < samples; i++) {
            total += method.speed((i + 0.5D) / samples);
        }
        return total / samples;
    }

    private int animationClickSector(double spin, int capacity) {
        double sectorSize = Math.PI * 2.0D / Math.max(1, capacity);
        return (int) Math.floor((spin + Math.PI / 2.0D) / sectorSize);
    }

    private List<Integer> animationRemovalOrder(int capacity) {
        List<Integer> order = new ArrayList<>();
        for (int i = 1; i < capacity; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingInt(index -> -Math.min(index, capacity - index)));
        return order;
    }

    private void spawnAnimationTrails(
            Player viewer,
            Location center,
            WheelAnimationGeometry.Frame wheelFrame,
            double radius,
            double spin,
            AnimationSettings settings
    ) {
        int trails = settings.getWheelTrails();
        for (int i = 0; i < trails; i++) {
            double trailSpin = spin + Math.PI * 2.0D * i / trails;
            Location location = wheelLocation(center, wheelFrame, radius, trailSpin, 0, 1);
            // Show particles to all nearby players, not just the viewer
            for (Player nearbyPlayer : center.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(location) <= 32.0) {
                    playParticleAt(nearbyPlayer, location, settings.getTrailParticle(), 1, 0.02D);
                }
            }
        }
    }

    private Location finalAwardLocation(Location center, AnimationSettings settings) {
        if ("MID".equals(settings.getDescentToThe())) {
            return center.clone().add(0.0D, 0.1D, 0.0D);
        }
        return center.clone().add(0.0D, -settings.getWheelRadius(), 0.0D);
    }

    private void removeAnimationDisplays(Collection<AnimationDisplay> displays) {
        for (AnimationDisplay display : displays) {
            removeAnimationDisplay(display);
        }
    }

    private void removeAnimationDisplay(AnimationDisplay display) {
        removeEntity(display.itemDisplay);
        removeEntity(display.textDisplay);
    }

    private void finishCaseOpening(
            Player player,
            CaseBox box,
            CaseAward award,
            PlayerCaseData data,
            String keyId,
            boolean closeInventory
    ) {
        try {
            List<String> awardActions = new ArrayList<>();
            awardActions.addAll(box.getOnAwardActions());
            awardActions.addAll(award.getActions());
            awardActions.addAll(box.getEndActions());
            runActions(awardActions, player, box, award);
            data.addHistory(box, award, keyId);
        } catch (RuntimeException exception) {
            getLogger().log(Level.WARNING, "Failed to execute case award actions for " + box.getId(), exception);
            sendMessage(player, "session.opener.error", "&cAn error occurred while opening this case.", Map.of());
        } finally {
            savePlayerData(data);
            endCaseOpening(player.getUniqueId());
            if (closeInventory) {
                player.closeInventory();
            }
        }
    }

    private void beginCaseOpening(Player player, CasePoint point) {
        openingPlayers.add(player.getUniqueId());
        if (point == null) {
            return;
        }

        World world = Bukkit.getWorld(point.getWorld());
        if (world == null) {
            return;
        }

        String pointKey = point.blockKey();
        Block block = world.getBlockAt(point.getX(), point.getY(), point.getZ());
        ActivePointOpening state = new ActivePointOpening(point, block.getState());
        activePointOpenings.put(pointKey, state);
        openingPointByPlayer.put(player.getUniqueId(), pointKey);
        removePointHologram(point);
        block.setType(Material.AIR, false);
    }

    private void endCaseOpening(UUID playerId) {
        openingPlayers.remove(playerId);
        String pointKey = openingPointByPlayer.remove(playerId);
        if (pointKey == null) {
            return;
        }

        ActivePointOpening state = activePointOpenings.remove(pointKey);
        if (state != null) {
            restoreOpeningPoint(state, true);
        }
    }

    private void restoreAllOpeningPoints() {
        for (ActivePointOpening state : List.copyOf(activePointOpenings.values())) {
            restoreOpeningPoint(state, false);
        }
    }

    private void restoreOpeningPoint(ActivePointOpening state, boolean restoreHologram) {
        CasePoint point = state.point();
        World world = Bukkit.getWorld(point.getWorld());
        if (world == null) {
            return;
        }

        state.blockState().update(true, false);
        if (restoreHologram) {
            refreshPointHologram(point);
        }
    }

    private ItemStack createAwardDisplayItem(CaseBox box, CaseAward award) {
        ItemStack stack = createItemFromSpec(award == null ? box.getItemSpec() : award.getHologramItem());
        if (award != null && award.isRare()) {
            var meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private void runActions(List<String> actions, Player player, CaseBox box, CaseAward award) {
        actionExecutor.execute(actions, new ActionExecutor.Context(
                player,
                box,
                award,
                player.getLocation(),
                player.getLocation()
        ));
    }

    private void runPointEffectors(CasePoint point, Player player, CaseBox box, CaseAward award) {
        if (point == null || point.getEffectorKit().isEmpty()) {
            return;
        }
        Location middle = caseAnimationLocation(player, point);
        for (String effector : point.getEffectorKit()) {
            List<String> actions = effectorConfig.getStringList("effectors." + effector + ".actions");
            if (actions.isEmpty()) {
                debug("Unknown point effector: " + effector);
                continue;
            }
            actionExecutor.execute(actions, new ActionExecutor.Context(
                    player,
                    box,
                    award,
                    player.getLocation(),
                    middle,
                    point.getParameters()
            ));
        }
    }

    private void playSoundAction(Location location, String soundName) {
        String keyValue = soundName.toLowerCase(Locale.ROOT).replace('_', '.');
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(
                keyValue.contains(":") ? keyValue : "minecraft:" + keyValue
        );
        Sound sound = key == null ? null : org.bukkit.Registry.SOUNDS.get(key);
        if (sound != null) {
            // Play sound for all nearby players, not just the opener
            for (Player nearbyPlayer : location.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(location) <= 32.0) {
                    nearbyPlayer.playSound(location, sound, 1.0F, 1.0F);
                }
            }
        } else {
            debug("Unknown sound: " + soundName);
        }
    }

    private void playParticleAt(Player viewer, Location location, String particleName, int count, double offset) {
        String normalized = particleName.replace("#", "").toUpperCase(Locale.ROOT);
        if ("REDSTONE".equals(normalized)) {
            normalized = "DUST";
        } else if ("EXPLOSION_LARGE".equals(normalized) || "EXPLOSION_HUGE".equals(normalized)) {
            normalized = "EXPLOSION";
        } else if ("EXPLOSION_NORMAL".equals(normalized)) {
            normalized = "POOF";
        }

        try {
            Particle particle = Particle.valueOf(normalized);
            if (particle == Particle.DUST) {
                viewer.spawnParticle(
                        particle,
                        location,
                        count,
                        offset,
                        offset,
                        offset,
                        0.01D,
                        new Particle.DustOptions(Color.RED, 1.2F)
                );
                return;
            }

            viewer.spawnParticle(particle, location, count, offset, offset, offset, 0.01D);
        } catch (RuntimeException exception) {
            debug("Unknown or unsupported particle: " + particleName);
        }
    }

    private void trackAnimationEntity(Entity entity) {
        animationEntityIds.add(entity.getUniqueId());
    }

    private void removeSpawnedAnimationEntities() {
        for (UUID entityId : new HashSet<>(animationEntityIds)) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        animationEntityIds.clear();
    }

    private void removeEntities(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            removeEntity(entity);
        }
    }

    private void removeEntity(Entity entity) {
        if (entity == null) {
            return;
        }
        animationEntityIds.remove(entity.getUniqueId());
        entity.remove();
    }

    private ItemStack buildBoxItem(CaseBox box, int amount, BoxDisplayState state) {
        ItemStack stack = createItemFromSpec(
                state == BoxDisplayState.NORMAL ? box.getItemSpec() : box.getEmptyItemSpec()
        );
        stack.setAmount(Math.max(1, Math.min(64, amount)));

        String nameTemplate = messages.getString("gui.inventory.boxitem.name", "%coloredBoxName%");
        String name = nameTemplate.replace("%coloredBoxName%", box.getName())
                .replace("%box%", box.getId())
                .replace("%amount%", String.valueOf(amount));

        List<String> lore = new ArrayList<>();
        List<String> templateLore = messages.getStringList(
                "gui.inventory.boxitem.description." + state.messageKey
        );
        if (templateLore.isEmpty()) {
            lore.addAll(box.getDescription());
            lore.add("");
            lore.add("&7Amount: &e" + amount);
        } else {
            String requiredKeys = catalog.keysFor(box).stream()
                    .map(CaseKey::getName)
                    .reduce((left, right) -> left + "&7, " + right)
                    .orElse("");
            for (String line : templateLore) {
                if (line.trim().equals("%description%")) {
                    lore.addAll(box.getDescription());
                } else {
                    lore.add(line
                            .replace("%description%", String.join("\n", box.getDescription()))
                            .replace("%coloredBoxName%", box.getName())
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%box%", box.getId())
                            .replace("%requiredKeys%", requiredKeys)
                            .replace("%keyWithdrawalAmount%", String.valueOf(box.getKeyWithdrawalAmount())));
                }
            }
        }

        applyMeta(stack, name, lore);
        return stack;
    }

    private ItemStack buildKeyItem(CaseKey key, int amount) {
        ItemStack stack = createItemFromSpec(key.getItemSpec());
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        String nameTemplate = messages.getString(
                "gui.inventory.keyitem.name",
                "%coloredKeyName% &8(&7%amount%&8)"
        );
        String name = nameTemplate
                .replace("%coloredKeyName%", key.getName())
                .replace("%commonKeyName%", key.getId())
                .replace("%amount%", String.valueOf(amount));
        List<String> lore = new ArrayList<>();
        List<String> template = messages.getStringList("gui.inventory.keyitem.description");
        if (template.isEmpty()) {
            lore.addAll(key.getDescription());
            lore.add("");
            lore.add("&7Количество: &e" + amount);
        } else {
            for (String line : template) {
                if ("%description%".equals(line.trim())) {
                    lore.addAll(key.getDescription());
                } else if ("%boxes%".equals(line.trim())) {
                    key.getCanOpen().forEach(box -> lore.add("&8- &e" + box));
                } else {
                    lore.add(line
                            .replace("%coloredKeyName%", key.getName())
                            .replace("%commonKeyName%", key.getId())
                            .replace("%amount%", String.valueOf(amount)));
                }
            }
        }
        applyMeta(stack, name, lore);
        return stack;
    }

    private ItemStack buildHistoryItem(PlayerCaseData.HistoryEntry entry, String username, int index) {
        CaseBox box = boxes.get(entry.getBox());
        CaseAward award = box == null ? null : box.getAwards().stream()
                .filter(candidate -> candidate.getId().equals(entry.getAward()))
                .findFirst()
                .orElse(null);
        ItemStack stack = createItemFromSpec(award == null ? "PAPER" : award.getHologramItem());
        long ageSeconds = Math.max(0L, (System.currentTimeMillis() - entry.getTime()) / 1000L);
        Map<String, String> replacements = Map.of(
                "%index%", String.valueOf(index),
                "%hologramText%", entry.getText(),
                "%username%", username,
                "%coloredBoxName%", box == null ? entry.getBox() : box.getName(),
                "%chance%", String.valueOf(entry.getChance()),
                "%etime%", String.valueOf(ageSeconds)
        );
        String name = applyReplacements(
                messages.getString("gui.inventory.history.name", "&f#%index% %hologramText%"),
                replacements
        );
        List<String> lore = messages.getStringList("gui.inventory.history.description").stream()
                .map(line -> applyReplacements(line, replacements))
                .toList();
        applyMeta(stack, name, lore);
        return stack;
    }

    private ItemStack createConfiguredItem(String path) {
        return itemFactory.fromConfig(inventoryConfig, path);
    }

    private ItemStack createItemFromSpec(String itemSpec) {
        return itemFactory.fromSpec(itemSpec);
    }

    private void applyMeta(ItemStack stack, String name, List<String> lore) {
        itemFactory.applyMeta(stack, name, lore);
    }

    private List<String> parseSchematicRow(String row) {
        if (row == null || row.isBlank()) {
            return List.of();
        }

        String[] parts = row.trim().split("\\s+");
        if (parts.length == 9) {
            return Arrays.asList(parts);
        }

        List<String> tokens = new ArrayList<>();
        for (char symbol : row.toCharArray()) {
            if (!Character.isWhitespace(symbol)) {
                tokens.add(String.valueOf(symbol));
            }
        }
        return tokens;
    }

    private PlayerCaseData getPlayerData(OfflinePlayer player) {
        PlayerCaseData cached = playerData.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }

        try {
            if (dataStorage == null) {
                return null;
            }
            PlayerCaseData loaded = dataStorage.load(player);
            playerData.put(loaded.getUuid(), loaded);
            return loaded;
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to load player data for " + player.getUniqueId(), exception);
            return null;
        }
    }

    private void savePlayerData(PlayerCaseData data) {
        if (dataStorage == null) {
            return;
        }

        try {
            dataStorage.save(data);
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUuid(), exception);
        }
    }

    private boolean hasUsePermission(Player player) {
        if (player.hasPermission("epiccase.use")) {
            return true;
        }
        sendMessage(player, "errors.permission", "&cNo permission.", Map.of());
        return false;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (!(sender instanceof Player) || sender.hasPermission("epiccase.admin")) {
            return true;
        }
        sendMessage(sender, "errors.permission", "&cNo permission.", Map.of());
        return false;
    }

    private void sendMessage(CommandSender sender, String path, String fallback, Map<String, String> replacements) {
        List<String> lines = messageLines(path, fallback);
        if (lines.isEmpty()) {
            return;
        }

        String prefix = messages == null ? "" : messages.getString("prefix", "");
        for (String line : lines) {
            String prepared = applyReplacements(prefix + line, replacements);
            for (String part : prepared.split("\\R", -1)) {
                sender.sendMessage(color(part));
            }
        }
    }

    private List<String> messageLines(String path, String fallback) {
        if (messages != null && messages.isList(path)) {
            return messages.getStringList(path);
        }
        if (messages != null && messages.isString(path)) {
            return List.of(messages.getString(path, ""));
        }
        if (fallback == null || fallback.isBlank()) {
            return List.of();
        }
        return List.of(fallback);
    }

    private Map<String, String> balancePlaceholders(
            CommandSender sender,
            OfflinePlayer target,
            String unitId,
            int amount,
            int total,
            BalanceKind kind
    ) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%player%", displayName(target));
        replacements.put("%admin%", sender.getName());
        replacements.put("%amount%", String.valueOf(amount));
        replacements.put("%total%", String.valueOf(total));
        replacements.put("%item%", unitId);
        replacements.put("%type%", kind == BalanceKind.BOX ? "box" : "key");
        return replacements;
    }

    private String applyReplacements(String input, Map<String, String> replacements) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace(entry.getKey(), entry.getValue());
        }
        return output;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private int parseNonNegativeInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? -1 : parsed;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private Object parseConfigurationValue(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private String safeId(String raw) {
        String id = raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return id.isBlank() ? "point" : id;
    }

    private String defaultPointId(Block block) {
        return safeId(block.getWorld().getName() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    private void debug(String message) {
        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info(message);
        }
    }

    private List<String> complete(String prefix, Collection<String> variants) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String variant : variants) {
            if (variant.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(variant);
            }
        }
        return result;
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private enum BalanceKind {
        BOX,
        KEY
    }

    private record ActivePointOpening(CasePoint point, BlockState blockState) {
    }

    private static final class AnimationDisplay {
        private static final double MOVE_EPSILON = 0.000001D;

        private final ItemDisplay itemDisplay;
        private final TextDisplay textDisplay;
        private Location lastItemLocation;

        private AnimationDisplay(ItemDisplay itemDisplay, TextDisplay textDisplay) {
            this.itemDisplay = itemDisplay;
            this.textDisplay = textDisplay;
            this.lastItemLocation = itemDisplay.getLocation();
        }

        private void move(Location itemLocation) {
            if (lastItemLocation.getWorld() == itemLocation.getWorld()
                    && lastItemLocation.distanceSquared(itemLocation) < MOVE_EPSILON) {
                return;
            }
            if (itemDisplay.isValid()) {
                itemDisplay.teleport(itemLocation);
            }
            if (textDisplay.isValid()) {
                textDisplay.teleport(itemLocation.clone().add(0.0D, 0.62D, 0.0D));
            }
            lastItemLocation = itemLocation.clone();
        }
    }

    private enum BoxDisplayState {
        NORMAL("normal"),
        AMOUNT("amount"),
        KEYS("keys"),
        CONCURRENT("concurrent"),
        WRONG_POINT("wrongpoint");

        private final String messageKey;

        BoxDisplayState(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private enum BalanceOperation {
        GIVE("give"),
        TAKE("take"),
        SET("set");

        private final String messageKey;

        BalanceOperation(String messageKey) {
            this.messageKey = messageKey;
        }
    }

    private static final class SelectionHolder implements InventoryHolder {
        private final CasePoint point;
        private final Map<Integer, String> boxSlots = new HashMap<>();
        private Inventory inventory;

        private SelectionHolder(CasePoint point) {
            this.point = point;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class KeySelectionHolder implements InventoryHolder {
        private final String boxId;
        private final CasePoint point;
        private final Map<Integer, String> keySlots = new HashMap<>();
        private Inventory inventory;

        private KeySelectionHolder(String boxId, CasePoint point) {
            this.boxId = boxId;
            this.point = point;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ConfirmationHolder implements InventoryHolder {
        private final String boxId;
        private final CasePoint point;
        private final String keyId;
        private final Set<Integer> yesSlots = new HashSet<>();
        private final Set<Integer> noSlots = new HashSet<>();
        private Inventory inventory;

        private ConfirmationHolder(String boxId, CasePoint point, String keyId) {
            this.boxId = boxId;
            this.point = point;
            this.keyId = keyId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

}
