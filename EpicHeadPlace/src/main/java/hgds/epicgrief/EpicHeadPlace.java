package hgds.epicgrief;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.logging.Level;

public final class EpicHeadPlace extends JavaPlugin implements Listener, TabExecutor {
    private static final String ADMIN_PERMISSION = "epicheadplace.admin";
    private static final String BYPASS_PERMISSION = "epicheadplace.bypass";
    private static final Pattern EVENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private NamespacedKey eventIdKey;
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        eventIdKey = new NamespacedKey(this, "event_id");

        try {
            loadData();
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not load data.yml", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand();
        getLogger().info("EpicHeadPlace enabled. Custom heads are "
                + (isCustomHeadPlacementAllowed() ? "enabled" : "disabled") + ".");
    }

    @Override
    public void onDisable() {
        saveConfig();
        saveData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "on":
                setCustomHeadPlacement(sender, true);
                return true;
            case "off":
                setCustomHeadPlacement(sender, false);
                return true;
            case "setevent":
                setEventHead(sender, args);
                return true;
            case "event":
                setEventState(sender, args);
                return true;
            default:
                sendMessage(sender, "messages.unknown-command", Map.of());
                sendUsage(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return partialMatches(args[0], List.of("on", "off", "setevent", "event", "help"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            return partialMatches(args[1], List.of("on", "off"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setevent")) {
            return partialMatches(args[1], List.of("1", "2", "3"));
        }

        return Collections.emptyList();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!isCustomHeadItem(item) || isCustomHeadPlacementAllowed() || canBypass(event.getPlayer())) {
            return;
        }

        if (isAllowedWhileOff(item)) {
            return;
        }

        event.setCancelled(true);
        sendMessage(event.getPlayer(), "messages.custom-head-blocked", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String eventId = getEventHeadId(block);
        if (eventId == null) {
            return;
        }

        if (getConfig().getBoolean("event.protect-heads", true) && !event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
            sendMessage(event.getPlayer(), "messages.event.protected", Map.of("%id%", eventId));
            return;
        }

        removeEventHead(block);
        sendMessage(event.getPlayer(), "messages.event.removed", Map.of("%id%", eventId));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHeadInteract(PlayerInteractEvent event) {
        if (!getConfig().getBoolean("event.enabled", false)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        String eventId = getEventHeadId(block);
        if (eventId == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (getConfig().getBoolean("event.once-per-player", true) && hasCollected(player, eventId)) {
            sendMessage(player, "messages.event.already-collected", Map.of("%id%", eventId));
            return;
        }

        rememberCollected(player, eventId);
        Map<String, String> placeholders = eventPlaceholders(player, block, eventId);
        sendMessage(player, "messages.event.found", placeholders);
        broadcastMessage("messages.event.broadcast", placeholders);
        runEventCommands(placeholders, eventId);
    }

    private void setCustomHeadPlacement(CommandSender sender, boolean allowed) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sendMessage(sender, "messages.no-permission", Map.of());
            return;
        }

        getConfig().set("settings.allow-custom-heads", allowed);
        saveConfig();
        sendMessage(sender, allowed ? "messages.heads-on" : "messages.heads-off", Map.of());
    }

    private void setEventHead(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sendMessage(sender, "messages.no-permission", Map.of());
            return;
        }

        if (!(sender instanceof Player player)) {
            sendMessage(sender, "messages.players-only", Map.of());
            return;
        }

        if (args.length != 2 || !EVENT_ID_PATTERN.matcher(args[1]).matches()) {
            sendMessage(sender, "messages.event.invalid-id", Map.of());
            return;
        }

        int distance = Math.max(1, getConfig().getInt("settings.target-distance", 6));
        Block block = player.getTargetBlockExact(distance);
        if (block == null || !isHeadBlock(block.getType())) {
            sendMessage(player, "messages.event.not-looking-at-head", Map.of());
            return;
        }

        String eventId = args[1];
        writeEventHead(block, eventId);
        writeEventIdToBlock(block, eventId);
        saveConfig();
        sendMessage(player, "messages.event.set", eventPlaceholders(player, block, eventId));
    }

    private void setEventState(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sendMessage(sender, "messages.no-permission", Map.of());
            return;
        }

        if (args.length != 2) {
            sendMessage(sender, "messages.event.usage", Map.of());
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {
            getConfig().set("event.enabled", true);
            saveConfig();
            sendMessage(sender, "messages.event.on", Map.of());
            return;
        }

        if (args[1].equalsIgnoreCase("off")) {
            getConfig().set("event.enabled", false);
            saveConfig();
            sendMessage(sender, "messages.event.off", Map.of());
            return;
        }

        sendMessage(sender, "messages.event.usage", Map.of());
    }

    private void registerCommand() {
        PluginCommand headsCommand = getCommand("heads");
        if (headsCommand == null) {
            getLogger().warning("Command 'heads' is missing from plugin.yml");
            return;
        }

        headsCommand.setExecutor(this);
        headsCommand.setTabCompleter(this);
    }

    private void loadData() throws IOException {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder");
        }

        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataFile.isFile()) {
            data.set("players", new LinkedHashMap<String, Object>());
            saveData();
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) {
            return;
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    private boolean isCustomHeadPlacementAllowed() {
        return getConfig().getBoolean("settings.allow-custom-heads", false);
    }

    private boolean canBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION) || player.hasPermission(ADMIN_PERMISSION);
    }

    private boolean isCustomHeadItem(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD;
    }

    private boolean isAllowedWhileOff(Material material) {
        for (String configuredMaterial : getConfig().getStringList("settings.allowed-while-off")) {
            Material allowed = Material.matchMaterial(configuredMaterial);
            if (allowed == material) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedWhileOff(ItemStack item) {
        if (item == null) {
            return false;
        }

        if (isAllowedWhileOff(item.getType())) {
            return true;
        }

        return item.getType() == Material.PLAYER_HEAD && hasAllowedCustomHeadMarker(item);
    }

    private boolean hasAllowedCustomHeadMarker(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<String> markers = getConfig().getStringList("settings.allowed-custom-head-name-parts");
        if (markers.isEmpty()) {
            return false;
        }

        List<String> text = new ArrayList<>();
        if (meta.hasDisplayName()) {
            text.add(meta.getDisplayName());
        }
        if (meta.hasLore() && meta.getLore() != null) {
            text.addAll(meta.getLore());
        }

        for (String line : text) {
            String normalizedLine = normalizeText(line);
            for (String marker : markers) {
                if (!marker.isBlank() && normalizedLine.contains(normalizeText(marker))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHeadBlock(Material material) {
        String name = material.name();
        if (name.equals("PISTON_HEAD")) {
            return false;
        }
        return name.endsWith("_HEAD")
                || name.endsWith("_WALL_HEAD")
                || name.endsWith("_SKULL")
                || name.endsWith("_WALL_SKULL");
    }

    private void writeEventHead(Block block, String eventId) {
        String path = "event-heads." + locationKey(block);
        Location location = block.getLocation();
        getConfig().set(path + ".id", eventId);
        getConfig().set(path + ".world", location.getWorld().getName());
        getConfig().set(path + ".x", location.getBlockX());
        getConfig().set(path + ".y", location.getBlockY());
        getConfig().set(path + ".z", location.getBlockZ());
    }

    private void removeEventHead(Block block) {
        getConfig().set("event-heads." + locationKey(block), null);
        clearEventIdFromBlock(block);
        saveConfig();
    }

    private String getEventHeadId(Block block) {
        if (!isHeadBlock(block.getType())) {
            return null;
        }

        String blockId = getEventIdFromBlock(block);
        if (blockId != null && !blockId.isBlank()) {
            return blockId;
        }

        String configId = getConfig().getString("event-heads." + locationKey(block) + ".id");
        return configId == null || configId.isBlank() ? null : configId;
    }

    private void writeEventIdToBlock(Block block, String eventId) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) {
            return;
        }

        tileState.getPersistentDataContainer().set(eventIdKey, PersistentDataType.STRING, eventId);
        tileState.update(true, false);
    }

    private String getEventIdFromBlock(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) {
            return null;
        }

        return tileState.getPersistentDataContainer().get(eventIdKey, PersistentDataType.STRING);
    }

    private void clearEventIdFromBlock(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof TileState tileState)) {
            return;
        }

        tileState.getPersistentDataContainer().remove(eventIdKey);
        tileState.update(true, false);
    }

    private String locationKey(Block block) {
        Location location = block.getLocation();
        String raw = location.getWorld().getName()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasCollected(Player player, String eventId) {
        return data.getStringList("players." + player.getUniqueId() + ".heads").contains(eventId);
    }

    private void rememberCollected(Player player, String eventId) {
        String path = "players." + player.getUniqueId();
        List<String> collected = new ArrayList<>(data.getStringList(path + ".heads"));
        if (!collected.contains(eventId)) {
            collected.add(eventId);
        }
        data.set(path + ".name", player.getName());
        data.set(path + ".heads", collected);
        saveData();
    }

    private void runEventCommands(Map<String, String> placeholders, String eventId) {
        List<String> commands = new ArrayList<>(getConfig().getStringList("event.commands"));
        commands.addAll(getConfig().getStringList("event.per-id." + eventId + ".commands"));
        for (String command : commands) {
            String preparedCommand = replacePlaceholders(command, placeholders).trim();
            if (preparedCommand.startsWith("/")) {
                preparedCommand = preparedCommand.substring(1);
            }
            if (!preparedCommand.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), preparedCommand);
            }
        }
    }

    private Map<String, String> eventPlaceholders(Player player, Block block, String eventId) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%player%", player.getName());
        placeholders.put("%id%", eventId);
        placeholders.put("%world%", block.getWorld().getName());
        placeholders.put("%x%", Integer.toString(block.getX()));
        placeholders.put("%y%", Integer.toString(block.getY()));
        placeholders.put("%z%", Integer.toString(block.getZ()));
        return placeholders;
    }

    private void sendUsage(CommandSender sender) {
        for (String line : getConfig().getStringList("messages.usage")) {
            sendRaw(sender, line);
        }
    }

    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path, "");
        if (message == null || message.isBlank()) {
            return;
        }

        sendRaw(sender, getConfig().getString("messages.prefix", "") + replacePlaceholders(message, placeholders));
    }

    private void broadcastMessage(String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path, "");
        if (message == null || message.isBlank()) {
            return;
        }

        Bukkit.broadcastMessage(color(getConfig().getString("messages.prefix", "") + replacePlaceholders(message, placeholders)));
    }

    private void sendRaw(CommandSender sender, String message) {
        String colored = color(message);
        if (sender instanceof Player) {
            sender.sendMessage(colored);
        } else {
            sender.sendMessage(ChatColor.stripColor(colored));
        }
    }

    private String replacePlaceholders(String input, Map<String, String> placeholders) {
        String result = input == null ? "" : input;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            result = result.replace(placeholder.getKey(), placeholder.getValue());
        }
        return result;
    }

    private String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String normalizeText(String input) {
        return ChatColor.stripColor(color(input)).toLowerCase(Locale.ROOT);
    }

    private List<String> partialMatches(String token, List<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, matches);
        Collections.sort(matches);
        return matches;
    }
}
